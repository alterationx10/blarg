package commands.newsite

import commands.build.FrontMatter
import dev.alteration.branch.macaroni.extensions.PathExtensions.*
import dev.alteration.branch.ursula.args.{Argument, Flag}
import dev.alteration.branch.ursula.command.Command

import java.nio.file.{Files, Path, StandardOpenOption}
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import scala.util.Using

object DirFlag extends Flag[Path] {

  override val name: String          = "dir"
  override val shortKey: String      = "d"
  override val description: String   =
    "The path to create the project folder in. Defaults to ."
  override val default: Option[Path] = Some(wd)

  override def parse: PartialFunction[String, Path] = { case str =>
    wd / str.stripPrefix("/")
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
        .getOrElse(wd)
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
    ).foreach(p => Files.createDirectories(p))

    // Copy default config
    Using.resource(
      getClass.getClassLoader.getResourceAsStream("site/blarg.json")
    ) { is =>
      Files.write(
        siteFolder / "blarg.json",
        is.readAllBytes()
      )
    }

    // Add a gitignore
    Using.resource(
      getClass.getClassLoader.getResourceAsStream(".gitignore")
    ) { is =>
      val gitignore   = projectFolder / ".gitignore"
      val contentToAdd = new String(is.readAllBytes())

      if Files.exists(gitignore) && Files.isRegularFile(gitignore) then {
        // Check if content already exists to avoid duplicates
        val existingContent = Files.readString(gitignore)
        if !existingContent.contains(contentToAdd.trim) then {
          Files.writeString(
            gitignore,
            existingContent + (if existingContent.endsWith("\n") then "" else "\n") + contentToAdd,
            StandardOpenOption.WRITE
          )
        }
      } else {
        Files.writeString(gitignore, contentToAdd, StandardOpenOption.CREATE)
      }
    }

    // Copy templates
    // Main layout
    Using.resource(
      getClass.getClassLoader.getResourceAsStream(
        "site/templates/site.mustache"
      )
    ) { is =>
      Files.write(
        siteFolder / "templates" / "site.mustache",
        is.readAllBytes()
      )
    }

    // Partials
    List("header", "nav", "footer").foreach { t =>
      Using.resource(
        getClass.getClassLoader.getResourceAsStream(
          s"site/templates/partials/${t}.mustache"
        )
      ) { is =>
        Files.write(
          siteFolder / "templates" / "partials" / s"${t}.mustache",
          is.readAllBytes()
        )
      }
    }

    // Page templates
    List("page", "blog", "latest", "tags").foreach { t =>
      Using.resource(
        getClass.getClassLoader.getResourceAsStream(
          s"site/templates/pages/${t}.mustache"
        )
      ) { is =>
        Files.write(
          siteFolder / "templates" / "pages" / s"${t}.mustache",
          is.readAllBytes()
        )
      }
    }

    // Copy static resources
    List(
      "img/favicon.png",
      "img/logo.png"
    ).foreach { a =>
      Using.resource(
        getClass.getClassLoader.getResourceAsStream(s"site/static/$a")
      ) { is =>
        Files.write(
          siteFolder / "static" / a,
          is.readAllBytes()
        )
      }
    }

    // copy pages
    List(
      "about.md",
      "index.md"
    ).foreach { p =>
      Using.resource(
        getClass.getClassLoader.getResourceAsStream(s"site/pages/$p")
      ) { is =>
        Files.write(
          siteFolder / "pages" / p,
          is.readAllBytes()
        )
      }
    }

    // Make a new blog post
    // TODO this could be configurable
    val dtf = DateTimeFormatter
      .ofPattern("yyyy-MM-dd")
      .withZone(ZoneId.systemDefault())

    val blogTitle = dtf.format(Instant.now()) + "-" + "first-post.md"
    Files.writeString(
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
