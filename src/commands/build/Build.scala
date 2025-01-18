package commands.build

import dev.wishingtree.branch.ursula.args.{Argument, Flag}
import dev.wishingtree.branch.ursula.command.Command
import dev.wishingtree.branch.macaroni.fs.PathOps.*

import java.nio.file.Path

object DirFlag extends Flag[Path] {

  override val name: String          = "dir"
  override val shortKey: String      = "d"
  override val description: String   =
    "The path to build the site from. Defaults to ."
  override val default: Option[Path] = Some(wd)

  override def parse: PartialFunction[String, Path] = { case str =>
    wd / str
  }
}

object Build extends Command {

  override val description: String         = "Build the site"
  override val usage: String               = "build -d ./my-site"
  override val examples: Seq[String]       = Seq(
    "build",
    "build -d ./my-site"
  )
  override val trigger: String             = "build"
  override val flags: Seq[Flag[?]]         = Seq(DirFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def action(args: Seq[String]): Unit = {
    val root = DirFlag.parseFirstArg(args).get
    val sb   = SiteBuilder(root)
    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()
  }
}
