package commands.build

import munit.FunSuite

class LinkValidatorSuite extends FunSuite {

  test("extractLinks should find all href attributes in HTML") {
    val html = """
      <a href="/page1">Link 1</a>
      <a href="https://example.com">External</a>
      <link href="/css/style.css" rel="stylesheet">
      <a href='/page2'>Single quotes</a>
      <a href="/page3#section">With fragment</a>
    """

    val links = LinkValidator.extractLinks(html)

    assertEquals(links.size, 5)
    assert(links.contains("/page1"))
    assert(links.contains("https://example.com"))
    assert(links.contains("/css/style.css"))
    assert(links.contains("/page2"))
    assert(links.contains("/page3#section"))
  }

  test("extractLinks should handle empty HTML") {
    val html  = "<div>No links here</div>"
    val links = LinkValidator.extractLinks(html)
    assertEquals(links.size, 0)
  }

  test("extractLinks should handle malformed HTML gracefully") {
    val html  =
      """<a href="/valid">Link</a> <a href="">Empty</a> <a>No href</a>"""
    val links = LinkValidator.extractLinks(html)
    assert(links.contains("/valid"))
    // Empty href is extracted but will be filtered later by isInternalLink
  }

  test("isInternalLink should identify absolute internal links") {
    assert(LinkValidator.isInternalLink("/about"))
    assert(LinkValidator.isInternalLink("/blog/post"))
    assert(LinkValidator.isInternalLink("/css/style.css"))
  }

  test("isInternalLink should reject external URLs") {
    assert(!LinkValidator.isInternalLink("http://example.com"))
    assert(!LinkValidator.isInternalLink("https://example.com/page"))
    assert(!LinkValidator.isInternalLink("https://example.com"))
  }

  test("isInternalLink should reject mailto links") {
    assert(!LinkValidator.isInternalLink("mailto:test@example.com"))
  }

  test("isInternalLink should reject fragment-only links") {
    assert(!LinkValidator.isInternalLink("#section"))
    assert(!LinkValidator.isInternalLink("#"))
  }

  test("isInternalLink should reject relative paths") {
    assert(!LinkValidator.isInternalLink("../other.html"))
    assert(!LinkValidator.isInternalLink("./sibling.html"))
    assert(!LinkValidator.isInternalLink("relative/path.html"))
  }

  test("normalizeLink should remove fragments") {
    assertEquals(
      LinkValidator.normalizeLink("/page#section"),
      "/page.html"
    )
    assertEquals(
      LinkValidator.normalizeLink("/about.html#top"),
      "/about.html"
    )
  }

  test("normalizeLink should remove query strings") {
    assertEquals(
      LinkValidator.normalizeLink("/search?q=test"),
      "/search.html"
    )
    assertEquals(
      LinkValidator.normalizeLink("/page.html?param=value"),
      "/page.html"
    )
  }

  test("normalizeLink should add .html to extensionless links") {
    assertEquals(
      LinkValidator.normalizeLink("/about"),
      "/about.html"
    )
    assertEquals(
      LinkValidator.normalizeLink("/blog/post"),
      "/blog/post.html"
    )
  }

  test("normalizeLink should preserve .html extension") {
    assertEquals(
      LinkValidator.normalizeLink("/about.html"),
      "/about.html"
    )
  }

  test("normalizeLink should preserve trailing slash") {
    assertEquals(
      LinkValidator.normalizeLink("/folder/"),
      "/folder/"
    )
  }

  test("normalizeLink should preserve other file extensions") {
    assertEquals(
      LinkValidator.normalizeLink("/css/style.css"),
      "/css/style.css"
    )
    assertEquals(
      LinkValidator.normalizeLink("/js/app.js"),
      "/js/app.js"
    )
    assertEquals(
      LinkValidator.normalizeLink("/img/logo.png"),
      "/img/logo.png"
    )
  }

  test("normalizeLink should handle complex cases") {
    assertEquals(
      LinkValidator.normalizeLink("/page?query=test#section"),
      "/page.html"
    )
  }

  test("linkExists should find exact matches") {
    val pages = Set("/about.html", "/index.html", "/blog/post.html")

    val (exists, _) = LinkValidator.linkExists("/about.html", pages)
    assert(exists)
  }

  test("linkExists should find directory index variations") {
    val pages = Set("/other/index.html")

    // /other.html should match /other/index.html
    val (exists1, attempts1) = LinkValidator.linkExists("/other.html", pages)
    assert(exists1)
    assert(attempts1.contains("/other/index.html"))

    // /other/ should match /other/index.html
    val (exists2, attempts2) = LinkValidator.linkExists("/other/", pages)
    assert(exists2)
    assert(attempts2.contains("/other/index.html"))
  }

  test("linkExists should return all attempted paths on failure") {
    val pages = Set("/about.html")

    val (exists, attempts) = LinkValidator.linkExists("/missing.html", pages)
    assert(!exists)
    assert(attempts.contains("/missing.html"))
  }

