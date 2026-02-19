package commands.serve

import cask.endpoints.{staticFiles, QueryParamReader, StaticUtil}
import cask.model.{Request, Response}
import cask.router.HttpEndpoint
import os.*
import ursula.args.{Argument, BooleanFlag, Flag, Flags, IntFlag}
import ursula.command.{Command, CommandContext}

object Serve extends Command {

  val NoTTYFlag: BooleanFlag = Flags.boolean(
    "no-tty",
    "no-tty",
    "Don't wait for user input to exit (when no TTY available)"
  )

  val PortFlag: IntFlag = Flags.int(
    "port",
    "p",
    "The port to serve on. Defaults to 9000",
    default = Some(9000)
  )

  val DirFlag: Flag[Path] = Flags.custom[Path](
    "dir",
    "d",
    "The path to serve files from. Defaults to ./build",
    parser = p => os.pwd / os.RelPath(p),
    default = Some(os.pwd / "build")
  )

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

    val port  = ctx.requiredFlag(PortFlag)
    val dir   = ctx.requiredFlag(DirFlag)
    val noTTY = ctx.booleanFlag(NoTTYFlag)

    val routes = new cask.Routes {
      @blargFiles("/")
      def files(): String = dir.toString
      initialize()
    }

    val server = new cask.Main {
      override def port = ctx.requiredFlag(PortFlag)
      override def host = "localhost"
      override def allRoutes = Seq(routes)
    }

    println(s"Server started at http://localhost:$port")
    println(s"Serving files from: $dir")

    if noTTY then {
      println(s"Press Ctrl+C to exit")
      server.main(Array.empty)
      Thread.currentThread().join()
    } else {
      println(s"Press return to exit")
      // Start server in background thread
      val serverThread = new Thread(() => server.main(Array.empty))
      serverThread.start()
      scala.io.StdIn.readLine()
      println(s"Shutting down server")
      System.exit(0)
    }

  }

  override def action(args: Seq[String]): Unit = {}
}
