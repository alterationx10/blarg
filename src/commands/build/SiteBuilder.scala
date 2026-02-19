package commands.build

import config.SiteConfig
import mustachio.Stache.Str
import mustachio.{Mustachio, Stache}
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.node.{Node, Paragraph}
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.markdown.MarkdownRenderer

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Try

trait SiteBuilder {
  def copyStatic(): Unit
  def parseSite(): Unit
  def cleanBuild(): Unit
  def validateLinks(): Unit
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

  def apply(siteFolder: os.Path): SiteBuilder = new SiteBuilder {

    val _thisBuild: os.Path = siteFolder / os.up / "build"

    val mdParser: Parser = MDParser()

    val htmlRenderer: HtmlRenderer =
      HtmlRenderer.builder().extensions(MDParser.extensions.asJava).build()

    def buildTimestamp: String = java.time.Instant.now().toString
    def buildYear: Int         = java.time.Year.now().getValue

    // Track all generated pages for link validation
    private val generatedPages: mutable.Map[String, String] = mutable.Map.empty
    // Track static files as available resources
    private val staticFiles: mutable.Set[String]            = mutable.Set.empty

    lazy val siteConfig: SiteConfig = {
      val configPath = siteFolder / "blarg.json"
      if !os.exists(configPath) then {
        System.err.println(s"ERROR: Config file not found at: $configPath")
        System.err.println(
          s"Please create a blarg.json file or run 'blarg new' to create a new site."
        )
        System.exit(1)
        throw new RuntimeException("unreachable")
      }

      Try(upickle.default.read[SiteConfig](os.read(configPath))) match {
        case scala.util.Success(cfg) => cfg
        case scala.util.Failure(ex)  =>
          System.err.println(
            s"ERROR: Failed to parse config file at: $configPath"
          )
          System.err.println(s"Reason: ${ex.getMessage}")
          System.err.println(
            s"Please check your blarg.json is valid JSON with all required fields."
          )
          System.exit(1)
          throw new RuntimeException("unreachable")
      }
    }

    val contentLoader: ContentLoader = ContentLoader(siteFolder)

    def partials(contentPartial: String): Stache.Obj = {
      Stache.obj(
        "header"  -> Str(contentLoader.loadHeaderPartial()),
        "nav"     -> Str(contentLoader.loadNavPartial()),
        "footer"  -> Str(contentLoader.loadFooterPartial()),
        "content" -> Str(contentPartial)
      )
    }

    override def copyStatic(): Unit = Try {
      val staticDir = siteFolder / "static"
      if os.exists(staticDir) then {
        os.walk(staticDir).sorted.foreach { path =>
          val dest = _thisBuild / path.relativeTo(staticDir)
          if os.isDir(path) then
            os.makeDir.all(dest)
          else {
            os.copy(path, dest, createFolders = true, replaceExisting = true)
            // Track static file for link validation
            val fileUrl = "/" + dest.relativeTo(_thisBuild)
            staticFiles.add(fileUrl)
          }
        }
      }
    }.recover { case ex =>
      System.err.println(
        s"WARNING: Failed to copy static files: ${ex.getMessage}"
      )
    }

    private def buildContent(
        sourceFolder: os.Path,
        templateName: String,
        templateLoader: () => String,
        urlBuilder: (os.Path, os.Path, String) => os.Path
    ): List[ContentContext] = {
      val contentCollection: mutable.ListBuffer[ContentContext] =
        mutable.ListBuffer.empty

      if !os.exists(sourceFolder) then return List.empty

      os.walk(sourceFolder)
        .filter(p => p.ext == "md" || os.isDir(p))
        .sorted
        .foreach { path =>
          if os.isDir(path) then
            os.makeDir.all(_thisBuild / path.relativeTo(sourceFolder))
          else {
            val siteTemplate   = contentLoader.loadSiteTemplate()
            val contentPartial = templateLoader()

            val content     = contentLoader.load(path)
            val contentNode = mdParser.parse(content)

            val visitor     = new YamlFrontMatterVisitor()
            contentNode.accept(visitor)
            val frontMatter = visitor.getData.asScala.toMap
              .map((k, v) => (k -> v.asScala.toList))

            val fn = path.last.stripSuffix(".md")

            val destination = urlBuilder(path, sourceFolder, fn)

            if destination.segments.length > 0 then
              os.makeDir.all(destination / os.up)

            val cctx = ContentContext(
              content = htmlRenderer.render(contentNode),
              fm = FrontMatter(frontMatter),
              href = "/" + destination.relativeTo(_thisBuild),
              summary = summary(contentNode)
            )

            contentCollection.addOne(cctx)

            val ctx = BuildContext(
              content = cctx,
              config = siteConfig,
              buildTime = buildTimestamp,
              year = buildYear
            )

            val siteContent = Mustachio.render(
              siteTemplate,
              ctx,
              Some(partials(contentPartial))
            )

            os.write.over(destination, siteContent)

            // Track generated page for link validation
            val pageUrl = "/" + destination.relativeTo(_thisBuild)
            generatedPages.put(pageUrl, siteContent)
          }
        }
      contentCollection.toList
    }

    private def buildPages(): List[ContentContext] = {
      buildContent(
        siteFolder / "pages",
        "page",
        () => contentLoader.loadPageTemplate(),
        (path, sourceFolder, fn) => {
          val htmlFn = fn + ".html"
          val relToSource = path.relativeTo(sourceFolder)
          relToSource.segments.length match {
            case 1 => _thisBuild / htmlFn
            case _ =>
              _thisBuild / (path / os.up).relativeTo(sourceFolder) / htmlFn
          }
        }
      )
    }

    private def buildTags(
        contentList: List[ContentContext]
    ): Unit = {

      val siteTemplate    = contentLoader.loadSiteTemplate()
      val contentTemplate = contentLoader.loadTagTemplate()

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
          content = cctx,
          config = siteConfig,
          buildTime = buildTimestamp,
          year = buildYear
        ),
        Some(partials(contentTemplate))
      )

