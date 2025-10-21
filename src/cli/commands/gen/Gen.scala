package dev.alteration.blarg.cli.commands.gen

import dev.alteration.blarg.core.rendering.FrontMatter
import dev.alteration.branch.macaroni.extensions.PathExtensions.*
import dev.alteration.branch.ursula.args.{Argument, Flag}
import dev.alteration.branch.ursula.command.{Command, CommandContext}

import java.nio.file.{Files, Path}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

object FrontmatterFlag extends Flag[Path] {

  override val name: String        = "frontmatter"
  override val shortKey: String    = "fm"
  override val description: String = "Append frontmatter to an exising file"

  override def parse: PartialFunction[String, Path] = str => wd / str

  override val exclusive: Option[Seq[Flag[?]]] = Some(
    Seq(
      PageFlag,
      BlogFlag
    )
  )

}

object DirFlag extends Flag[Path] {

  override val name: String          = "dir"
  override val shortKey: String      = "d"
  override val description: String   =
    "The root path to generate the file in. Defaults to ./site"
  override val default: Option[Path] = Some(wd / "site")

  override def parse: PartialFunction[String, Path] = { case str =>
    wd / str
  }
}

object BlogFlag extends Flag[String] {

  override val name: String        = "blog"
  override val shortKey: String    = "b"
  override val description: String =
    "Generate a new blog post with the given title"

  override def parse: PartialFunction[String, String] = identity(_)

  override val exclusive: Option[Seq[Flag[?]]] = Some(
    Seq(PageFlag, FrontmatterFlag)
  )
}

object PageFlag extends Flag[Path] {

  override val expectsArgument: Boolean = false

  override val name: String        = "page"
  override val shortKey: String    = "p"
  override val description: String = "Generate a new page"

  override def parse: PartialFunction[String, Path] = str =>
    wd / str.stripPrefix("/")

  override val exclusive: Option[Seq[Flag[?]]] = Some(
    Seq(BlogFlag, FrontmatterFlag)
  )
}

object Gen extends Command {

  override val description: String         = "Generate a new blog post or page"
  override val usage: String               = "gen -b \"My New Blog Post\""
  override val examples: Seq[String]       = Seq(
    "gen -b \"My New Blog Post\"",
    "gen -p path/to/new-page.md",
    "gen -d ./other-site -p new-page.md"
  )
  override val trigger: String             = "gen"
  override val flags: Seq[Flag[?]]         =
    Seq(DirFlag, BlogFlag, PageFlag, FrontmatterFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {

    // TODO this could be configurable
    val dtf = DateTimeFormatter
      .ofPattern("yyyy-MM-dd")
      .withZone(ZoneId.systemDefault())

    val sitePath = ctx.requiredFlag(DirFlag)

    ctx.flag(PageFlag).foreach { newFile =>
      val destination = sitePath / "pages" / newFile.relativeTo(wd)
      if Files.exists(destination) then
        println(s"Page already exists at $destination")
      else {
        Files.createDirectories(destination.getParent)
        val result = Files.writeString(
          destination,
          FrontMatter.blank().toContent
        )
        println(s"New page created at $result")
      }
    }

    ctx.flag(BlogFlag).foreach { title =>
      val name        = dtf.format(Instant.now()) + "-" + title.toLowerCase.trim
        .replaceAll(" ", "-") + ".md"
      val destination = sitePath / "blog" / name
      if Files.exists(destination) then
        println(s"Blog post already exists at $destination")
      else {
        Files.createDirectories(destination.getParent)
        Files.writeString(
          destination,
          FrontMatter.blank(Some(title)).toContent
        )
        println(
          s"New blog post created at $destination"
        )
      }
    }

    ctx.flag(FrontmatterFlag).foreach { fmPath =>
      if Files.exists(fmPath) then {
        // TODO parse and merge
        val current = Files.readString(fmPath)
        Files.writeString(fmPath, FrontMatter.blank().toContent + current)
        println(s"Frontmatter added to $fmPath")
      } else println(s"File does not exist at $fmPath")
    }

  }

}
