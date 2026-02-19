package commands.build

import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.renderer.html.HtmlRenderer

import scala.jdk.CollectionConverters.*

class MDParserSuite extends munit.FunSuite {

  val parser = MDParser()
  // Renderer needs the same extensions as the parser to render extension nodes
  val renderer = HtmlRenderer.builder()
    .extensions(MDParser.extensions.asJava)
    .build()

  test("parses basic markdown to HTML") {
    val node = parser.parse("# Hello\n\nWorld")
    val html = renderer.render(node)
    assert(html.contains("<h1"), s"Expected h1 tag, got: $html")
    assert(html.contains("World"))
  }

  test("parses YAML front matter") {
    val md =
      """---
        |title: Test
        |tags:
        |  - a
        |  - b
        |---
        |
        |Content here
        |""".stripMargin
    val node    = parser.parse(md)
    val visitor = new YamlFrontMatterVisitor()
    node.accept(visitor)
    val data = visitor.getData.asScala.toMap.map((k, v) => (k -> v.asScala.toList))
    assertEquals(data("title"), List("Test"))
    assertEquals(data("tags"), List("a", "b"))
  }

  test("parses GFM tables") {
    val md = "| A | B |\n| --- | --- |\n| 1 | 2 |\n"
    val node = parser.parse(md)
    val html = renderer.render(node)
    assert(html.contains("<table"), s"Expected table tag, got: $html")
    assert(html.contains("<td>"), s"Expected td tag, got: $html")
  }

  test("parses strikethrough") {
    val node = parser.parse("hello ~~deleted~~ world")
    val html = renderer.render(node)
    assert(html.contains("<del>"), s"Expected del tag, got: $html")
  }

  test("parses autolinks") {
    val node = parser.parse("Visit https://example.com for details")
    val html = renderer.render(node)
    assert(html.contains("href=\"https://example.com\""), s"Expected autolink, got: $html")
  }

  test("parses heading anchors") {
    val node = parser.parse("# My Heading")
    val html = renderer.render(node)
    assert(html.contains("my-heading"), s"Expected heading anchor, got: $html")
  }

}
