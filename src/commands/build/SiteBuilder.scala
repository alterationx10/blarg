package commands.build

import dev.wishingtree.branch.macaroni.fs.PathOps.*
import org.commonmark.Extension
import org.commonmark.ext.front.matter.{
  YamlFrontMatterExtension,
  YamlFrontMatterVisitor
}
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
      val _thisBuild = root.getParent / "build"
      Files
        .walk(root / "static")
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            Files.createDirectories(
              _thisBuild / path.relativeTo(root / "static")
            )
          else
            Files.copy(
              path,
              _thisBuild / path.relativeTo(root / "static")
            )
        }
    }

    private def buildPages(root: Path): Unit = {
      val _thisRoot  = root / "site" / "pages"
      val _thisBuild = root.getParent / "build"
      Files
        .walk(_thisRoot)
        .filter(p => p.toString.endsWith(".md") || Files.isDirectory(p))
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            Files.createDirectories(
              _thisBuild / path.relativeTo(_thisRoot)
            )
          else
            val siteTemplate = ContentLoader(root).loadSiteTemplate().get
            val pageTemplate = ContentLoader(root).loadPageTemplate().get

            val siteHtml = Jsoup.parse(siteTemplate)
            val pageHtml = Jsoup.parse(pageTemplate)

            val rawContent    = ContentLoader(root).load(path).get
            val parsedContent = mdParser.parse(rawContent)

            val visitor     = new YamlFrontMatterVisitor()
            parsedContent.accept(visitor)
            val frontMatter = visitor.getData.asScala.toMap

            pageHtml
              .getElementById("page-content")
              .html(htmlRenderer.render(parsedContent))

            siteHtml
              .getElementById("site-content")
              .html(pageHtml.html())

            frontMatter.get("title").foreach { l =>
              l.asScala.headOption.foreach { title =>
                siteHtml.title(title)
              }
            }

            val fn = path
              .relativeTo(path.getParent)
              .toString
              .stripSuffix(".md") + ".html"

            val destination = path.relativeTo(_thisRoot).getNameCount match {
              case 1 => _thisBuild / fn
              case _ => _thisBuild / path.relativeTo(_thisRoot).getParent / fn
            }

            Files.writeString(
              destination,
              siteHtml.html()
            )
        }
    }

    private def buildBlog(root: Path): Unit = {
      val _thisRoot  = root / "site" / "blog"
      val _thisBuild = root.getParent / "build"
      val _thisBlog  = _thisBuild / "blog"

      Files
        .walk(_thisRoot)
        .filter(p => p.toString.endsWith(".md") || Files.isDirectory(p))
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            Files.createDirectories(
              _thisBlog / path.relativeTo(_thisRoot)
            )
          else
            val siteTemplate = ContentLoader(root).loadSiteTemplate().get
            val blogTemplate = ContentLoader(root).loadBlogTemplate().get

            val siteHtml = Jsoup.parse(siteTemplate)
            val blogHtml = Jsoup.parse(blogTemplate)

            val rawContent    = ContentLoader(root).load(path).get
            val parsedContent = mdParser.parse(rawContent)

            val visitor     = new YamlFrontMatterVisitor()
            parsedContent.accept(visitor)
            val frontMatter = visitor.getData.asScala.toMap

            blogHtml
              .getElementById("blog-content")
              .html(htmlRenderer.render(parsedContent))

            siteHtml
              .getElementById("site-content")
              .html(blogHtml.html())

            frontMatter.get("title").foreach { l =>
              l.asScala.headOption.foreach { title =>
                siteHtml.title(title)
              }
            }

            val fn: String = path
              .relativeTo(path.getParent)
              .toString
              .stripSuffix(".md")

            val destination = path.relativeTo(_thisRoot).getNameCount match {
              case 1 =>
                fn match {
                  case s"$year-$month-$day-$slug" =>
                    _thisBlog / year / month / day / s"$slug.html"
                  case _                          =>
                    _thisBlog / s"$fn.html"
                }
              case _ =>
                _thisBlog / path.relativeTo(_thisRoot).getParent / s"$fn.html"
            }

            if destination.getNameCount > 0 then
              Files.createDirectories(destination.getParent)

            Files.writeString(
              destination,
              siteHtml.html()
            )

        }

    }

    override def parseSite(): Unit = {
      buildPages(root)
      buildBlog(root)
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
