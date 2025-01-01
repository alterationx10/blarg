package commands.build

import dev.wishingtree.branch.macaroni.fs.PathOps.*
import org.commonmark.Extension
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup

import java.nio.file.{Files, Path}
import java.util.Comparator
import scala.jdk.CollectionConverters.*
import scala.util.Try

trait SiteBuilder {
  def copyStatic(): Unit
  def parseSite(): Unit
  def cleanBuild(): Unit
}

object SiteBuilder {

  def apply(root: Path): SiteBuilder = new SiteBuilder {

    val fmExtension: Extension =
      YamlFrontMatterExtension.create()

    val mdParser: Parser =
      Parser
        .builder()
        .extensions(
          List(
            fmExtension
          ).asJava
        )
        .build()

    val htmlRenderer: HtmlRenderer =
      HtmlRenderer.builder().build()

    override def copyStatic(): Unit = Try {
      Files
        .walk(root / "static")
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            Files.createDirectories(
              root.getParent / "build" / path.relativeTo(root / "static")
            )
          else
            Files.copy(
              path,
              root.getParent / "build" / path.relativeTo(root / "static")
            )
        }
    }

    private def buildPages(root: Path): Unit = {
      val _thisRoot = root / "site" / "pages"
      Files
        .walk(_thisRoot)
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            println(path.relativeTo(_thisRoot))
            Files.createDirectories(
              root.getParent / "build" / path.relativeTo(_thisRoot)
            )
          else
            println(path.relativeTo(_thisRoot))
            val pageTemplate = ContentLoader(root).loadPageTemplate().get
            val pageHtml = Jsoup.parse(pageTemplate)
            val pageContent = ContentLoader(root).load(path).get
            val content = pageHtml.getElementById("page-content").html(htmlRenderer.render(mdParser.parse(pageContent)))
            val fn = path.relativeTo(path.getParent).toString.stripSuffix(".md") + ".html"
            Files.writeString(
              root.getParent / "build" / path.relativeTo(_thisRoot) / path.getParent / fn,
              pageHtml.html()
            )
        }

      ???
    }

    override def parseSite(): Unit = {
      ???
    }

    override def cleanBuild(): Unit =
      Try {
        Files
          .walk(root.getParent / "build")
          .sorted(Comparator.reverseOrder()) // Files before Dirs
          .forEach(Files.deleteIfExists(_))
      }
  }

}
