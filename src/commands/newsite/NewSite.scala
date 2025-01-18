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
    "The path to build the site from. Defaults to ."
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

    val rootDestination: Path = {
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

    // Make all directories
    List(
      rootDestination / "site",
      rootDestination / "site" / "blog",
      rootDestination / "site" / "pages",
      rootDestination / "static",
      rootDestination / "static" / "css",
      rootDestination / "static" / "img",
      rootDestination / "templates"
    ).foreach(p => Files.createDirectories(p))

    // Write a default config
    Files.writeString(
      rootDestination / "blarg.json",
      Json.encode(SiteConfig.default).toJsonString // TODO pretty string
    )

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
          getClass.getClassLoader.getResourceAsStream(s"templates/$t")
        ) { is =>
          Files.write(
            rootDestination / "templates" / t,
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
        getClass.getClassLoader.getResourceAsStream(s"static/$a")
      ) { is =>
        Files.write(
          rootDestination / "static" / a,
          is.readAllBytes()
        )
      }
    }

  }
}
