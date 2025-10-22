package commands.serve

import dev.alteration.branch.macaroni.extensions.PathExtensions.*
import dev.alteration.branch.spider.server.{
  FileServing,
  ServerConfig,
  SpiderServer
}
import dev.alteration.branch.ursula.args.{
  Argument,
  BooleanFlag,
  Flag,
  Flags,
  IntFlag
}
import dev.alteration.branch.ursula.command.{Command, CommandContext}

import java.nio.file.Path

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
    parser = p => wd / p,
    default = Some(wd / "build")
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

    val server = new SpiderServer(
      port = port,
      router = FileServing.createRouter(dir),
      config = ServerConfig.default
    )

    println(s"Server started at http://localhost:$port")
    println(s"Serving files from: $dir")

    if noTTY then {
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

  override def action(args: Seq[String]): Unit = {}
}
