package blarg.core.server

import blarg.core.BlargSite
import blarg.core.routing.BlargRouter

import dev.alteration.branch.spider.webview.WebViewServer
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

import java.net.InetSocketAddress
import java.nio.file.{Files, Path, Paths}
import scala.util.Using

/**
 * Unified server for Blarg sites.
 *
 * Serves:
 * - Static pages (pre-built HTML from /build)
 * - SSR pages (rendered per-request with caching)
 * - WebView pages (reactive components with WebSocket)
 * - Static assets (CSS, JS, images from /static)
 *
 * All on the same port (WebSocket upgrade for WebView).
 */
object BlargServer {

  /**
   * Start the unified Blarg server.
   *
   * This method:
   * 1. Builds static pages to disk (if any)
   * 2. Creates Spider HTTP server
   * 3. Registers static file serving
   * 4. Registers SSR routes
   * 5. Registers WebView components
   * 6. Starts the server
   *
   * @param site The site definition
   */
  def start(site: BlargSite): Unit = {
    val port = site.config.port
    val devMode = site.config.devMode

    println(s"🚀 Starting Blarg server...")

    // Step 1: Build static pages (if any)
    if (site.staticPages.nonEmpty) {
      println(s"📄 Building ${site.staticPages.length} static pages...")
      SiteBuilder.build(site, Paths.get("build"))
    }

    // Step 2: Create HTTP server
    val server = HttpServer.create(new InetSocketAddress(port), 0)

    // Step 3: Serve pre-built static pages
    val buildDir = Paths.get("build")
    if (Files.exists(buildDir)) {
      server.createContext("/", createFileHandler(buildDir))
      println(s"📁 Serving static pages from /build")
    }

    // Step 4: Serve static assets
    if (Files.exists(site.staticDir)) {
      server.createContext("/static", createFileHandler(site.staticDir))
      println(s"🎨 Serving static assets from ${site.staticDir}")
    }

    // Step 5: Register SSR routes
    if (site.serverPages.nonEmpty) {
      // TODO: Implement SSR route handling with updated Spider API
      println(s"⚠️  SSR routes not yet implemented (${site.serverPages.length} pages)")
      site.serverPages.foreach { page =>
        println(s"   - ${page.route}")
      }
    }

    // Step 6: Register WebView components (if any)
    if (site.webViewPages.nonEmpty) {
      // TODO: Integrate WebView server properly
      println(s"⚠️  WebView integration not yet implemented (${site.webViewPages.length} pages)")
      site.webViewPages.foreach { page =>
        println(s"   - ${page.route}")
      }
    }

    // Step 7: Start the HTTP server
    server.start()

    println()
    println(s"✅ Blarg server running at http://localhost:$port")
    println()
    println(s"Site: ${site.config.siteTitle}")
    println(s"Static pages: ${site.staticPages.length}")
    println(s"SSR pages: ${site.serverPages.length}")
    println(s"WebView pages: ${site.webViewPages.length}")

    if (devMode) {
      println()
      println("🛠️  Dev mode enabled (hot reload, DevTools)")
    }

    // Keep server running
    println("Press Ctrl+C to stop")
    Thread.currentThread().join()
  }

  private def createFileHandler(baseDir: Path): HttpHandler = new HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val path = exchange.getRequestURI.getPath
      val file = baseDir.resolve(if (path == "/") "index.html" else path.stripPrefix("/"))

      if (Files.exists(file) && Files.isRegularFile(file)) {
        val bytes = Files.readAllBytes(file)
        exchange.sendResponseHeaders(200, bytes.length)
        Using.resource(exchange.getResponseBody)(_.write(bytes))
      } else {
        val notFound = "404 Not Found".getBytes
        exchange.sendResponseHeaders(404, notFound.length)
        Using.resource(exchange.getResponseBody)(_.write(notFound))
      }
    }
  }

  /**
   * Start a simple static file server.
   *
   * Used for Simple Mode (markdown-only sites without Scala code).
   *
   * @param port The port to run on
   */
  def serveStatic(port: Int = 8080): Unit = {
    println(s"🚀 Starting static file server...")

    val buildDir = Paths.get("build")
    val staticDir = Paths.get("site/static")

    val server = HttpServer.create(new InetSocketAddress(port), 0)

    if (Files.exists(buildDir)) {
      server.createContext("/", createFileHandler(buildDir))
    }

    if (Files.exists(staticDir)) {
      server.createContext("/static", createFileHandler(staticDir))
    }

    server.start()

    println(s"✅ Serving static site at http://localhost:$port")
    println("Press Ctrl+C to stop")
    Thread.currentThread().join()
  }
}