      os.write(_thisBuild / "tags.html", siteContent)

      // Track generated page for link validation
      generatedPages.put("/tags.html", siteContent)

    }

    private def buildBlog(): List[ContentContext] = {
      buildContent(
        siteFolder / "blog",
        "blog",
        () => contentLoader.loadBlogTemplate(),
        (path, sourceFolder, fn) => {
          val relToSource = path.relativeTo(sourceFolder)
          relToSource.segments.length match {
            case 1 =>
              // Parse date-based filename: YYYY-MM-DD-slug.md
              fn match {
                case s"$year-$month-$day-$slug" =>
                  _thisBuild / year / month / day / s"$slug.html"
                case _                          =>
                  _thisBuild / s"$fn.html"
              }
            case _ =>
              _thisBuild / (path / os.up).relativeTo(sourceFolder) / s"$fn.html"
          }
        }
      )
    }

    private def buildLatest(
        contentList: List[ContentContext]
    ): Unit = {

      val siteTemplate    = contentLoader.loadSiteTemplate()
      val contentTemplate = contentLoader.loadLatestTemplate()

      val cctx = Stache.Arr(
        contentList.map(ContentContext.given_Conversion_ContentContext_Stache)
      )

      val siteContent = Mustachio.render(
        siteTemplate,
        BuildContext(
          content = cctx,
          config = siteConfig,
          buildTime = buildTimestamp,
          year = buildYear
        ),
        Some(partials(contentTemplate))
      )

      os.write(_thisBuild / "latest.html", siteContent)

      // Track generated page for link validation
      generatedPages.put("/latest.html", siteContent)

    }

    override def parseSite(): Unit = {
      val pageContent = buildPages()
      val blogContent = buildBlog()
      buildTags(pageContent ++ blogContent)
      buildLatest(blogContent.sortBy(_.fm.published).reverse)
    }

    override def cleanBuild(): Unit =
      Try {
        if os.exists(_thisBuild) then
          os.remove.all(_thisBuild)
        // Clear tracking data for new build
        generatedPages.clear()
        staticFiles.clear()
      }.recover { case ex =>
        System.err.println(
          s"WARNING: Failed to clean build directory: ${ex.getMessage}"
        )
      }

    override def validateLinks(): Unit = {
      // Combine all available pages (HTML pages + static resources)
      val availablePages = generatedPages.keySet.toSet ++ staticFiles.toSet

      // Validate links in all generated HTML pages
      val brokenLinks = LinkValidator.validateLinks(
        generatedPages.toMap,
        availablePages
      )

      // Report any broken links
      LinkValidator.reportBrokenLinks(brokenLinks)
    }
  }

}
