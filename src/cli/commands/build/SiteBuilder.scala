package dev.alteration.blarg.cli.commands.build

import dev.alteration.branch.macaroni.extensions.PathExtensions.*
import dev.alteration.branch.mustachio.Stache.Str
import dev.alteration.branch.mustachio.{Mustachio, Stache}
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.node.{Node, Paragraph}
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.markdown.MarkdownRenderer

import java.nio.file.{Files, Path}
import java.util.Comparator
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Try

trait SiteBuilder {
  def copyStatic(): Unit
  def parseSite(): Unit
  def cleanBuild(): Unit
}

object SiteBuilder {

  private def summary(node: Node): String = {
    val child                 = node.getFirstChild
    def loop(c: Node): String = {
      c match {
        case null         => ""
        case p: Paragraph => MarkdownRenderer.builder().build().render(p)
        case n: Node      => loop(n.getNext)
      }
    }
    loop(child)
  }

  def apply(siteFolder: Path): SiteBuilder = new SiteBuilder {

    val _thisBuild: Path = siteFolder.getParent.resolve("build")

    val mdParser: Parser = MDParser()

    val htmlRenderer: HtmlRenderer =
      HtmlRenderer.builder().build()

    override def copyStatic(): Unit = Try {
      Files
        .walk(siteFolder.resolve("static"))
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            Files.createDirectories(
              _thisBuild / path.relativeTo(siteFolder / "static")
            )
          else
            Files.copy(
              path,
              _thisBuild / path.relativeTo(siteFolder / "static")
            )
        }
    }

    private def buildPages(): List[ContentContext] = {
      val pagesFolder = siteFolder / "pages"

      val contentCollection: mutable.ListBuffer[ContentContext] =
        mutable.ListBuffer.empty

      Files
        .walk(pagesFolder)
        .filter(p => p.toString.endsWith(".md") || Files.isDirectory(p))
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            Files.createDirectories(
              _thisBuild / path.relativeTo(pagesFolder)
            )
          else {
            val siteTemplate = ContentLoader(siteFolder).loadSiteTemplate()
            val pagePartial  = ContentLoader(siteFolder).loadPageTemplate()

            val content     = ContentLoader(siteFolder).load(path)
            val contentNode = mdParser.parse(content)

            val visitor     = new YamlFrontMatterVisitor()
            contentNode.accept(visitor)
            val frontMatter = visitor.getData.asScala.toMap
              .map((k, v) => (k -> v.asScala.toList))

            val fn = path
              .relativeTo(path.getParent)
              .toString
              .stripSuffix(".md") + ".html"

            val destination = path.relativeTo(pagesFolder).getNameCount match {
              case 1 => _thisBuild / fn
              case _ => _thisBuild / path.relativeTo(pagesFolder).getParent / fn
            }

            val cctx = ContentContext(
              content = htmlRenderer.render(contentNode),
              fm = FrontMatter(frontMatter),
              href = "/" + destination.relativeTo(_thisBuild),
              summary = summary(contentNode)
            )

            contentCollection.addOne(cctx)

            val ctx = BuildContext(
              content = cctx
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
      contentCollection.toList

    }

    private def buildTags(
        contentList: List[ContentContext]
    ): Unit = {

      val siteTemplate    = ContentLoader(siteFolder).loadSiteTemplate()
      val contentTemplate = ContentLoader(siteFolder).loadTagTemplate()

      val sortedTags =
        contentList
          .flatMap(_.fm.tags.getOrElse(List.empty))
          .distinct
          .filterNot(_.isBlank)
          .sorted

      val cctx = Stache.Arr(
        sortedTags.map { t =>
          Stache.obj(
            "tag"      -> Stache.Str(t),
            "articles" -> Stache.Arr(
              contentList
                .filter(_.fm.tags.getOrElse(List.empty).contains(t))
                .sortBy(_.fm.published)
                .map(ContentContext.given_Conversion_ContentContext_Stache)
            )
          )
        }
      )

      val siteContent = Mustachio.render(
        siteTemplate,
        BuildContext(
          content = cctx
        ),
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

    private def buildBlog(): List[ContentContext] = {
      val blogFolder = siteFolder / "blog"

      val contentCollection: mutable.ListBuffer[ContentContext] =
        mutable.ListBuffer.empty

      Files
        .walk(blogFolder)
        .filter(p => p.toString.endsWith(".md") || Files.isDirectory(p))
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            Files.createDirectories(
              _thisBuild / path.relativeTo(blogFolder)
            )
          else {
            val siteTemplate = ContentLoader(siteFolder).loadSiteTemplate()
            val blogPartial  = ContentLoader(siteFolder).loadBlogTemplate()

            val content     = ContentLoader(siteFolder).load(path)
            val contentNode = mdParser.parse(content)

            val visitor     = new YamlFrontMatterVisitor()
            contentNode.accept(visitor)
            val frontMatter = visitor.getData.asScala.toMap
              .map((k, v) => (k -> v.asScala.toList))

            val fn: String = path
              .relativeTo(path.getParent)
              .toString
              .stripSuffix(".md")

            val destination = path.relativeTo(blogFolder).getNameCount match {
              case 1 =>
                fn match {
                  case s"$year-$month-$day-$slug" =>
                    _thisBuild / year / month / day / s"$slug.html"
                  case _                          =>
                    _thisBuild / s"$fn.html"
                }
              case _ =>
                _thisBuild / path.relativeTo(blogFolder).getParent / s"$fn.html"
            }

            if destination.getNameCount > 0 then
              Files.createDirectories(destination.getParent)

            val cctx = ContentContext(
              content = htmlRenderer.render(contentNode),
              fm = FrontMatter(frontMatter),
              href = "/" + destination.relativeTo(_thisBuild),
              summary = summary(contentNode)
            )
            contentCollection.addOne(cctx)

            val ctx = BuildContext(
              content = cctx
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

            Files.writeString(
              destination,
              siteContent
            )
          }

        }
      contentCollection.toList

    }

    private def buildLatest(
        contentList: List[ContentContext]
    ): Unit = {

      val siteTemplate    = ContentLoader(siteFolder).loadSiteTemplate()
      val contentTemplate = ContentLoader(siteFolder).loadLatestTemplate()

      val sortedTags =
        contentList.flatMap(_.fm.tags.getOrElse(List.empty)).distinct.sorted

      val cctx = Stache.Arr(
        contentList.map(ContentContext.given_Conversion_ContentContext_Stache)
      )

      val siteContent = Mustachio.render(
        siteTemplate,
        BuildContext(
          content = cctx
        ),
        Some(
          Stache.obj(
            "content" -> Str(contentTemplate)
          )
        )
      )

      Files.writeString(
        _thisBuild / "latest.html",
        siteContent
      )

    }

    override def parseSite(): Unit = {
      val pageContent = buildPages()
      val blogContent = buildBlog()
      buildTags(pageContent ++ blogContent)
      buildLatest(blogContent.sortBy(_.fm.published).reverse)
    }

    override def cleanBuild(): Unit =
      Try {
        Files
          .walk(_thisBuild)
          .sorted(Comparator.reverseOrder()) // Files before Dirs
          .forEach(Files.deleteIfExists(_))
      }
  }

}
