package commands.build

import munit.FunSuite

class SiteBuilderSuite extends FunSuite {

  var tempDir: os.Path = null

  override def beforeEach(context: BeforeEach): Unit = {
    tempDir = os.temp.dir(prefix = "blarg-test")
  }

  override def afterEach(context: AfterEach): Unit = {
    if (tempDir != null && os.exists(tempDir)) {
      os.remove.all(tempDir)
    }
  }

  private def createSiteStructure(siteRoot: os.Path): Unit = {
    // Create directories
    os.makeDir.all(siteRoot / "pages")
    os.makeDir.all(siteRoot / "blog")
    os.makeDir.all(siteRoot / "static" / "css")
    os.makeDir.all(siteRoot / "static" / "js")
    os.makeDir.all(siteRoot / "templates" / "pages")
    os.makeDir.all(siteRoot / "templates" / "partials")

    // Create config
    val config = """
    {
      "siteTitle": "Test Site",
      "author": "Test Author",
      "navigation": [
        {"label": "Home", "href": "/"},
        {"label": "About", "href": "/about.html"}
      ],
      "dynamic": {}
    }
    """
    os.write(siteRoot / "blarg.json", config)

    // Create templates
    os.write(
      siteRoot / "templates" / "site.mustache",
      "<html><body>{{{content}}}</body></html>"
    )
    os.write(
      siteRoot / "templates" / "pages" / "page.mustache",
      "<article>{{{content.content}}}</article>"
    )
    os.write(
      siteRoot / "templates" / "pages" / "blog.mustache",
      "<article><h1>{{content.fm.title}}</h1>{{{content.content}}}</article>"
    )
    os.write(
      siteRoot / "templates" / "pages" / "tags.mustache",
      "<div>{{#content}}<h2>{{tag}}</h2>{{/content}}</div>"
    )
    os.write(
      siteRoot / "templates" / "pages" / "latest.mustache",
      "<div>{{#content}}<h2>{{fm.title}}</h2>{{/content}}</div>"
    )
    os.write(
      siteRoot / "templates" / "partials" / "header.mustache",
      "<header>Header</header>"
    )
    os.write(
      siteRoot / "templates" / "partials" / "nav.mustache",
      "<nav>Nav</nav>"
    )
    os.write(
      siteRoot / "templates" / "partials" / "footer.mustache",
      "<footer>Footer</footer>"
    )
  }

