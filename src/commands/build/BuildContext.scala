package commands.build

import config.{NavItem, SiteConfig}
import dev.alteration.branch.mustachio.Stache
import dev.alteration.branch.mustachio.Stache.{Arr, Null, Str}

import java.time.temporal.ChronoUnit
import java.time.{Instant, Year, ZoneId}
import java.time.format.DateTimeFormatter
import scala.util.Try

case class BuildContext(
    content: Stache,
    config: SiteConfig,
    buildTime: String,
    year: Int
)

object BuildContext {
  given Conversion[NavItem, Stache] = ni =>
    Stache.obj(
      "label" -> Str(ni.label),
      "href"  -> Str(ni.href)
    )

  given Conversion[SiteConfig, Stache] = sc =>
    Stache.obj(
      "siteTitle"  -> Str(sc.siteTitle),
      "author"     -> Str(sc.author),
      "navigation" -> Arr(sc.navigation.map(summon[Conversion[NavItem, Stache]].apply))
    )

  given Conversion[BuildContext, Stache] = bc =>
    Stache.obj(
      "buildTime" -> Str(bc.buildTime),
      "year"      -> Str(bc.year.toString),
      "content"   -> bc.content,
      "config"    -> bc.config
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

  private val humanDateFormatter: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern("MMMM d, yyyy")
      .withZone(ZoneId.systemDefault())

  private def formatDate(instant: Instant): String =
    humanDateFormatter.format(instant)

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

  private def parseStringField(fm: Map[String, List[String]], key: String): Option[String] = {
    fm.get(key)
      .map(_.mkString)
      .filter(_.nonEmpty)  // Convert empty strings to None
  }

  def apply(fm: Map[String, List[String]]): FrontMatter = {
    FrontMatter(
      parseStringField(fm, "title"),
      parseStringField(fm, "description"),
      parseStringField(fm, "author"),
      parseStringField(fm, "published")
        .flatMap(str => Try(Instant.parse(str)).toOption),
      parseStringField(fm, "lastUpdated")
        .flatMap(str => Try(Instant.parse(str)).toOption),
      fm.get("tags").map(_.filter(_.nonEmpty))  // YAML already provides list, just filter empties
    )
  }

  given Conversion[FrontMatter, Stache] = fm =>
    Stache.obj(
      "title"       -> fm.title.map(Str.apply).getOrElse(Null),
      "description" -> fm.description.map(Str.apply).getOrElse(Null),
      "author"      -> fm.author.map(Str.apply).getOrElse(Null),
      "published"   -> fm.published
        .map(formatDate)
        .map(Str.apply)
        .getOrElse(Null),
      "lastUpdated" -> fm.lastUpdated
        .map(formatDate)
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
