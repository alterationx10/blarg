package commands.build

import java.time.Instant
import java.time.temporal.ChronoUnit

class FrontMatterTest extends munit.FunSuite {

  test("FrontMatter.apply parses a complete map") {
    val data = Map(
      "title"       -> List("My Title"),
      "description" -> List("A description"),
      "author"      -> List("Author"),
      "published"   -> List("2025-01-01T00:00:00Z"),
      "lastUpdated" -> List("2025-06-15T12:00:00Z"),
      "tags"        -> List("scala", "blog")
    )

    val fm = FrontMatter(data)
    assertEquals(fm.title, Some("My Title"))
    assertEquals(fm.description, Some("A description"))
    assertEquals(fm.author, Some("Author"))
    assertEquals(fm.published, Some(Instant.parse("2025-01-01T00:00:00Z")))
    assertEquals(fm.lastUpdated, Some(Instant.parse("2025-06-15T12:00:00Z")))
    assertEquals(fm.tags, Some(List("scala", "blog")))
  }

  test("FrontMatter.apply handles missing fields") {
    val fm = FrontMatter(Map.empty)
    assertEquals(fm.title, None)
    assertEquals(fm.description, None)
    assertEquals(fm.author, None)
    assertEquals(fm.published, None)
    assertEquals(fm.lastUpdated, None)
    assertEquals(fm.tags, None)
  }

  test("FrontMatter.apply handles invalid instant gracefully") {
    val data = Map(
      "published" -> List("not-a-date")
    )
    val fm = FrontMatter(data)
    assertEquals(fm.published, None)
  }

  test("FrontMatter.apply handles empty author string") {
    val data = Map(
      "author" -> List("")
    )
    val fm = FrontMatter(data)
    assertEquals(fm.author, Some(""))
  }

  test("FrontMatter.blank creates a FrontMatter with timestamps") {
    val fm = FrontMatter.blank()
    assertEquals(fm.title, None)
    assert(fm.published.isDefined)
    assert(fm.lastUpdated.isDefined)
  }

  test("FrontMatter.blank accepts a title") {
    val fm = FrontMatter.blank(Some("Test Title"))
    assertEquals(fm.title, Some("Test Title"))
  }

  test("FrontMatter.toContent round-trips through YAML front matter") {
    val fm = FrontMatter(
      title = Some("Test"),
      description = Some("Desc"),
      author = Some("Me"),
      published = Some(Instant.parse("2025-01-01T00:00:00Z")),
      lastUpdated = Some(Instant.parse("2025-01-01T00:00:00Z")),
      tags = Some(List("a", "b"))
    )
    val content = fm.toContent
    assert(content.startsWith("---"), s"Expected YAML delimiters, got: $content")
    assert(content.contains("title: Test"))
    assert(content.contains("description: Desc"))
    assert(content.contains("author: Me"))
    assert(content.contains("  - a"))
    assert(content.contains("  - b"))
  }

  test("FrontMatter.toContent handles empty tags") {
    val fm = FrontMatter(
      title = Some("Test"),
      description = None,
      author = None,
      published = None,
      lastUpdated = None,
      tags = None
    )
    val content = fm.toContent
    assert(!content.contains("  - "), s"Should have no tag items, got: $content")
  }

  test("FrontMatter to Stache conversion") {
    val fm: FrontMatter = FrontMatter(
      title = Some("Hello"),
      description = None,
      author = Some("Auth"),
      published = None,
      lastUpdated = None,
      tags = Some(List("x"))
    )
    val stache: mustachio.Stache = fm
    val obj = stache.asInstanceOf[mustachio.Stache.Obj]
    assertEquals(obj.value("title"), mustachio.Stache.Str("Hello"))
    assertEquals(obj.value("description"), mustachio.Stache.Null)
    assertEquals(obj.value("author"), mustachio.Stache.Str("Auth"))
  }

}
