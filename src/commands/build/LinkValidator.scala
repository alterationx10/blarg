package commands.build

import scala.util.matching.Regex

object LinkValidator {

  private val hrefPattern: Regex = """href=["']([^"']+)["']""".r

  case class BrokenLink(
      sourcePage: String,
      targetHref: String,
      normalizedTarget: String
  )

  /**
   * Extract all href attributes from HTML content
   */
  def extractLinks(html: String): List[String] = {
    hrefPattern
      .findAllMatchIn(html)
      .map(_.group(1))
      .toList
  }

  /**
   * Check if a link is internal (not external URL)
   * Internal links start with "/" or are relative paths
   */
  def isInternalLink(href: String): Boolean = {
    !href.startsWith("http://") &&
    !href.startsWith("https://") &&
    !href.startsWith("mailto:") &&
    !href.startsWith("#")
  }

  /**
   * Normalize a link for comparison:
   * - Remove fragments (#anchor)
   * - Remove query strings
   * - Ensure it starts with /
   * - Add .html extension if missing and not a directory reference
   */
  def normalizeLink(href: String): String = {
    val withoutFragment = href.split('#').head
    val withoutQuery = withoutFragment.split('?').head

    val normalized = if !withoutQuery.startsWith("/") then
      "/" + withoutQuery
    else
      withoutQuery

    // If it doesn't end with .html or /, assume it's a file that should have .html
    if normalized.endsWith("/") || normalized.endsWith(".html") then
      normalized
    else if normalized.contains(".") then
      // Has an extension already (e.g., .css, .js, .png)
      normalized
    else
      // Assume it's an HTML page without the extension
      normalized + ".html"
  }

  /**
   * Validate links in content against available pages
   * Returns list of broken links with source page info
   */
  def validateLinks(
      contentMap: Map[String, String], // Map of page URL -> HTML content
      availablePages: Set[String]       // Set of all generated page URLs
  ): List[BrokenLink] = {
    contentMap.flatMap { case (sourceUrl, htmlContent) =>
      val links = extractLinks(htmlContent)
      val internalLinks = links.filter(isInternalLink)

      internalLinks.flatMap { href =>
        val normalized = normalizeLink(href)
        if !availablePages.contains(normalized) then
          Some(BrokenLink(sourceUrl, href, normalized))
        else
          None
      }
    }.toList
  }

  /**
   * Print warnings for broken links
   */
  def reportBrokenLinks(brokenLinks: List[BrokenLink]): Unit = {
    if brokenLinks.nonEmpty then {
      System.err.println(s"\nWARNING: Found ${brokenLinks.size} broken internal link(s):")
      brokenLinks.foreach { bl =>
        System.err.println(s"  In ${bl.sourcePage}:")
        System.err.println(s"    Link to '${bl.targetHref}' (resolves to '${bl.normalizedTarget}') - page not found")
      }
      System.err.println()
    }
  }

}
