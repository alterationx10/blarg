package commands.build

import dev.wishingtree.branch.ursula.args.{Argument, BooleanFlag, Flag}
import dev.wishingtree.branch.ursula.command.Command
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.macaroni.runtimes.BranchExecutors

import java.nio.file.{Files, FileSystems, Path, StandardWatchEventKinds}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

object WatchFlag extends BooleanFlag {
  override val name: String        = "watch"
  override val shortKey: String    = "w"
  override val description: String = "Watch for changes and rebuild"
}

object DirFlag extends Flag[Path] {

  override val name: String          = "dir"
  override val shortKey: String      = "d"
  override val description: String   =
    "The path to containing the site files. Defaults to ./site"
  override val default: Option[Path] = Some(wd / "site")

  override def parse: PartialFunction[String, Path] = { case str =>
    wd / str.stripPrefix("/")
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
  override val flags: Seq[Flag[?]]         = Seq(DirFlag, WatchFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def action(args: Seq[String]): Unit = {
    val siteFolder = DirFlag.parseFirstArg(args).get
    val sb         = SiteBuilder(siteFolder)
    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()

    if WatchFlag.isPresent(args) then {
      val watcher = FileSystems.getDefault.newWatchService()
      Files
        .walk(siteFolder)
        .filter(Files.isDirectory(_))
        .forEach { path =>
          path.register(
            watcher,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
          )
        }
      val bg      = Future {
        while true do {
          val key    = watcher.take()
          val events = key
            .pollEvents()
            .asScala
            .filterNot(_.kind() == StandardWatchEventKinds.OVERFLOW)
          if events.nonEmpty then {
            println(s"Rebuilding site")
            sb.copyStatic()
            sb.parseSite()
          }
          key.reset()
        }
      }(BranchExecutors.executionContext)

      println("Watching for changes. Press return to stop.")
      scala.io.StdIn.readLine()

    }

  }
}
