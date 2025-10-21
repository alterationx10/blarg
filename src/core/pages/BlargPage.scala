package dev.alteration.blarg.core.pages

import dev.alteration.blarg.core.rendering.FrontMatter
import dev.alteration.branch.mustachio.Stache

/**
 * Core abstraction for all page types in Blarg.
 * Provides a unified interface for static, server-rendered, and reactive pages.
 */
trait BlargPage {
  /** URL route for this page (e.g., "/", "/blog/my-post", "/products/:id") */
  def route: String

  /** Mustache template file path (relative to templates directory) */
  def template: String

  /** Optional layout template to wrap the page content */
  def layout: Option[String] = Some("site.mustache")

  /** Optional front matter metadata for SEO and page info */
  def frontMatter: Option[FrontMatter] = None
}
