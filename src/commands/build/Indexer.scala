package commands.build

import dev.wishingtree.branch.friday.Json.{JsonArray, JsonString}
import dev.wishingtree.branch.friday.JsonEncoder
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import org.jsoup.Jsoup

import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

trait Indexer {
  def index(): Unit
}

object Indexer {

  case class IndexedHtml(
      title: String,
      description: String,
      tags: List[String],
      content: String,
      href: String
  ) derives JsonEncoder

  def apply(root: Path): Indexer = new Indexer {
    override def index(): Unit = {
      val _thisBuild = root.getParent / "build"

      val loader: ContentLoader = ContentLoader(_thisBuild)

      val indexedData: mutable.ArrayBuffer[IndexedHtml] =
        mutable.ArrayBuffer.empty

      val indexedTags: mutable.ArrayBuffer[String] =
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
            .toList.filterNot(_.isBlank)

          indexedData.addOne(
            IndexedHtml(
              title = html.title(),
              description =
                html.select("meta[name=description]").attr("content"),
              tags = tags,
              content = content,
              href = "/" + f.relativeTo(_thisBuild).toString
            )
          )

          indexedTags.addAll(tags)

        }

      Files.writeString(
        _thisBuild / "search.json",
        JsonArray(indexedData.map(_.toJson).toIndexedSeq).toJsonString
      )

      Files.writeString(
        _thisBuild / "tags.json",
        JsonArray(indexedTags.sorted.distinct.map(JsonString.apply).toIndexedSeq).toJsonString
      )
    }
  }
}
