import commands.build.{ContentLoader, SiteBuilder}
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import dev.wishingtree.branch.macaroni.fs.PathOps.*
import org.commonmark.ext.front.matter.*
import org.jsoup.Jsoup

import scala.jdk.CollectionConverters.*
import java.nio.file.{Files, Path, Paths}

object Main {

  val frontMatter = YamlFrontMatterExtension.create()
  val parser      = Parser
    .builder()
    .extensions(
      List(
        frontMatter
      ).asJava
    )
    .build()
  val renderer    = HtmlRenderer.builder().build()

  val content = Files.readString(
    wd / "test_site" / "site" / "blog" / "2025-01-01-new-year.md"
  )

  val siteTemplate = Files.readString(
    wd / "test_site" / "templates" / "site.html"
  )

  val blogTemplate = Files.readString(
    wd / "test_site" / "templates" / "blog.html"
  )

  val parsedBlog   = parser.parse(content)
  val visitor      = new YamlFrontMatterVisitor()
  parsedBlog.accept(visitor)
  val fmData       = visitor.getData.asScala.toMap
  val renderedBlog = renderer.render(parsedBlog)

  val blogDoc = Jsoup.parse(blogTemplate)
  blogDoc.getElementById("blog-content").html(renderedBlog)

  val siteDoc = Jsoup.parse(siteTemplate)
  fmData.get("title").foreach { title =>
    siteDoc.title(title.getFirst)
  }
  siteDoc.getElementById("site-content").html(blogDoc.html())

  def main(args: Array[String]): Unit = {
    println {
      val sb = SiteBuilder(wd / "test_site")
      sb.cleanBuild()
      sb.copyStatic()
      sb.parseSite()
    }
  }
}
