package blarg.core.pages

import dev.alteration.branch.mustachio.Stache
import blarg.core.routing.PageRequest
import scala.concurrent.duration.*

/**
 * A server-rendered page (SSR - Server-Side Rendering).
 *
 * Server pages are rendered on each request (or cached) at runtime.
 * They can access request parameters, query strings, and generate dynamic content.
 *
 * Use for:
 * - Dynamic product pages
 * - Search results
 * - Personalized content
 * - Database-driven pages
 *
 * Example:
 * {{{
 * object ProductPage extends ServerPage {
 *   def route = "/products/:id"
 *   def template = "product.mustache"
 *
 *   def render(req: PageRequest) = {
 *     val product = ProductDB.findById(req.params("id"))
 *     Stache.obj(
 *       "name" -> Stache.str(product.name),
 *       "price" -> Stache.num(product.price)
 *     )
 *   }
 * }
 * }}}
 */
trait ServerPage extends BlargPage {
  /**
   * Render the page to a Stache context based on the request.
   * Called on each request (unless cached).
   */
  def render(req: PageRequest): Stache

  /**
   * Optional cache TTL for this page.
   * - Some(duration) = cache for specified duration
   * - None = no caching (always render fresh)
   *
   * Default: 5 minutes
   */
  def cacheTTL: Option[Duration] = Some(5.minutes)
}
