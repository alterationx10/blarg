package dev.alteration.blarg.cli.commands.serve

import blarg.core.server.BlargServer
import dev.alteration.branch.macaroni.extensions.PathExtensions.*
import dev.alteration.branch.ursula.args.{Argument, Flag, IntFlag}
import dev.alteration.branch.ursula.command.{Command, CommandContext}

import java.nio.file.Path

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

object StaticFlag extends Flag[Path] {

  override val name: String                         = "static"
  override val shortKey: String                     = "s"
  override val description: String                  =
    "Optional path to static assets directory (served at /static)"
  override def parse: PartialFunction[String, Path] = { case str =>
    wd / str
  }
}

object Serve extends Command {

  override val description: String         = "Start an HTTP server that serves files"
  override val usage: String               = "serve -p 9000 -d ./build -s ./site/static"
  override val examples: Seq[String]       = Seq(
    "serve",
    "serve -p 9000",
    "serve -d ./build",
    "serve -d ./build -s ./site/static"
  )
  override val trigger: String             = "serve"
  override val flags: Seq[Flag[?]]         = Seq(PortFlag, DirFlag, StaticFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {
    val port = ctx.requiredFlag(PortFlag)
    val dir  = ctx.requiredFlag(DirFlag)
    val staticDir = ctx.flag(StaticFlag)

    // Use core BlargServer to serve static files
    BlargServer.serveStatic(
      port = port,
      buildDir = dir,
      staticDir = staticDir
    )
  }
}
