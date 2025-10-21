package dev.alteration.blarg.cli.commands.build
import dev.alteration.branch.mustachio.Stache
import dev.alteration.branch.mustachio.Stache.{Arr, Null, Str}

import java.time.temporal.ChronoUnit
import java.time.{Instant, Year}
import scala.util.Try

case class BuildContext(
    content: Stache,
    buildTime: String = Instant.now().toString,
    year: Int = Year.now().getValue
)

object BuildContext {
  given Conversion[BuildContext, Stache] = bc =>
    Stache.obj(
      "buildTime" -> Str(bc.buildTime),
      "year"      -> Str(bc.year.toString),
      "content"   -> bc.content
    )

}

case class FrontMatter(
    title: Option[String],
    description: Option[String],
    author: Option[String],
    published: Option[Instant],
    lastUpdated: Option[Instant],
    tags: Option[List[String]]
)

object FrontMatter {

  extension (fm: FrontMatter) {
    def toContent: String = {
      val sep = System.lineSeparator()
      val sb  = new StringBuilder()
      sb.append("---" + sep)
      sb.append("title: " + fm.title.getOrElse("") + sep)
      sb.append("description: " + fm.description.getOrElse("") + sep)
      sb.append("author: " + fm.author.getOrElse("") + sep)
      sb.append("published: " + fm.published.getOrElse("") + sep)
      sb.append("lastUpdated: " + fm.lastUpdated.getOrElse("") + sep)
      sb.append("tags: " + sep)
      if fm.tags.exists(_.nonEmpty) then
        fm.tags
          .getOrElse(List.empty)
          .foreach(tag => sb.append("  - " + tag + sep))
      sb.append("---" + sep)
      sb.result()
    }
  }

  def blank(title: Option[String] = Option.empty): FrontMatter = FrontMatter(
    title,
    Option.empty,
    Option.empty,
    Some(Instant.now().truncatedTo(ChronoUnit.MINUTES)),
    Some(Instant.now().truncatedTo(ChronoUnit.MINUTES)),
    Option.empty
  )

  def apply(fm: Map[String, List[String]]): FrontMatter = {
    FrontMatter(
      fm.get("title").map(_.mkString),
      fm.get("description").map(_.mkString),
      fm.get("author").map(_.mkString),
      fm.get("published")
        .map(_.mkString)
        .flatMap(str => Try(Instant.parse(str)).toOption),
      fm.get("lastUpdated")
        .map(_.mkString)
        .flatMap(str => Try(Instant.parse(str)).toOption),
      fm.get("tags").map(_.mkString(",").split(",").toList)
    )
  }

  given Conversion[FrontMatter, Stache] = fm =>
    Stache.obj(
      "title"       -> fm.title.map(Str.apply).getOrElse(Null),
      "description" -> fm.description.map(Str.apply).getOrElse(Null),
      "author"      -> fm.author.map(Str.apply).getOrElse(Null),
      "published"   -> fm.published
        .map(_.toString)
        .map(Str.apply)
        .getOrElse(Null),
      "lastUpdated" -> fm.lastUpdated
        .map(_.toString)
        .map(Str.apply)
        .getOrElse(Null),
      "tags"        -> fm.tags.map(_.map(Str.apply)).map(Arr.apply).getOrElse(Null)
    )
}

case class ContentContext(
    content: String,
    fm: FrontMatter,
    href: String,
    summary: String
)

object ContentContext {
  given Conversion[ContentContext, Stache] = pc =>
    Stache.obj(
      "content" -> Str(pc.content),
      "fm"      -> pc.fm,
      "href"    -> Str(pc.href),
      "summary" -> Str(pc.summary)
    )
}
