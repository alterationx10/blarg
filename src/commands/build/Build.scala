package commands.build

import os.*
import ursula.args.*
import ursula.command.{Command, CommandContext}

object Build extends Command {

  val WatchFlag: BooleanFlag = Flags.boolean(
    "watch",
    "w",
    "Watch for changes and rebuild"
  )

  val DirFlag: Flag[Path] = Flags.custom[Path](
    "dir",
    "d",
    "The path to containing the site files. Defaults to ./site",
    default = Some(os.pwd / "site"),
    parser = p => os.pwd / os.RelPath(p.stripPrefix("/"))
  )

  override val description: String         = "Build the site"
  override val usage: String               = "build -d ./my-site"
  override val examples: Seq[String]       = Seq(
    "build",
    "build -d ./my-site"
  )
  override val trigger: String             = "build"
  override val flags: Seq[Flag[?]]         = Seq(DirFlag, WatchFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {

    val siteFolder  = ctx.requiredFlag(DirFlag)
    val shouldWatch = ctx.booleanFlag(WatchFlag)

    val sb = SiteBuilder(siteFolder)
    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()
    sb.validateLinks()

    if shouldWatch then {
      os.watch.watch(
        Seq(siteFolder),
        onEvent = { _ =>
          println("Rebuilding site...")
          sb.cleanBuild()
          sb.copyStatic()
          sb.parseSite()
          sb.validateLinks()
          println("Build complete!")
        }
      )

      println("Watching for changes. Press return to stop.")
      scala.io.StdIn.readLine()

    }

  }

  override def action(args: Seq[String]): Unit = {}
}
