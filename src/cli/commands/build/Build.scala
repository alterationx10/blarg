package dev.alteration.blarg.cli.commands.build

import dev.alteration.blarg.core.{BlargSite, SiteConfig}
import dev.alteration.blarg.core.rendering.{Blarg, Markdown}
import dev.alteration.blarg.core.server.SiteBuilder
import dev.alteration.branch.macaroni.extensions.PathExtensions.*
import dev.alteration.branch.macaroni.runtimes.BranchExecutors
import dev.alteration.branch.ursula.args.{Argument, BooleanFlag, Flag}
import dev.alteration.branch.ursula.command.{Command, CommandContext}

import java.nio.file.{FileSystems, Files, Path, StandardWatchEventKinds}
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

object OutFlag extends Flag[Path] {
  override val name: String          = "out"
  override val shortKey: String      = "o"
  override val description: String   = "Output directory for build. Defaults to ./build"
  override val default: Option[Path] = Some(wd / "build")

  override def parse: PartialFunction[String, Path] = { case str =>
    wd / str.stripPrefix("/")
  }
}

object Build extends Command {

  override val description: String         = "Build the site"
  override val usage: String               = "build -d ./my-site -o ./build"
  override val examples: Seq[String]       = Seq(
    "build",
    "build -d ./my-site",
    "build -d ./my-site -o ./dist"
  )
  override val trigger: String             = "build"
  override val flags: Seq[Flag[?]]         = Seq(DirFlag, OutFlag, WatchFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {
    val siteFolder = ctx.requiredFlag(DirFlag)
    val buildDir   = ctx.requiredFlag(OutFlag)

    // Create a simple BlargSite instance from the folder structure
    val site = new BlargSite {
      override def contentDir: Path = siteFolder
      override def templateDir: Path = siteFolder.resolve("templates")
      override def staticDir: Path = siteFolder.resolve("static")

      // Load all markdown pages from pages/ and blog/ directories
      override def staticPages: Seq[dev.alteration.blarg.core.pages.StaticPage] = {
        val pagesDir = siteFolder.resolve("pages")
        val blogDir = siteFolder.resolve("blog")

        val pages = if (Files.exists(pagesDir)) Markdown.loadAllPages(pagesDir) else Seq.empty
        val blog = if (Files.exists(blogDir)) Markdown.loadAllPages(blogDir) else Seq.empty

        pages ++ blog
      }
    }

    // Build the site using core SiteBuilder
    SiteBuilder.build(site, buildDir)

    // Watch mode
    if ctx.booleanFlag(WatchFlag) then {
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
      val bg = Future {
        while true do {
          val key    = watcher.take()
          val events = key
            .pollEvents()
            .asScala
            .filterNot(_.kind() == StandardWatchEventKinds.OVERFLOW)
          if events.nonEmpty then {
            println(s"Rebuilding site...")
            Blarg.clearCache()  // Clear template cache
            SiteBuilder.build(site, buildDir)
          }
          key.reset()
        }
      }(BranchExecutors.executionContext)

      println("Watching for changes. Press return to stop.")
      scala.io.StdIn.readLine()
    }
  }
}
