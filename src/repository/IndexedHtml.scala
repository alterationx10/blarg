package repository

import dev.wishingtree.branch.friday.JsonCodec
import dev.wishingtree.branch.macaroni.poolers.ResourcePool
import dev.wishingtree.branch.piggy.{ResultSetGetter, Sql}
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.piggy.Sql.*

import java.nio.file.Path
import java.sql.{Connection, DriverManager, ResultSet}

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

trait IndexedHtmlRepository {
  def init: Sql[Unit]
  def ingest(record: IndexedHtml): Sql[Unit]
  def fts(txt: String): Sql[Seq[IndexedHtml]]
  def byTag(tag: String): Sql[Seq[IndexedHtml]]
}

object IndexedHtmlRepository extends IndexedHtmlRepository {

  def connPool(path: Path): ResourcePool[Connection] =
    new ResourcePool[Connection] {
      val dbPath: Path = path / "index.sqlite"

      override def acquire: Connection =
        DriverManager.getConnection(s"jdbc:sqlite:${dbPath.toString}")

      override def release(resource: Connection): Unit = resource.close()
    }

  given ResultSetGetter[List[String]] = new ResultSetGetter[List[String]]:
    override def get(rs: ResultSet)(index: Int): List[String] =
      rs.getString(index).split(",").toList

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

  override def fts(txt: String): Sql[Seq[IndexedHtml]] =
    Sql
      .prepareQuery[Tuple1[
        String
      ], (String, String, List[String], String, String, String, String, String)](
        str =>
          ps"SELECT * FROM indexed_html WHERE indexed_html.content MATCH $str ORDER BY rank",
        txt.tuple1
      )
      .map(_.map(IndexedHtml.apply.tupled))

  override def byTag(tag: String): Sql[Seq[IndexedHtml]] =
    Sql
      .prepareQuery[Tuple1[
        String
      ], (String, String, List[String], String, String, String, String, String)](
        str =>
          ps"SELECT * FROM indexed_html WHERE indexed_html.tags MATCH $str ORDER BY rank",
        tag.tuple1
      )
      .map(_.map(IndexedHtml.apply.tupled))

}
