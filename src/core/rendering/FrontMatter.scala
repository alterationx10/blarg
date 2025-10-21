package dev.alteration.blarg.core.rendering

import dev.alteration.branch.mustachio.Stache
import dev.alteration.branch.mustachio.Stache.{Arr, Null, Str}

import java.time.temporal.ChronoUnit
import java.time.Instant
import scala.util.Try

/**
 * Front matter metadata for pages.
 *
 * Parsed from YAML front matter in markdown files or defined programmatically.
 *
 * Example markdown:
 * {{{
 * ---
 * title: My Blog Post
 * description: A great post about Scala
 * author: John Doe
 * published: 2025-01-15T10:00:00Z
 * tags:
 *   - scala
 *   - webdev
 * ---
 * }}}
 */
case class FrontMatter(
  title: Option[String],
  description: Option[String] = None,
  author: Option[String] = None,
  published: Option[Instant] = None,
  lastUpdated: Option[Instant] = None,
  tags: Option[List[String]] = None,
  slug: Option[String] = None,           // URL slug override
  template: Option[String] = None,        // Template override
  layout: Option[String] = None,          // Layout override
  image: Option[String] = None,           // Social media image
  canonical: Option[String] = None        // Canonical URL
)

object FrontMatter {

  extension (fm: FrontMatter) {
    /** Convert front matter to YAML string for generation */
    def toContent: String = {
      val sep = System.lineSeparator()
      val sb = new StringBuilder()
      sb.append("---" + sep)
      fm.title.foreach(t => sb.append(s"title: $t$sep"))
      fm.description.foreach(d => sb.append(s"description: $d$sep"))
      fm.author.foreach(a => sb.append(s"author: $a$sep"))
      fm.published.foreach(p => sb.append(s"published: $p$sep"))
      fm.lastUpdated.foreach(u => sb.append(s"lastUpdated: $u$sep"))
      fm.slug.foreach(s => sb.append(s"slug: $s$sep"))
      fm.template.foreach(t => sb.append(s"template: $t$sep"))
      fm.layout.foreach(l => sb.append(s"layout: $l$sep"))
      fm.image.foreach(i => sb.append(s"image: $i$sep"))
      fm.canonical.foreach(c => sb.append(s"canonical: $c$sep"))

      if (fm.tags.exists(_.nonEmpty)) {
        sb.append("tags:" + sep)
        fm.tags.get.foreach(tag => sb.append(s"  - $tag$sep"))
      }

      sb.append("---" + sep)
      sb.result()
    }
  }

  /** Create blank front matter for new pages */
  def blank(title: Option[String] = None): FrontMatter = FrontMatter(
    title,
    published = Some(Instant.now().truncatedTo(ChronoUnit.MINUTES)),
    lastUpdated = Some(Instant.now().truncatedTo(ChronoUnit.MINUTES))
  )

  /** Parse front matter from YAML map */
  def apply(fm: Map[String, List[String]]): FrontMatter = {
    FrontMatter(
      title = fm.get("title").map(_.mkString),
      description = fm.get("description").map(_.mkString),
      author = fm.get("author").map(_.mkString),
      published = fm.get("published")
        .map(_.mkString)
        .flatMap(str => Try(Instant.parse(str)).toOption),
      lastUpdated = fm.get("lastUpdated")
        .map(_.mkString)
        .flatMap(str => Try(Instant.parse(str)).toOption),
      tags = fm.get("tags").map(_.mkString(",").split(",").map(_.trim).toList),
      slug = fm.get("slug").map(_.mkString),
      template = fm.get("template").map(_.mkString),
      layout = fm.get("layout").map(_.mkString),
      image = fm.get("image").map(_.mkString),
      canonical = fm.get("canonical").map(_.mkString)
    )
  }

  /** Convert front matter to Stache for template rendering */
  given Conversion[FrontMatter, Stache] = fm =>
    Stache.obj(
      "title" -> fm.title.map(Str.apply).getOrElse(Null),
      "description" -> fm.description.map(Str.apply).getOrElse(Null),
      "author" -> fm.author.map(Str.apply).getOrElse(Null),
      "published" -> fm.published.map(_.toString).map(Str.apply).getOrElse(Null),
      "lastUpdated" -> fm.lastUpdated.map(_.toString).map(Str.apply).getOrElse(Null),
      "tags" -> fm.tags.map(_.map(Str.apply)).map(Arr.apply).getOrElse(Null),
      "slug" -> fm.slug.map(Str.apply).getOrElse(Null),
      "image" -> fm.image.map(Str.apply).getOrElse(Null),
      "canonical" -> fm.canonical.map(Str.apply).getOrElse(Null)
    )

  /** Convert to Stache (explicit method) */
  extension (fm: FrontMatter) {
    def toStache: Stache = summon[Conversion[FrontMatter, Stache]].apply(fm)
  }
}
