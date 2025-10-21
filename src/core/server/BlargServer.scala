package blarg.core.server

import dev.alteration.blarg.core.BlargSite
import dev.alteration.blarg.core.pages.ServerPage
import dev.alteration.blarg.core.server.SiteBuilder
import dev.alteration.branch.macaroni.extensions.*
import dev.alteration.branch.spider.server.*
import dev.alteration.branch.spider.server.RequestHandler.given
import dev.alteration.branch.spider.common.HttpMethod
import dev.alteration.branch.spider.webview.WebViewServer

import java.nio.file.{Files, Path, Paths}

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
   * 2. Creates WebView server (if needed) or SpiderServer
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

    // Step 2: Determine if we need WebView server or basic Spider server
    if (site.hasWebViewRoutes) {
      startWithWebView(site, port, devMode)
    } else {
      startBasicServer(site, port, devMode)
    }
  }

  /**
   * Start server with WebView support.
   */
  private def startWithWebView(site: BlargSite, port: Int, devMode: Boolean): Unit = {
    // Build WebView server with all routes
    var server = WebViewServer()

    // Register WebView pages using the site's callback
    server = site.registerWebViewRoutes(server)
    println(s"📱 WebView routes registered")

    // Register SSR pages
    if (site.serverPages.nonEmpty) {
      println(s"⚠️  SSR routes not yet supported with WebView server (${site.serverPages.length} pages)")
      site.serverPages.foreach { page =>
        println(s"   - ${page.route}")
      }
    }

    // Enable dev mode if requested
    if (devMode) {
      server = server.withDevMode(true)
      println("🛠️  Dev mode enabled (DevTools at /__devtools)")
    }

    // Serve static files from build and static directories
    val buildDir = Paths.get("build")
    val staticDir = site.staticDir

    if (Files.exists(buildDir)) {
      val buildFileHandler = new StaticFileHandler(buildDir)
      server = server.withHttpRoute("", buildFileHandler)
      println(s"📁 Serving static pages from /build")
    }

    if (Files.exists(staticDir)) {
      val staticFileHandler = new StaticFileHandler(staticDir)
      server = server.withHttpRoute("static", staticFileHandler)
      println(s"🎨 Serving static assets from ${staticDir}")
    }

    println()
    println(s"✅ Blarg server running at http://localhost:$port")
    println()
    println(s"Site: ${site.config.siteTitle}")
    println(s"Static pages: ${site.staticPages.length}")
    println(s"SSR pages: ${site.serverPages.length}")
    println()
    println("Press Ctrl+C to stop")

    // Start the server (blocking)
    server.start(port = port)
  }

  /**
   * Start basic Spider server without WebView.
   */
  private def startBasicServer(site: BlargSite, port: Int, devMode: Boolean): Unit = {
    val buildDir = Paths.get("build")
    val staticDir = site.staticDir

    // Create router for static files and SSR pages
    val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      // Serve static assets
      case (HttpMethod.GET, "static" :: path) if Files.exists(staticDir) =>
        new StaticFileHandler(staticDir)

      // SSR routes
      case (method, path) if isSSRRoute(site.serverPages, method, path) =>
        createSSRHandler(site.serverPages, method, path)

      // Serve pre-built static pages from /build
      case (HttpMethod.GET, path) if Files.exists(buildDir) =>
        new StaticFileHandler(buildDir)
    }

    val server = new SpiderServer(
      port = port,
      router = router,
      config = ServerConfig.default
    )

    println(s"📁 Serving static pages from /build")
    if (Files.exists(staticDir)) {
      println(s"🎨 Serving static assets from ${staticDir}")
    }

    if (site.serverPages.nonEmpty) {
      println(s"🔧 SSR routes (${site.serverPages.length} pages):")
      site.serverPages.foreach { page =>
        println(s"   - ${page.route}")
      }
    }

    println()
    println(s"✅ Blarg server running at http://localhost:$port")
    println()
    println(s"Site: ${site.config.siteTitle}")
    println(s"Static pages: ${site.staticPages.length}")
    println(s"SSR pages: ${site.serverPages.length}")
    println()
    println("Press Ctrl+C to stop")

    // Start the server (blocking)
    server.start()
  }

  /**
   * Check if a route matches an SSR page.
   */
  private def isSSRRoute(serverPages: Seq[ServerPage], method: HttpMethod, path: List[String]): Boolean = {
    val pathStr = "/" + path.mkString("/")
    serverPages.exists { page =>
      method == HttpMethod.GET && page.route == pathStr
    }
  }

  /**
   * Create a handler for an SSR page.
   */
  private def createSSRHandler(serverPages: Seq[ServerPage], method: HttpMethod, path: List[String]): RequestHandler[?, ?] = {
    val pathStr = "/" + path.mkString("/")
    serverPages.find(_.route == pathStr) match {
      case Some(page) => new ServerPageHandler(page)
      case None => NotFoundHandler()
    }
  }

  /**
   * Start a simple static file server.
   *
   * Used for Simple Mode (markdown-only sites without Scala code).
   *
   * @param port The port to run on
   * @param buildDir The directory containing built HTML files
   * @param staticDir Optional directory for static assets (served at /static)
   */
  def serveStatic(
    port: Int = 9000,
    buildDir: Path = Paths.get("build"),
    staticDir: Option[Path] = None
  ): Unit = {
    println(s"🚀 Starting static file server...")

    // Create handlers for serving static files
    val buildHandler = new StaticFileHandler(buildDir)

    val router: PartialFunction[(HttpMethod, List[String]), RequestHandler[?, ?]] = {
      // Serve static assets from the optional static directory
      case (HttpMethod.GET, "static" :: path) if staticDir.exists(Files.exists(_)) =>
        new StaticFileHandler(staticDir.get)

      // Serve all other GET requests from the build directory
      case (HttpMethod.GET, _) if Files.exists(buildDir) =>
        buildHandler
    }

    val server = new SpiderServer(
      port = port,
      router = router,
      config = ServerConfig.default
    )

    if (Files.exists(buildDir)) {
      println(s"📁 Serving files from: $buildDir")
    }
    staticDir.filter(Files.exists(_)).foreach { dir =>
      println(s"🎨 Serving static assets from: $dir")
    }
    println()
    println(s"✅ Server running at http://localhost:$port")
    println("Press Ctrl+C to stop")

    server.start()
  }
}

