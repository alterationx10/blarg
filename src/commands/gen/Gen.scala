package commands.gen

import commands.build.FrontMatter
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.ursula.args.{Argument, Flag}
import dev.wishingtree.branch.ursula.command.Command

import java.nio.file.{Files, Path}
import java.text.SimpleDateFormat
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter

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

  override val exclusive: Option[Seq[Flag[?]]] = Some(Seq(PageFlag))
}

object PageFlag extends Flag[Path] {

  override val expectsArgument: Boolean = false

  override val name: String        = "page"
  override val shortKey: String    = "p"
  override val description: String = "Generate a new page"

  override def parse: PartialFunction[String, Path] = str => wd / str

  override val exclusive: Option[Seq[Flag[_]]] = Some(Seq(BlogFlag))
}

object Gen extends Command {

  override val description: String         = "Generate a new blog post or page"
  override val usage: String               = "gen -b \"My New Blog Post\""
  override val examples: Seq[String]       = Seq(
    "gen -b \"My New Blog Post\"",
    "gen -p ./path/to/new-page.md",
    "gen -d ./site-root -p new-page.md"
  )
  override val trigger: String             = "gen"
  override val flags: Seq[Flag[_]]         = Seq(DirFlag, BlogFlag, PageFlag)
  override val arguments: Seq[Argument[_]] = Seq.empty

  override def action(args: Seq[String]): Unit =

    println(s"args: $args")
    val sitePath = DirFlag.parseFirstArg(args).get

    if PageFlag.isPresent(args) then {
      for {
        sitePath <- DirFlag.parseFirstArg(args)
        newFile  <- PageFlag.parseFirstArg(args)
      } yield {
        val result = Files.writeString(
          sitePath / "site" / "pages" / newFile.relativeTo(wd),
          "Drop in some FM"
        )
        println(s"New page created at $result")
      }
    }

    val dtf = DateTimeFormatter
      .ofPattern("yyyy-MM-dd")
      .withZone(ZoneId.systemDefault())

    if BlogFlag.isPresent(args) then
      for {
        sitePath <- DirFlag.parseFirstArg(args)
        title    <- BlogFlag.parseFirstArg(args)
      } yield {
        println(sitePath)
        println(title)
        val name        = dtf.format(Instant.now()) + "-" + title.toLowerCase.trim
          .replaceAll(" ", "-") + ".md"
        val destination = sitePath / "site" / "blog" / name
        if Files.exists(destination) then
          println(s"Blog post already exists at $destination")
        else
          Files.writeString(
            sitePath / "site" / "blog" / name,
            FrontMatter.blank(Some(title)).toContent
          )
          println(s"New blog post created at $destination")
      }

}
