package commands.build

import dev.alteration.branch.ursula.args.{Argument, BooleanFlag, Flag, Flags}
import dev.alteration.branch.ursula.command.{Command, CommandContext}
import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.macaroni.extensions.PathExtensions.*

import java.nio.file.{Files, FileSystems, Path, StandardWatchEventKinds}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

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
    default = Some(wd / "site"),
    parser = p => wd / p.stripPrefix("/")
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
      val watcher = FileSystems.getDefault.newWatchService()

      // Helper function to register all directories for watching
      def registerDirectories(): Unit = {
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
      }

      // Initial registration
      registerDirectories()

      val bg = Future {
        while true do {
          try {
            val key    = watcher.take()
            val events = key
              .pollEvents()
              .asScala
              .filterNot(_.kind() == StandardWatchEventKinds.OVERFLOW)
            if events.nonEmpty then {
              println(s"Rebuilding site...")
              sb.cleanBuild()
              sb.copyStatic()
              sb.parseSite()
              sb.validateLinks()
              // Re-register directories to catch any new ones
              registerDirectories()
              println("Build complete!")
            }
            key.reset()
          } catch {
            case ex: Exception =>
              System.err.println(s"ERROR during rebuild: ${ex.getMessage}")
          }
        }
      }(BranchExecutors.executionContext)

      println("Watching for changes. Press return to stop.")
      scala.io.StdIn.readLine()

    }

  }

  override def action(args: Seq[String]): Unit = {}
}
