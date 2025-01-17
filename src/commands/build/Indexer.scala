package commands.build

import dev.wishingtree.branch.friday.Json
import dev.wishingtree.branch.friday.Json.{JsonArray, JsonObject, JsonString}
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.mustachio.{Mustachio, Stache}
import org.jsoup.Jsoup
import repository.IndexedHtml

import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

trait Indexer {
  def index(): Unit
}

case class StaticIndexer(root: Path) extends Indexer {
  val _thisBuild: Path = root.getParent / "build"

  override def index(): Unit = {

    println("running indexer")

    val loader: ContentLoader = ContentLoader(_thisBuild)

    val indexedData: mutable.ArrayBuffer[IndexedHtml] =
      mutable.ArrayBuffer.empty

    Files
      .walk(_thisBuild)
      .filter(_.toString.endsWith(".html"))
      .forEach { f =>
        val rawContent = loader.load(f)
        val html       = Jsoup.parse(rawContent)
        val paragraphs = html.getElementById("site-content").select("p")
        val content    =
          paragraphs.iterator().asScala.map(_.text()).mkString(" ")

        val tags = html
          .select("meta[name=keywords]")
          .attr("content")
          .split(",")
          .toList
          .filterNot(_.isBlank)

        indexedData.addOne(
          IndexedHtml(
            title = html.title(),
            description = html.select("meta[name=description]").attr("content"),
            tags = tags,
            content = content.toLowerCase,
            href = "/" + f.relativeTo(_thisBuild).toString,
            published = "", // TODO
            updated = "",   // TODO
            summary = content.take(250) + "..."
          )
        )

      }


    Files.writeString(
      _thisBuild / "search.json",
      JsonArray(indexedData.map(_.toJson).toIndexedSeq).toJsonString
    )

    println(s"still going")
    val tagList = indexedData
      .flatMap(_.tags)
      .distinct
      .sorted
      .map { tag =>
        Json.obj(
          "tag"  -> JsonString(tag),
          "docs" -> Json.arr(
            indexedData
              .filter(_.tags.contains(tag))
              .map { d =>
                Json.obj(
                  "title"       -> JsonString(d.title),
                  "description" -> JsonString(d.description),
                  "href"        -> JsonString(d.href),
                  "published"   -> JsonString(d.published),
                  "updated"     -> JsonString(d.updated),
                  "tags"        -> JsonArray(
                    d.tags.map(JsonString.apply).toIndexedSeq
                  ),
                  "summary"     -> JsonString(d.content.take(250) + "...")
                )
              }
              .toSeq*
          )
        )
      }
      .toIndexedSeq

    println("tagy list")
//    val siteTemplate    = ContentLoader(root).loadSiteTemplate()
//    val contentTemplate = ContentLoader(root).loadTemplate("tags.html")

//    val siteHtml    = Jsoup.parse(siteTemplate)
//    val contentHtml = Jsoup.parse(contentTemplate)

//    val tags   = indexedData.flatMap(_.tags).distinct.sorted
//    val tagDiv = contentHtml.getElementById("tags-content")
//    tags.foreach { tag =>
//      tagDiv
//        .appendElement("section")
//        .attr("id", tag)
//        .appendElement("a")
//        .attr("href", s"#$tag")
//    }
//
//    println("taggy json")
//
//    Files.writeString(
//      _thisBuild / "tags.json",
//      JsonArray(tagList).toJsonString
//    )

    println(s"here at $root")
    println(s"need to be at  ${root.getParent}")
    val mustacheTemplate = ContentLoader(root.getParent / "test_site").loadTemplate("tags.mustache")
    println(s"template: $mustacheTemplate")
    val mustacheHtml = Mustachio.render(mustacheTemplate, Stache.fromJson(JsonArray(tagList)))
    println(s"html: $mustacheHtml")
    println("where?")
    Files.writeString(
      _thisBuild / "tags.html",
      mustacheHtml
    )
  }
}

object Indexer {

  def staticIndexer(root: Path): Indexer =
    StaticIndexer(root)

}