  test("SiteBuilder should clean build directory") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
  }

  test("SiteBuilder should copy static files") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.write(siteRoot / "static" / "css" / "style.css", "body { margin: 0; }")
    os.write(siteRoot / "static" / "js" / "app.js", "console.log('hello');")

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.copyStatic()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "css" / "style.css"))
    assert(os.exists(buildDir / "js" / "app.js"))

    val cssContent = os.read(buildDir / "css" / "style.css")
    assertEquals(cssContent, "body { margin: 0; }")
  }

  test("SiteBuilder should build pages from markdown") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    val pageContent = """---
title: About Page
---

# About

This is the about page."""

    os.write(siteRoot / "pages" / "about.md", pageContent)

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "about.html"))
  }

  test("SiteBuilder should build blog posts with date-based URLs") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    val postContent = """---
title: My First Post
tags:
  - test
  - blog
---

# First Post

Hello world!"""

    os.write(siteRoot / "blog" / "2025-01-15-first-post.md", postContent)

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "2025" / "01" / "15" / "first-post.html"))

    val html = os.read(buildDir / "2025" / "01" / "15" / "first-post.html")
    assert(html.nonEmpty)
  }

  test("SiteBuilder should generate tags page") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.write(
      siteRoot / "blog" / "2025-01-01-post1.md",
      "---\ntitle: Post 1\ntags:\n  - scala\n  - programming\n---\nContent 1"
    )
    os.write(
      siteRoot / "blog" / "2025-01-02-post2.md",
      "---\ntitle: Post 2\ntags:\n  - scala\n  - testing\n---\nContent 2"
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "tags.html"))

    val html = os.read(buildDir / "tags.html")
    assert(html.nonEmpty)
  }

  test("SiteBuilder should generate latest posts page") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.write(
      siteRoot / "blog" / "2025-01-01-old-post.md",
      "---\ntitle: Old Post\npublished: 2025-01-01T10:00:00Z\n---\nOld content"
    )
    os.write(
      siteRoot / "blog" / "2025-01-15-new-post.md",
      "---\ntitle: New Post\npublished: 2025-01-15T10:00:00Z\n---\nNew content"
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "latest.html"))

    val html = os.read(buildDir / "latest.html")
    assert(html.nonEmpty)
  }

  test("SiteBuilder should handle nested pages") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.makeDir.all(siteRoot / "pages" / "docs")
    os.write(
      siteRoot / "pages" / "docs" / "guide.md",
      "---\ntitle: Guide\n---\n# Guide\nContent here"
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "docs" / "guide.html"))
  }

  test("SiteBuilder cleanBuild should remove old files") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    val buildDir = tempDir / "build"
    os.makeDir.all(buildDir)
    os.write(buildDir / "old-file.html", "old content")

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()

    assert(!os.exists(buildDir / "old-file.html"))
  }

  test("SiteBuilder should validate links") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.write(
      siteRoot / "pages" / "index.md",
      "---\ntitle: Home\n---\n[Broken link](/missing.html)\n[Good link](/about.html)\n"
    )
    os.write(
      siteRoot / "pages" / "about.md",
      "---\ntitle: About\n---\nAbout page"
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()
    builder.validateLinks()
  }

  test("SiteBuilder should handle empty blog directory") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.write(
      siteRoot / "pages" / "index.md",
      "---\ntitle: Home\n---\nHome page"
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "tags.html"))
    assert(os.exists(buildDir / "latest.html"))
  }

  test("SiteBuilder should handle pages without frontmatter") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.write(
      siteRoot / "pages" / "simple.md",
      "# Simple Page\n\nNo frontmatter here."
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "simple.html"))

    val html = os.read(buildDir / "simple.html")
    assert(html.nonEmpty)
  }

  test("SiteBuilder should preserve directory structure for static files") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.makeDir.all(siteRoot / "static" / "img" / "icons")
    os.write(siteRoot / "static" / "img" / "icons" / "favicon.png", "fake-png")

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.copyStatic()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "img" / "icons" / "favicon.png"))
  }

  test("SiteBuilder full build pipeline") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.write(siteRoot / "pages" / "index.md", "---\ntitle: Home\n---\nWelcome")
    os.write(siteRoot / "pages" / "about.md", "---\ntitle: About\n---\nAbout us")
    os.write(
      siteRoot / "blog" / "2025-01-15-post.md",
      "---\ntitle: Post\ntags:\n  - test\n---\nBlog content"
    )
    os.write(siteRoot / "static" / "css" / "style.css", "body{}")

    val builder = SiteBuilder(siteRoot)

    builder.cleanBuild()
    builder.copyStatic()
    builder.parseSite()
    builder.validateLinks()

    val buildDir = tempDir / "build"

    assert(os.exists(buildDir / "index.html"))
    assert(os.exists(buildDir / "about.html"))
    assert(os.exists(buildDir / "2025" / "01" / "15" / "post.html"))
    assert(os.exists(buildDir / "tags.html"))
    assert(os.exists(buildDir / "latest.html"))
    assert(os.exists(buildDir / "css" / "style.css"))
  }

  test("SiteBuilder should handle markdown with CommonMark extensions") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    val markdown = """---
title: Extensions
---

# Markdown Extensions

| Feature | Supported |
|---------|-----------|
| Tables  | Yes       |

~~Strikethrough~~ works too.

[Link](https://example.com) gets autolinked.
"""

    os.write(siteRoot / "pages" / "extensions.md", markdown)

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "extensions.html"))
  }

  test("SiteBuilder parseSite is idempotent when cleanBuild is called first") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.write(siteRoot / "pages" / "index.md", "---\ntitle: Home\n---\nWelcome")
    os.write(
      siteRoot / "blog" / "2025-01-15-first-post.md",
      "---\ntitle: First Post\ntags:\n  - intro\n  - test\n---\nFirst Post"
    )
    os.write(siteRoot / "static" / "css" / "style.css", "body{}")

    val builder = SiteBuilder(siteRoot)

    builder.cleanBuild()
    builder.copyStatic()
    builder.parseSite()

    builder.cleanBuild()
    builder.copyStatic()
    builder.parseSite()

    val buildDir = tempDir / "build"
    assert(os.exists(buildDir / "index.html"))
    assert(os.exists(buildDir / "tags.html"))
    assert(os.exists(buildDir / "latest.html"))
  }

  test("SiteBuilder blog post hrefs use forward slashes") {
    val siteRoot = tempDir / "site"
    createSiteStructure(siteRoot)

    os.write(
      siteRoot / "blog" / "2025-01-15-first-post.md",
      "---\ntitle: First Post\ntags:\n  - test\n---\nFirst Post"
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val firstPost = tempDir / "build" / "2025" / "01" / "15" / "first-post.html"
    val content = os.read(firstPost)
    assert(!content.contains("\\"), s"Paths should use forward slashes, got: $content")
  }
}