/**
 * Handler for server-rendered pages.
 */
private class ServerPageHandler(page: ServerPage) extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    // TODO: Implement actual SSR rendering with caching
    Response(200, s"<html><body><h1>SSR Page: ${page.route}</h1></body></html>")
  }
}

/**
 * Handler for 404 Not Found.
 */
private case class NotFoundHandler() extends RequestHandler[Unit, String] {
  override def handle(request: Request[Unit]): Response[String] = {
    Response(404, "<html><body><h1>404 Not Found</h1></body></html>")
  }
}

/**
 * Handler for serving static files from a directory.
 */
private class StaticFileHandler(rootDir: Path) extends RequestHandler[Unit, Array[Byte]] {
  override def handle(request: Request[Unit]): Response[Array[Byte]] = {
    val path = request.uri.getPath

    // Remove leading slash and resolve relative to root
    val relativePath = if (path == "/" || path.isEmpty) {
      "index.html"
    } else {
      path.stripPrefix("/")
    }

    val filePath = rootDir.resolve(relativePath)

    // Security check: ensure resolved path is within rootDir
    if (!filePath.normalize().startsWith(rootDir.normalize())) {
      return Response(403, "Forbidden".getBytes)
    }

    // Check if file exists
    if (!Files.exists(filePath)) {
      // If it's a directory, try index.html
      if (Files.isDirectory(filePath)) {
        val indexPath = filePath.resolve("index.html")
        if (Files.exists(indexPath)) {
          return serveFile(indexPath)
        }
      }
      return Response(404, "Not Found".getBytes)
    }

    // If it's a directory, try to serve index.html
    if (Files.isDirectory(filePath)) {
      val indexPath = filePath.resolve("index.html")
      if (Files.exists(indexPath)) {
        return serveFile(indexPath)
      } else {
        return Response(404, "Not Found".getBytes)
      }
    }

    serveFile(filePath)
  }

  private def serveFile(filePath: Path): Response[Array[Byte]] = {
    val bytes = Files.readAllBytes(filePath)
    val contentType = guessContentType(filePath)
    Response(200, bytes).withContentType(contentType)
  }

  private def guessContentType(filePath: Path): String = {
    val fileName = filePath.getFileName.toString.toLowerCase
    fileName match {
      case f if f.endsWith(".html") || f.endsWith(".htm") => "text/html; charset=utf-8"
      case f if f.endsWith(".css") => "text/css; charset=utf-8"
      case f if f.endsWith(".js") => "application/javascript; charset=utf-8"
      case f if f.endsWith(".json") => "application/json; charset=utf-8"
      case f if f.endsWith(".xml") => "application/xml; charset=utf-8"
      case f if f.endsWith(".png") => "image/png"
      case f if f.endsWith(".jpg") || f.endsWith(".jpeg") => "image/jpeg"
      case f if f.endsWith(".gif") => "image/gif"
      case f if f.endsWith(".svg") => "image/svg+xml"
      case f if f.endsWith(".ico") => "image/x-icon"
      case f if f.endsWith(".woff") => "font/woff"
      case f if f.endsWith(".woff2") => "font/woff2"
      case f if f.endsWith(".ttf") => "font/ttf"
      case f if f.endsWith(".eot") => "application/vnd.ms-fontobject"
      case f if f.endsWith(".pdf") => "application/pdf"
      case f if f.endsWith(".zip") => "application/zip"
      case _ => "application/octet-stream"
    }
  }
}
