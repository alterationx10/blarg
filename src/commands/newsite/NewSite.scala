package commands.newsite

import config.SiteConfig
import dev.wishingtree.branch.friday.Json
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.ursula.args.{Argument, Flag}
import dev.wishingtree.branch.ursula.command.Command
import dev.wishingtree.branch.friday.Json.*

import java.nio.file.{Files, Path}
import scala.util.Using

object DirFlag extends Flag[Path] {

  override val name: String          = "dir"
  override val shortKey: String      = "d"
  override val description: String   =
    "The path to create the project folder in. Defaults to ."
  override val default: Option[Path] = Some(wd)

  override def parse: PartialFunction[String, Path] = { case str =>
    wd / str
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
      siteFolder / " static",
      siteFolder / " static" / "js",
      siteFolder / " static" / "css",
      siteFolder / "static" / "img",
      siteFolder / "templates"
    ).foreach(p => Files.createDirectories(p))

    // Write a default config
    Files.writeString(
      siteFolder / "blarg.json",
      Json.encode(SiteConfig.default).toJsonString
    )

    // Add a gitignore
    Using.resource(
      getClass.getClassLoader.getResourceAsStream(".gitignore")
    ) { is =>
      Files.write(
        projectFolder / ".gitignore",
        is.readAllBytes()
      )
    }

    // Copy templates
    List(
      "blog",
      "latest",
      "page",
      "site",
      "tags"
    ).map(_ + ".mustache")
      .foreach { t =>
        Using.resource(
          getClass.getClassLoader.getResourceAsStream(s"site/templates/$t")
        ) { is =>
          Files.write(
            siteFolder / "templates" / t,
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

  }
}
