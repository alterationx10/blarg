package commands.serve

import dev.alteration.branch.macaroni.extensions.PathExtensions.*
import dev.alteration.branch.spider.server.{FileServing, ServerConfig, SpiderServer}
import dev.alteration.branch.ursula.args.{Argument, BooleanFlag, Flag, IntFlag}
import dev.alteration.branch.ursula.command.Command

import java.nio.file.Path

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

    val port = PortFlag.parseFirstArg(args).get
    val dir  = DirFlag.parseFirstArg(args).get

    val server = new SpiderServer(
      port = port,
      router = FileServing.createRouter(dir),
      config = ServerConfig.default
    )

    println(s"Server started at http://localhost:$port")
    println(s"Serving files from: $dir")

    if NoTTYFlag.isPresent(args) then {
      println(s"Press Ctrl+C to exit")
      server.start() // Blocking call
    } else {
      println(s"Press return to exit")
      // Start server in background thread
      val serverThread = new Thread(() => server.start())
      serverThread.start()
      scala.io.StdIn.readLine()
      println(s"Shutting down server")
      System.exit(0)
    }

  }
}
