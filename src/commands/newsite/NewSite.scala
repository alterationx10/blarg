package commands.newsite

import commands.build.FrontMatter
import os.*
import ursula.args.{Argument, Flag}
import ursula.command.Command

import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import scala.util.Using

object DirFlag extends Flag[Path] {

  override val name: String          = "dir"
  override val shortKey: String      = "d"
  override val description: String   =
    "The path to create the project folder in. Defaults to ."
  override val default: Option[Path] = Some(os.pwd)

  override def parse: PartialFunction[String, Path] = { case str =>
    os.pwd / os.RelPath(str.stripPrefix("/"))
  }
}

object NameArg extends Argument[String] {

  override val name: String        = "name"
  override val description: String =
    "The name of the folder to create the new site in."

  override def parse: PartialFunction[String, String] = identity(_)
}

object NewSite extends Command {
  override val description: String         = "Templates out a new site"
  override val usage: String               = "new site-name"
  override val examples: Seq[String]       = Seq(
    "new site-name",
    "new site-name -d ./"
  )
  override val trigger: String             = "new"
  override val flags: Seq[Flag[?]]         = Seq(DirFlag)
  override val arguments: Seq[Argument[?]] = Seq(NameArg)

  override def action(args: Seq[String]): Unit = {

    val projectFolder: Path = {
      DirFlag
        .parseFirstArg(args)
        .map { p =>
          stripFlags(args).headOption.map(NameArg.parse) match {
            case Some(name) => p / name
            case None       => p
          }
        }
        .getOrElse(os.pwd)
    }

    val siteFolder = projectFolder / "site"

    // Make all directories
    List(
      siteFolder,
      siteFolder / "blog",
      siteFolder / "pages",
      siteFolder / "static",
      siteFolder / "static" / "js",
      siteFolder / "static" / "css",
      siteFolder / "static" / "img",
      siteFolder / "templates",
      siteFolder / "templates" / "partials",
      siteFolder / "templates" / "pages"
    ).foreach(p => os.makeDir.all(p))

    // Helper to load resource with better error handling
    def loadResource(path: String, destination: Path): Unit = {
      val resourceStream = getClass.getClassLoader.getResourceAsStream(path)
      if resourceStream == null then {
        System.err.println(s"ERROR: Could not find resource: $path")
        System.err.println(s"This is likely a build/packaging issue.")
        System.err.println(
          s"You can make an issue at https://github.com/alterationx10/blarg/issues."
        )
        System.exit(1)
      }
      Using.resource(resourceStream) { is =>
        os.write(destination, is.readAllBytes())
      }
    }

    // Copy default config
    loadResource("site/blarg.json", siteFolder / "blarg.json")

    // Add a gitignore
    val gitignoreStream =
      getClass.getClassLoader.getResourceAsStream("gitignore")
    if gitignoreStream != null then {
      Using.resource(gitignoreStream) { is =>
        val gitignore    = projectFolder / ".gitignore"
        val contentToAdd = new String(is.readAllBytes())

        if os.exists(gitignore) && !os.isDir(gitignore) then {
          // Check if content already exists to avoid duplicates
          val existingContent = os.read(gitignore)
          if !existingContent.contains(contentToAdd.trim) then {
            os.write.over(
              gitignore,
              existingContent + (if existingContent.endsWith("\n") then ""
                                 else "\n") + contentToAdd
            )
          }
        } else {
          os.write(gitignore, contentToAdd)
        }
      }
    }

    // Copy templates
    loadResource(
      "site/templates/site.mustache",
      siteFolder / "templates" / "site.mustache"
    )

    // Partials
    List("header", "nav", "footer").foreach { t =>
      loadResource(
        s"site/templates/partials/${t}.mustache",
        siteFolder / "templates" / "partials" / s"${t}.mustache"
      )
    }

    // Page templates
    List("page", "blog", "latest", "tags").foreach { t =>
      loadResource(
        s"site/templates/pages/${t}.mustache",
        siteFolder / "templates" / "pages" / s"${t}.mustache"
      )
    }

    // Copy static resources
    List("img/favicon.png", "img/logo.png").foreach { a =>
      loadResource(s"site/static/$a", siteFolder / "static" / os.RelPath(a))
    }

    // copy pages
    List("about.md", "index.md").foreach { p =>
      loadResource(s"site/pages/$p", siteFolder / "pages" / p)
    }

    // Make a new blog post
    // TODO this could be configurable
    val dtf = DateTimeFormatter
      .ofPattern("yyyy-MM-dd")
      .withZone(ZoneId.systemDefault())

    val blogTitle = dtf.format(Instant.now()) + "-" + "first-post.md"
    os.write(
      siteFolder / "blog" / blogTitle,
      FrontMatter
        .blank(Some("First Post"))
        .copy(
          tags = Some(List("first-post", "blarg")),
          description = Some("This is the first post on the site")
        )
        .toContent + "# First Post" + System.lineSeparator() + System
        .lineSeparator() + "This is the first post on the site"
    )
  }
}
