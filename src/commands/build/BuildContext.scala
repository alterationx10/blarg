package commands.build

import dev.wishingtree.branch.mustachio.Stache
import dev.wishingtree.branch.mustachio.Stache.{Arr, Null, Str}

import java.time.{Instant, Year}

case class BuildContext(
    buildTime: String = Instant.now().toString,
    year: Int = Year.now().getValue,
    page: Option[PageContext] = Option.empty,
    blog: Option[BlogContext] = Option.empty
)

object BuildContext {
  given Conversion[BuildContext, Stache] = bc =>
    Stache.obj(
      "buildTime" -> Str(bc.buildTime),
      "year"      -> Str(bc.year.toString),
      "page"      -> bc.page
        .map(PageContext.given_Conversion_PageContext_Stache)
        .getOrElse(Null),
      "blog"      -> bc.blog
        .map(BlogContext.given_Conversion_BlogContext_Stache)
        .getOrElse(Null)
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

  def apply(fm: Map[String, List[String]]): FrontMatter = {
    FrontMatter(
      fm.get("title").map(_.mkString),
      fm.get("description").map(_.mkString),
      fm.get("author").map(_.mkString),
      fm.get("published").map(_.mkString).map(Instant.parse),
      fm.get("lastUpdated").map(_.mkString).map(Instant.parse),
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

case class PageContext(
    content: String,
    fm: FrontMatter,
    href: String,
    summary: String
)

object PageContext {
  given Conversion[PageContext, Stache] = pc =>
    Stache.obj(
      "content" -> Str(pc.content),
      "fm"      -> pc.fm,
      "year"    -> Str(Year.now().toString),
      "summary" -> Str(pc.summary)
    )
}

case class BlogContext(
    content: String,
    fm: FrontMatter,
    href: String,
    summary: String
)

object BlogContext {
  given Conversion[BlogContext, Stache] = bc =>
    Stache.obj(
      "content" -> Str(bc.content),
      "fm"      -> bc.fm,
      "year"    -> Str(Year.now().toString),
      "summary" -> Str(bc.summary)
    )
}
