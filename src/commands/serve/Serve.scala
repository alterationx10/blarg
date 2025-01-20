package commands.serve

import com.sun.net.httpserver.HttpServer
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.spider.server.{ContextHandler, FileContextHandler}
import dev.wishingtree.branch.ursula.args.{Argument, BooleanFlag, Flag, IntFlag}
import dev.wishingtree.branch.ursula.command.Command

import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.concurrent.CountDownLatch

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

  override def action(args: Seq[String]): Unit = {

    val latch = new CountDownLatch(1)

    val port = PortFlag.parseFirstArg(args).get
    val dir  = DirFlag.parseFirstArg(args).get

    given server: HttpServer =
      HttpServer.create(new InetSocketAddress(port), 0)

    val fileHandler =
      FileContextHandler(dir, filters = Seq(ContextHandler.timingFilter))

    ContextHandler
      .registerHandler(fileHandler)

    server.start()

    println(s"Server started at http://localhost:$port")

    if NoTTYFlag.isPresent(args) then {
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
