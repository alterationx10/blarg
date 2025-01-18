package commands.build

import dev.wishingtree.branch.macaroni.fs.PathOps.*
import dev.wishingtree.branch.mustachio.Stache.Str
import dev.wishingtree.branch.mustachio.{Mustachio, Stache}
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

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

    private def buildPages(root: Path): List[ContentContext] = {
      val _thisRoot  = root / "site" / "pages"
      val _thisBuild = root.getParent / "build"

      val contentCollection: mutable.ListBuffer[ContentContext] =
        mutable.ListBuffer.empty

      Files
        .walk(_thisRoot)
        .filter(p => p.toString.endsWith(".md") || Files.isDirectory(p))
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            Files.createDirectories(
              _thisBuild / path.relativeTo(_thisRoot)
            )
          else {
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

            val cctx = ContentContext(
              content = htmlRenderer.render(contentNode),
              fm = FrontMatter(frontMatter),
              href = "",   // TODO
              summary = "" // TODO
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
        root: Path,
        contentList: List[ContentContext]
    ): Unit = {
      val _thisBuild = root.getParent / "build"

      val siteTemplate    = ContentLoader(root).loadSiteTemplate()
      val contentTemplate = ContentLoader(root).loadTagTemplate()

      val sortedTags =
        contentList.flatMap(_.fm.tags.getOrElse(List.empty)).distinct.sorted

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

    private def buildBlog(root: Path): List[ContentContext] = {
      val _thisRoot  = root / "site" / "blog"
      val _thisBuild = root.getParent / "build"
      val _thisBlog  = _thisBuild / "blog"

      val contentCollection: mutable.ListBuffer[ContentContext] =
        mutable.ListBuffer.empty

      Files
        .walk(_thisRoot)
        .filter(p => p.toString.endsWith(".md") || Files.isDirectory(p))
        .sorted(Comparator.naturalOrder())
        .forEach { path =>
          if Files.isDirectory(path) then
            Files.createDirectories(
              _thisBlog / path.relativeTo(_thisRoot)
            )
          else {
            val siteTemplate = ContentLoader(root).loadSiteTemplate()
            val blogPartial  = ContentLoader(root).loadBlogTemplate()

            val content     = ContentLoader(root).load(path)
            val contentNode = mdParser.parse(content)

            val visitor     = new YamlFrontMatterVisitor()
            contentNode.accept(visitor)
            val frontMatter = visitor.getData.asScala.toMap
              .map((k, v) => (k -> v.asScala.toList))

            val cctx = ContentContext(
              content = htmlRenderer.render(contentNode),
              fm = FrontMatter(frontMatter),
              href = "",   // TODO
              summary = "" // TODO
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
      contentCollection.toList

    }

    private def buildLatest(
        root: Path,
        contentList: List[ContentContext]
    ): Unit = {
      val _thisBuild = root.getParent / "build"

      val siteTemplate    = ContentLoader(root).loadSiteTemplate()
      val contentTemplate = ContentLoader(root).loadLatestTemplate()

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
      val pageContent = buildPages(root)
      val blogContent = buildBlog(root)
      buildTags(root, pageContent ++ blogContent)
      buildLatest(root, blogContent.sortBy(_.fm.published).reverse)
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
