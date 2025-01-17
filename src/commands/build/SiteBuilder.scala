package commands.build

import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.mustachio.Stache.Str
import dev.wishingtree.branch.mustachio.{Mustachio, Stache}
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup

import java.nio.file.{Files, Path}
import java.time.Year
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

    val _thisBuild: Path = root.getParent / "build"

    val mdParser: Parser = MDParser()

    val htmlRenderer: HtmlRenderer =
      HtmlRenderer.builder().build()

    override def copyStatic(): Unit = Try {
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
            val siteTemplate = ContentLoader(root).loadSiteTemplate()
            val pagePartial  = ContentLoader(root).loadPageTemplate()

            val content     = ContentLoader(root).load(path)
            val contentNode = mdParser.parse(content)

            val visitor     = new YamlFrontMatterVisitor()
            contentNode.accept(visitor)
            val frontMatter = visitor.getData.asScala.toMap
              .map((k, v) => (k -> v.asScala.toList))

            val fn = path
              .relativeTo(path.getParent)
              .toString
              .stripSuffix(".md") + ".html"

            val destination = path.relativeTo(_thisRoot).getNameCount match {
              case 1 => _thisBuild / fn
              case _ => _thisBuild / path.relativeTo(_thisRoot).getParent / fn
            }

            val ctx = BuildContext(
              page = Some(
                PageContext(
                  content = htmlRenderer.render(contentNode),
                  fm = FrontMatter(frontMatter),
                  href = "",   // TODO
                  summary = "" // TODO
                )
              )
            )

            val siteContent = Mustachio.render(
              siteTemplate,
              ctx,
              Some(
                Stache.obj(
                  "content" -> Str(pagePartial)
                )
              )
            )

            Files.writeString(
              destination,
              siteContent
            )
        }
    }

    private def buildTags(root: Path): Unit = {
      val _thisBuild = root.getParent / "build"

      val siteTemplate    = ContentLoader(root).loadSiteTemplate()
      val contentTemplate = ContentLoader(root).loadTagTemplate()

      val siteContent = Mustachio.render(
        siteTemplate,
        BuildContext(),
        Some(
          Stache.obj(
            "content" -> Str(contentTemplate)
          )
        )
      )

      Files.writeString(
        _thisBuild / "tags.html",
        siteContent
      )

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
            val siteTemplate = ContentLoader(root).loadSiteTemplate()
            val blogPartial  = ContentLoader(root).loadBlogTemplate()

            val content     = ContentLoader(root).load(path)
            val contentNode = mdParser.parse(content)

            val visitor     = new YamlFrontMatterVisitor()
            contentNode.accept(visitor)
            val frontMatter = visitor.getData.asScala.toMap
              .map((k, v) => (k -> v.asScala.toList))

            val ctx         = BuildContext(
              blog = Some(
                BlogContext(
                  content = htmlRenderer.render(contentNode),
                  fm = FrontMatter(frontMatter),
                  href = "",   // TODO
                  summary = "" // TODO
                )
              )
            )
            val siteContent = Mustachio.render(
              siteTemplate,
              ctx,
              Some(
                Stache.obj(
                  "content" -> Str(blogPartial)
                )
              )
            )

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
              siteContent
            )

        }

    }

    override def parseSite(): Unit = {
      buildPages(root)
      buildBlog(root)
//      Indexer.staticIndexer(_thisBuild).index()
      buildTags(root)
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
