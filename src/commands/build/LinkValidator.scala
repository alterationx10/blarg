package commands.build

import scala.util.matching.Regex

object LinkValidator {

  private val hrefPattern: Regex = """href=["']([^"']+)["']""".r

  case class BrokenLink(
      sourcePage: String,
      targetHref: String,
      normalizedTarget: String,
      attemptedPaths: List[String]
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
   * Check if a link is internal and absolute (not external URL or relative path)
   * Only validates absolute internal links starting with "/"
   * Relative paths (like "../other.html") are skipped for validation
   */
  def isInternalLink(href: String): Boolean = {
    !href.startsWith("http://") &&
    !href.startsWith("https://") &&
    !href.startsWith("mailto:") &&
    !href.startsWith("#") &&
    href.startsWith("/")  // Only validate absolute internal links
  }

  /**
   * Normalize a link for comparison:
   * - Remove fragments (#anchor)
   * - Remove query strings
   * - Add .html extension if missing and not a directory reference
   * Note: This function expects absolute paths (starting with /)
   */
  def normalizeLink(href: String): String = {
    val withoutFragment = href.split('#').head
    val normalized = withoutFragment.split('?').head

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
   * Check if a normalized link exists in available pages
   * Supports directory indexes: /other or /other.html matches /other/index.html
   * Returns (exists: Boolean, attemptedPaths: List[String])
   */
  def linkExists(normalized: String, availablePages: Set[String]): (Boolean, List[String]) = {
    val attempts = scala.collection.mutable.ListBuffer[String]()

    // Direct match
    attempts += normalized
    if availablePages.contains(normalized) then
      return (true, attempts.toList)

    // Check for directory index: /other.html -> /other/index.html
    if normalized.endsWith(".html") then {
      val pathWithoutHtml = normalized.stripSuffix(".html")
      val withIndex = pathWithoutHtml + "/index.html"
      attempts += withIndex
      if availablePages.contains(withIndex) then
        return (true, attempts.toList)
    }

    // Check for directory index: /other -> /other/index.html
    if !normalized.endsWith("/") && !normalized.contains(".") then {
      val withIndex = normalized + "/index.html"
      attempts += withIndex
      if availablePages.contains(withIndex) then
        return (true, attempts.toList)
    }

    // Check for directory index with slash: /other/ -> /other/index.html
    if normalized.endsWith("/") then {
      val withIndex = normalized + "index.html"
      attempts += withIndex
      if availablePages.contains(withIndex) then
        return (true, attempts.toList)
    }

    (false, attempts.toList)
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
        val (exists, attemptedPaths) = linkExists(normalized, availablePages)
        if !exists then
          Some(BrokenLink(sourceUrl, href, normalized, attemptedPaths))
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
        val pathsText = if bl.attemptedPaths.size == 1 then
          bl.attemptedPaths.head
        else
          bl.attemptedPaths.mkString(" and ")
        System.err.println(s"    Link to '${bl.targetHref}' (tried: $pathsText) - page not found")
      }
      System.err.println()
    }
  }

}
