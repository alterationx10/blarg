package blarg.core.rendering

import blarg.core.pages.StaticPage
import dev.alteration.branch.mustachio.Stache

import org.commonmark.Extension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.footnotes.FootnotesExtension
import org.commonmark.ext.front.matter.{YamlFrontMatterExtension, YamlFrontMatterVisitor}
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension
import org.commonmark.ext.image.attributes.ImageAttributesExtension
import org.commonmark.ext.ins.InsExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*
import scala.jdk.StreamConverters.*

/**
 * Markdown utilities for loading and rendering markdown files.
 *
 * Supports CommonMark with extensions for:
 * - Front matter (YAML)
 * - Tables
 * - Strikethrough
 * - Autolinks
 * - Footnotes
 * - Heading anchors
 * - Image attributes
 * - Ins/underline
 */
object Markdown {

  private lazy val extensions: List[Extension] = List(
    AutolinkExtension.create(),
    StrikethroughExtension.create(),
    TablesExtension.create(),
    FootnotesExtension.create(),
    HeadingAnchorExtension.create(),
    InsExtension.create(),
    YamlFrontMatterExtension.create(),
    ImageAttributesExtension.create()
  )

  private lazy val parser: Parser = Parser
    .builder()
    .extensions(extensions.asJava)
    .build()

  private lazy val renderer: HtmlRenderer = HtmlRenderer
    .builder()
    .extensions(extensions.asJava)
    .build()

  /**
   * Convert markdown string to HTML.
   *
   * @param markdown The markdown content
   * @return Rendered HTML
   */
  def toHtml(markdown: String): String = {
    val document = parser.parse(markdown)
    renderer.render(document)
  }

  /**
   * Parse front matter and markdown from a string.
   *
   * @param content The full content (front matter + markdown)
   * @return Tuple of (FrontMatter, markdown content)
   */
  def parseFrontMatter(content: String): (FrontMatter, String) = {
    val document = parser.parse(content)

    // Extract front matter
    val visitor = new YamlFrontMatterVisitor()
    document.accept(visitor)
    val fmMap = visitor.getData.asScala.toMap.view.mapValues(_.asScala.toList).toMap

    val frontMatter = if (fmMap.nonEmpty) {
      FrontMatter(fmMap)
    } else {
      FrontMatter.blank()
    }

    // Extract markdown content (everything after front matter)
    val markdownContent = if (content.trim.startsWith("---")) {
      // Skip front matter block
      val lines = content.linesIterator.toList
      val endIndex = lines.tail.indexWhere(_.trim == "---")
      if (endIndex >= 0) {
        lines.drop(endIndex + 2).mkString("\n")
      } else {
        content
      }
    } else {
      content
    }

    (frontMatter, markdownContent)
  }

  /**
   * Load a single markdown file as a StaticPage.
   *
   * @param path Path to the markdown file
   * @return StaticPage instance
   */
  def loadPage(path: Path): StaticPage = {
    val content = Files.readString(path)
    val (frontMatter, markdown) = parseFrontMatter(content)
    val html = toHtml(markdown)

    // Capture the front matter in closure
    val capturedFrontMatter = frontMatter

    new StaticPage {
      def route: String = capturedFrontMatter.slug.getOrElse(pathToRoute(path))
      def template: String = capturedFrontMatter.template.getOrElse("page.mustache")
      override def layout: Option[String] = capturedFrontMatter.layout.orElse(Some("layouts/site.mustache"))
      override def frontMatter: Option[FrontMatter] = Some(capturedFrontMatter)

      def render(): Stache = {
        import dev.alteration.branch.mustachio.Stache.{Arr, Null as StacheNull, Str}

        Stache.obj(
          "title" -> Str(capturedFrontMatter.title.getOrElse("Untitled")),
          "description" -> Str(capturedFrontMatter.description.getOrElse("")),
          "content" -> Str(html),
          "author" -> Str(capturedFrontMatter.author.getOrElse("")),
          "published" -> capturedFrontMatter.published
            .map(i => Str(i.toString))
            .getOrElse(StacheNull),
          "lastUpdated" -> capturedFrontMatter.lastUpdated
            .map(i => Str(i.toString))
            .getOrElse(StacheNull),
          "tags" -> capturedFrontMatter.tags
            .map(tags => Arr(tags.map(Str.apply)))
            .getOrElse(StacheNull),
          "fm" -> capturedFrontMatter.toStache
        )
      }
    }
  }

  /**
   * Load all markdown files from a directory as StaticPages.
   *
   * @param dir Directory containing markdown files
   * @return Sequence of StaticPage instances
   */
  def loadAllPages(dir: Path): Seq[StaticPage] = {
    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      return Seq.empty
    }

    Files.walk(dir)
      .toScala(LazyList)
      .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".md"))
      .map(loadPage)
  }

  /**
   * Convert a file path to a route URL.
   *
   * Examples:
   * - site/blog/2025-01-15-my-post.md → /blog/my-post
   * - site/pages/about.md → /about
   */
  private def pathToRoute(path: Path): String = {
    val fileName = path.getFileName.toString.stripSuffix(".md")

    // Remove date prefix if present (e.g., "2025-01-15-my-post" → "my-post")
    val slug = fileName.replaceFirst("""^\d{4}-\d{2}-\d{2}-""", "")

    // Determine the route based on directory structure
    val pathStr = path.toString
    if (pathStr.contains("/blog/")) {
      s"/blog/$slug"
    } else if (pathStr.contains("/pages/")) {
      if (slug == "index") "/" else s"/$slug"
    } else {
      s"/$slug"
    }
  }
}
