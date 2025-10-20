package blarg.core

import blarg.core.pages.{StaticPage, ServerPage, WebViewPage}
import blarg.core.rendering.Markdown

import java.nio.file.{Path, Paths}

/**
 * Main site definition trait.
 *
 * Users extend this trait to define their site structure with static, server-rendered,
 * and WebView pages.
 *
 * Example:
 * {{{
 * object MySite extends BlargSite {
 *   def staticPages = Seq(
 *     HomePage(),
 *     AboutPage()
 *   ) ++ allBlogPosts
 *
 *   def serverPages = Seq(
 *     ProductPage()
 *   )
 *
 *   def webViewPages = Seq(
 *     DashboardPage()
 *   )
 *
 *   override def config = SiteConfig(
 *     siteTitle = "My Site",
 *     siteUrl = "https://example.com"
 *   )
 * }
 * }}}
 */
trait BlargSite {
  // Content directories
  def contentDir: Path = Paths.get("site")
  def templateDir: Path = contentDir.resolve("templates")
  def staticDir: Path = contentDir.resolve("static")

  // Page definitions
  def staticPages: Seq[StaticPage] = Seq.empty
  def serverPages: Seq[ServerPage] = Seq.empty
  def webViewPages: Seq[WebViewPage[?, ?]] = Seq.empty

  // Site metadata
  def config: SiteConfig = SiteConfig()

  // Convenience methods for loading markdown files
  def allMarkdownPages(subdir: String = "pages"): Seq[StaticPage] =
    Markdown.loadAllPages(contentDir.resolve(subdir))

  def allBlogPosts: Seq[StaticPage] =
    Markdown.loadAllPages(contentDir.resolve("blog"))
      .sortBy(_.frontMatter.flatMap(_.published).map(_.toEpochMilli).getOrElse(0L))
      .reverse
}

/**
 * Site configuration.
 *
 * @param siteTitle The title of the site
 * @param siteUrl The base URL of the site (e.g., "https://example.com")
 * @param port The port to run the server on
 * @param devMode Whether to enable development mode features (hot reload, DevTools)
 */
case class SiteConfig(
  siteTitle: String = "My Site",
  siteUrl: String = "http://localhost:8080",
  port: Int = 8080,
  devMode: Boolean = false
)
