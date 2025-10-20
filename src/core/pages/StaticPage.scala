package blarg.core.pages

import dev.alteration.branch.mustachio.Stache

/**
 * A static page that is pre-rendered at build time (SSG - Static Site Generation).
 *
 * Static pages are rendered once during `blarg build` and written to disk as HTML files.
 * They are served directly as static files (fastest possible delivery).
 *
 * Use for:
 * - Blog posts
 * - Documentation pages
 * - Marketing pages
 * - Any content that doesn't change per-request
 *
 * Example:
 * {{{
 * object HomePage extends StaticPage {
 *   def route = "/"
 *   def template = "home.mustache"
 *
 *   def render() = Stache.obj(
 *     "title" -> Stache.str("Welcome"),
 *     "hero" -> Stache.str("Build fast sites with Blarg")
 *   )
 * }
 * }}}
 */
trait StaticPage extends BlargPage {
  /**
   * Render the page to a Stache context.
   * Called once at build time.
   */
  def render(): Stache
}
