package commands.serve

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import dev.alteration.branch.macaroni.extensions.PathExtensions.*
// TODO: Update to use new spider.server API
// import dev.alteration.branch.spider.server.{ContextHandler, FileContextHandler}
import dev.alteration.branch.ursula.args.{Argument, BooleanFlag, Flag, IntFlag}
import dev.alteration.branch.ursula.command.{Command, CommandContext}

import java.net.InetSocketAddress
import java.nio.file.{Files, Path}
import java.util.concurrent.CountDownLatch
import scala.util.Using

object NoTTYFlag extends BooleanFlag {
  override val description: String =
    "Don't wait for user input to exit (when no TTY available)"
  override val name: String        = "no-tty"
  override val shortKey: String    = "no-tty"
}

object PortFlag extends IntFlag {

  override val name: String         = "port"
  override val shortKey: String     = "p"
  override val description: String  = "The port to serve on. Defaults to 9000"
  override val default: Option[Int] = Some(9000)

}

object DirFlag extends Flag[Path] {

  override val name: String                         = "dir"
  override val shortKey: String                     = "d"
  override val description: String                  =
    "The path to serve files from. Defaults to ./build"
  override val default: Option[Path]                = Some(wd / "build")
  override def parse: PartialFunction[String, Path] = { case str =>
    wd / str
  }
}

object Serve extends Command {

  override val description: String         = "Start an HTTP server that serves files"
  override val usage: String               = "serve -p 9000 -d ./build"
  override val examples: Seq[String]       = Seq(
    "serve",
    "serve -p 8080",
    "serve -d ./build"
  )
  override val trigger: String             = "serve"
  override val flags: Seq[Flag[?]]         = Seq(PortFlag, DirFlag, NoTTYFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {

    val latch = new CountDownLatch(1)

    val port = ctx.requiredFlag(PortFlag)
    val dir  = ctx.requiredFlag(DirFlag)

    val server: HttpServer =
      HttpServer.create(new InetSocketAddress(port), 0)

    // TODO: Replace with updated spider.server API when available
    // For now, use basic file serving
    val fileHandler = new HttpHandler {
      override def handle(exchange: HttpExchange): Unit = {
        val path = exchange.getRequestURI.getPath
        val file = dir.resolve(if (path == "/") "index.html" else path.stripPrefix("/"))

        if (Files.exists(file) && Files.isRegularFile(file)) {
          val bytes = Files.readAllBytes(file)
          exchange.sendResponseHeaders(200, bytes.length)
          Using.resource(exchange.getResponseBody)(_.write(bytes))
        } else {
          val notFound = "404 Not Found".getBytes
          exchange.sendResponseHeaders(404, notFound.length)
          Using.resource(exchange.getResponseBody)(_.write(notFound))
        }
      }
    }

    server.createContext("/", fileHandler)
    server.start()

    println(s"Server started at http://localhost:$port")

    if ctx.booleanFlag(NoTTYFlag) then {
      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run(): Unit = {
          latch.countDown()
        }
      })
      println(s"Press Ctrl+C to exit")
    } else {
      latch.countDown()
      println(s"Press return to exit")
      scala.io.StdIn.readLine()
    }

    latch.await()
    // No guarantee this will get printed on --no-tty
    println(s"Shutting down server")

  }
}
