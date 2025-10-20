package blarg.core.routing

import blarg.core.BlargSite
import blarg.core.pages.ServerPage
import blarg.core.rendering.Blarg
import com.sun.net.httpserver.{HttpExchange, HttpHandler}

import java.time.Instant
import java.net.URLDecoder
import scala.concurrent.duration.*
import scala.collection.concurrent.TrieMap
import scala.util.Using

/**
 * Router for server-rendered pages.
 *
 * Compiles ServerPage definitions into HTTP handlers with:
 * - Path parameter extraction (/products/:id)
 * - Query string parsing
 * - In-memory caching with TTL
 * - Automatic template rendering
 *
 * TODO: Update to use new Spider API when available
 */
object BlargRouter {

  private val cache = TrieMap[String, CachedPage]()

  case class CachedPage(html: String, timestamp: Instant, ttl: Duration)

  /**
   * Build HTTP handlers from a BlargSite's ServerPages.
   *
   * @param site The site definition
   * @return Map of route patterns to HTTP handlers
   */
  def buildRoutes(site: BlargSite): Map[String, HttpHandler] = {
    site.serverPages.map { page =>
      page.route -> buildHandler(page)
    }.toMap
  }

  /**
   * Build an HTTP handler for a single ServerPage.
   */
  private def buildHandler(page: ServerPage): HttpHandler = new HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val cacheTTL = page.cacheTTL.getOrElse(5.minutes)
      val path = exchange.getRequestURI.getPath
      val uri = exchange.getRequestURI.toString
      val cacheKey = s"${page.route}:$path"

      val html = if (cacheTTL.toMillis > 0) {
        // Caching enabled
        cache.get(cacheKey) match {
          case Some(cached) if cached.timestamp.plusMillis(cached.ttl.toMillis).isAfter(Instant.now()) =>
            // Cache hit
            cached.html

          case _ =>
            // Cache miss or expired - render and cache
            val pattern = parseRoutePattern(page.route)
            val segments = parseRoutePattern(path)
            val rendered = renderPage(page, path, segments, uri)
            cache.put(cacheKey, CachedPage(rendered, Instant.now(), cacheTTL))
            rendered
        }
      } else {
        // No caching - always render fresh
        val pattern = parseRoutePattern(page.route)
        val segments = parseRoutePattern(path)
        renderPage(page, path, segments, uri)
      }

      val bytes = html.getBytes("UTF-8")
      exchange.getResponseHeaders.set("Content-Type", "text/html; charset=utf-8")
      exchange.sendResponseHeaders(200, bytes.length)
      Using.resource(exchange.getResponseBody)(_.write(bytes))
    }
  }

  /**
   * Render a ServerPage with request context.
   */
  private def renderPage(
    page: ServerPage,
    path: String,
    segments: List[String],
    uri: String
  ): String = {
    val pattern = parseRoutePattern(page.route)
    val params = extractParams(pattern, segments)
    val query = parseQuery(uri)

    val pageRequest = PageRequest(path, params, query)
    val context = page.render(pageRequest)

    Blarg.renderTemplate(page.template, context, page.layout)
  }

  /**
   * Parse a route pattern into segments.
   *
   * Examples:
   * - "/" → List()
   * - "/blog" → List("blog")
   * - "/products/:id" → List("products", ":id")
   */
  private def parseRoutePattern(route: String): List[String] = {
    route.split("/").filter(_.nonEmpty).toList
  }

  /**
   * Check if path segments match the route pattern.
   */
  private def matchesPattern(pattern: List[String], segments: List[String]): Boolean = {
    if (pattern.length != segments.length) {
      false
    } else {
      pattern.zip(segments).forall {
        case (p, s) if p.startsWith(":") => true  // Parameter matches anything
        case (p, s) => p == s                      // Literal must match exactly
      }
    }
  }

  /**
   * Extract path parameters from segments based on pattern.
   *
   * Example:
   * - pattern: List("products", ":id")
   * - segments: List("products", "123")
   * - result: Map("id" -> "123")
   */
  private def extractParams(pattern: List[String], segments: List[String]): Map[String, String] = {
    pattern.zip(segments).collect {
      case (p, s) if p.startsWith(":") =>
        val paramName = p.stripPrefix(":")
        paramName -> s
    }.toMap
  }

  /**
   * Parse query string from URI.
   *
   * Example:
   * - uri: "/search?q=scala&sort=date"
   * - result: Map("q" -> "scala", "sort" -> "date")
   */
  private def parseQuery(uri: String): Map[String, String] = {
    if (uri.contains("?")) {
      val queryString = uri.split("\\?", 2)(1)
      queryString.split("&").flatMap { param =>
        param.split("=", 2) match {
          case Array(key, value) =>
            Some(URLDecoder.decode(key, "UTF-8") -> URLDecoder.decode(value, "UTF-8"))
          case Array(key) =>
            Some(URLDecoder.decode(key, "UTF-8") -> "")
          case _ => None
        }
      }.toMap
    } else {
      Map.empty
    }
  }

  /**
   * Clear the entire route cache.
   * Useful for development mode or manual cache invalidation.
   */
  def clearCache(): Unit = {
    cache.clear()
  }

  /**
   * Invalidate a specific route from the cache.
   *
   * @param route The route pattern (e.g., "/products/:id")
   */
  def invalidate(route: String): Unit = {
    cache.filterInPlace((key, _) => !key.startsWith(route))
  }

  /**
   * Get cache statistics.
   */
  def cacheStats: Map[String, Any] = Map(
    "size" -> cache.size,
    "keys" -> cache.keys.toList.sorted
  )
}
