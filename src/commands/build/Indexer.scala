package commands.build

import dev.wishingtree.branch.friday.Json.{JsonArray, JsonObject, JsonString}
import dev.wishingtree.branch.friday.{Json, JsonCodec, JsonEncoder}
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.macaroni.poolers.ResourcePool
import dev.wishingtree.branch.piggy.{ResultSetGetter, Sql}
import dev.wishingtree.branch.piggy.Sql.{ps, tuple1}
import org.jsoup.Jsoup

import java.nio.file.{Files, Path}
import java.sql.{Connection, DriverManager, ResultSet}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

case class IndexedHtml(
    title: String,
    description: String,
    tags: List[String],
    content: String,
    href: String,
    published: String,
    updated: String,
    summary: String
) derives JsonCodec

trait Indexer {
  def index(): Unit
  def init: Sql[Unit]
  def ingest(record: IndexedHtml): Sql[Unit]
  def fts(txt: String): Sql[Seq[IndexedHtml]]
  def byTag(tag: String): Sql[Seq[IndexedHtml]]
}

object Indexer {

  def apply(root: Path): Indexer = new Indexer {

    val _thisBuild: Path = root.getParent / "build"

    given connPool: ResourcePool[Connection] = new ResourcePool[Connection] {

      val dbPath: Path = _thisBuild / "index.sqlite"

      override def acquire: Connection =
        DriverManager.getConnection(s"jdbc:sqlite:${dbPath.toString}")

      override def release(resource: Connection): Unit = resource.close()

    }

    override def init: Sql[Unit] = Sql
      .statement(
        "CREATE VIRTUAL TABLE indexed_html USING fts5(title, description, tags, content, href, published, updated, summary);"
      )
      .map(_ => ())

    override def ingest(record: IndexedHtml): Sql[Unit] =
      Sql.prepare[IndexedHtml](
        ih =>
          ps"INSERT INTO indexed_html(title, description, tags, content, href, published, updated, summary) VALUES (${ih.title}, ${ih.description}, ${ih.tags
              .mkString(",")}, ${ih.content}, ${ih.href}, ${ih.published}, ${ih.updated}, ${ih.summary});",
        record
      )

    given ResultSetGetter[List[String]] = new ResultSetGetter[List[String]]:
      override def get(rs: ResultSet)(index: Int): List[String] =
        rs.getString(index).split(",").toList

    override def fts(txt: String): Sql[Seq[IndexedHtml]] =
      Sql
        .prepareQuery[Tuple1[
          String
        ], (String, String, List[String], String, String, String, String, String)](
          str =>
            ps"SELECT * FROM indexed_html WHERE indexed_html.content MATCH $str ORDER BY rank",
          txt.tuple1
        )
        .map(_.map((IndexedHtml.apply).tupled))

    override def byTag(tag: String): Sql[Seq[IndexedHtml]] =
      Sql
        .prepareQuery[Tuple1[
          String
        ], (String, String, List[String], String, String, String, String, String)](
          str =>
            ps"SELECT * FROM indexed_html WHERE indexed_html.tags MATCH $str ORDER BY rank",
          tag.tuple1
        )
        .map(_.map((IndexedHtml.apply).tupled))

    override def index(): Unit = {

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
              description =
                html.select("meta[name=description]").attr("content"),
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

      init.executePool
      indexedData.foreach { record =>
        ingest(record).executePool
      }

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

      Files.writeString(
        _thisBuild / "tags.json",
        JsonArray(tagList).toJsonString
      )
    }
  }
}