  test("linkExists should try multiple variations for index matching") {
    val pages = Set("/docs/index.html")

    val (exists, attempts) = LinkValidator.linkExists("/docs", pages)
    assert(exists)
    // Should have tried multiple paths before finding it
    assert(attempts.size >= 2)
  }

  test("validateLinks should find broken links in content") {
    val content        = Map(
      "/index.html" -> """<a href="/about">About</a> <a href="/missing">Missing</a>""",
      "/page.html"  -> """<a href="/exists">Exists</a>"""
    )
    val availablePages =
      Set("/index.html", "/page.html", "/about.html", "/exists.html")

    val brokenLinks = LinkValidator.validateLinks(content, availablePages)

    assertEquals(brokenLinks.size, 1)
    assertEquals(brokenLinks.head.sourcePage, "/index.html")
    assertEquals(brokenLinks.head.targetHref, "/missing")
  }

  test("validateLinks should ignore external links") {
    val content        = Map(
      "/index.html" -> """<a href="https://example.com">External</a>"""
    )
    val availablePages = Set("/index.html")

    val brokenLinks = LinkValidator.validateLinks(content, availablePages)
    assertEquals(brokenLinks.size, 0)
  }

  test("validateLinks should ignore fragment-only links") {
    val content        = Map(
      "/index.html" -> """<a href="#section">Section</a>"""
    )
    val availablePages = Set("/index.html")

    val brokenLinks = LinkValidator.validateLinks(content, availablePages)
    assertEquals(brokenLinks.size, 0)
  }

  test("validateLinks should validate static resources") {
    val content        = Map(
      "/index.html" -> """<link href="/css/style.css" rel="stylesheet">"""
    )
    val availablePages = Set("/index.html")

    val brokenLinks = LinkValidator.validateLinks(content, availablePages)
    assertEquals(brokenLinks.size, 1) // CSS file not found
    assertEquals(brokenLinks.head.targetHref, "/css/style.css")
  }

  test("validateLinks should pass when static resources exist") {
    val content        = Map(
      "/index.html" -> """<link href="/css/style.css" rel="stylesheet">
                          <img src="/img/logo.png">"""
    )
    val availablePages = Set("/index.html", "/css/style.css", "/img/logo.png")

    val brokenLinks = LinkValidator.validateLinks(content, availablePages)
    assertEquals(brokenLinks.size, 0)
  }

  test("validateLinks should handle multiple broken links from same page") {
    val content        = Map(
      "/index.html" -> """
        <a href="/missing1">Link 1</a>
        <a href="/missing2">Link 2</a>
        <a href="/exists">Link 3</a>
      """
    )
    val availablePages = Set("/index.html", "/exists.html")

    val brokenLinks = LinkValidator.validateLinks(content, availablePages)
    assertEquals(brokenLinks.size, 2)
    assert(brokenLinks.exists(_.targetHref == "/missing1"))
    assert(brokenLinks.exists(_.targetHref == "/missing2"))
  }

  test("validateLinks should handle links with query strings and fragments") {
    val content        = Map(
      "/index.html" -> """<a href="/about?ref=home#top">About</a>"""
    )
    val availablePages = Set("/index.html", "/about.html")

    val brokenLinks = LinkValidator.validateLinks(content, availablePages)
    assertEquals(brokenLinks.size, 0) // Should normalize and find /about.html
  }

  test("validateLinks integration: complex site structure") {
    val content = Map(
      "/index.html"                -> """
        <a href="/about">About</a>
        <a href="/blog/2025/01/15/post">Post</a>
        <a href="/docs/">Docs</a>
        <link href="/css/style.css">
      """,
      "/about.html"                -> """<a href="/">Home</a> <a href="/missing">Broken</a>""",
      "/blog/2025/01/15/post.html" -> """<a href="/">Home</a>"""
    )

    val availablePages = Set(
      "/index.html",
      "/about.html",
      "/blog/2025/01/15/post.html",
      "/docs/index.html",
      "/css/style.css"
    )

    val brokenLinks = LinkValidator.validateLinks(content, availablePages)

    // Should only find one broken link: /missing from about.html
    assertEquals(brokenLinks.size, 1)
    assertEquals(brokenLinks.head.sourcePage, "/about.html")
    assertEquals(brokenLinks.head.targetHref, "/missing")
  }

  test("reportBrokenLinks should handle empty list") {
    // Should not throw, just print nothing
    LinkValidator.reportBrokenLinks(List.empty)
  }

  test("BrokenLink case class should store all required data") {
    val brokenLink = LinkValidator.BrokenLink(
      sourcePage = "/index.html",
      targetHref = "/missing",
      normalizedTarget = "/missing.html",
      attemptedPaths = List("/missing.html", "/missing/index.html")
    )

    assertEquals(brokenLink.sourcePage, "/index.html")
    assertEquals(brokenLink.targetHref, "/missing")
    assertEquals(brokenLink.normalizedTarget, "/missing.html")
    assertEquals(brokenLink.attemptedPaths.size, 2)
  }
}
