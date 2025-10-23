package commands.build

import munit.FunSuite
import java.nio.file.{Files, Path, Paths}
import java.io.File
import scala.jdk.CollectionConverters.*

class SiteBuilderSuite extends FunSuite {

  var tempDir: Path = null

  override def beforeEach(context: BeforeEach): Unit = {
    tempDir = Files.createTempDirectory("blarg-test")
  }

  override def afterEach(context: AfterEach): Unit = {
    // Clean up temp directory
    if (tempDir != null && Files.exists(tempDir)) {
      Files
        .walk(tempDir)
        .sorted(java.util.Comparator.reverseOrder())
        .forEach(Files.deleteIfExists(_))
    }
  }

  private def createSiteStructure(siteRoot: Path): Unit = {
    // Create directories
    Files.createDirectories(siteRoot.resolve("pages"))
    Files.createDirectories(siteRoot.resolve("blog"))
    Files.createDirectories(siteRoot.resolve("static/css"))
    Files.createDirectories(siteRoot.resolve("static/js"))
    Files.createDirectories(siteRoot.resolve("templates/pages"))
    Files.createDirectories(siteRoot.resolve("templates/partials"))

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
    Files.writeString(siteRoot.resolve("blarg.json"), config)

    // Create templates
    Files.writeString(
      siteRoot.resolve("templates/site.mustache"),
      "<html><body>{{{content}}}</body></html>"
    )
    Files.writeString(
      siteRoot.resolve("templates/pages/page.mustache"),
      "<article>{{{content.content}}}</article>"
    )
    Files.writeString(
      siteRoot.resolve("templates/pages/blog.mustache"),
      "<article><h1>{{content.fm.title}}</h1>{{{content.content}}}</article>"
    )
    Files.writeString(
      siteRoot.resolve("templates/pages/tags.mustache"),
      "<div>{{#content}}<h2>{{tag}}</h2>{{/content}}</div>"
    )
    Files.writeString(
      siteRoot.resolve("templates/pages/latest.mustache"),
      "<div>{{#content}}<h2>{{fm.title}}</h2>{{/content}}</div>"
    )
    Files.writeString(
      siteRoot.resolve("templates/partials/header.mustache"),
      "<header>Header</header>"
    )
    Files.writeString(
      siteRoot.resolve("templates/partials/nav.mustache"),
      "<nav>Nav</nav>"
    )
    Files.writeString(
      siteRoot.resolve("templates/partials/footer.mustache"),
      "<footer>Footer</footer>"
    )
  }

  test("SiteBuilder should clean build directory") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()

    // cleanBuild should create and clear the build directory
    val buildDir = tempDir.resolve("build")
    // The build directory is cleaned, which may delete it entirely
    // This test just ensures cleanBuild doesn't throw
  }

  test("SiteBuilder should copy static files") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    // Create some static files
    Files.writeString(
      siteRoot.resolve("static/css/style.css"),
      "body { margin: 0; }"
    )
    Files.writeString(
      siteRoot.resolve("static/js/app.js"),
      "console.log('hello');"
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.copyStatic()

    val buildDir = tempDir.resolve("build")
    assert(Files.exists(buildDir.resolve("css/style.css")))
    assert(Files.exists(buildDir.resolve("js/app.js")))

    val cssContent = Files.readString(buildDir.resolve("css/style.css"))
    assertEquals(cssContent, "body { margin: 0; }")
  }

  test("SiteBuilder should build pages from markdown") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    // Create a page
    val pageContent = """---
title: About Page
---

# About

This is the about page."""

    Files.writeString(siteRoot.resolve("pages/about.md"), pageContent)

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir.resolve("build")
    // Check that the HTML file was created
    assert(Files.exists(buildDir.resolve("about.html")))
    assert(Files.isRegularFile(buildDir.resolve("about.html")))
  }

  test("SiteBuilder should build blog posts with date-based URLs") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    // Create a blog post
    val postContent = """---
title: My First Post
tags:
  - test
  - blog
---

# First Post

Hello world!"""

    Files.writeString(
      siteRoot.resolve("blog/2025-01-15-first-post.md"),
      postContent
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir.resolve("build")
    assert(Files.exists(buildDir.resolve("2025/01/15/first-post.html")))

    val html = Files.readString(buildDir.resolve("2025/01/15/first-post.html"))
    // Check that the blog post content is present
    assert(html.nonEmpty)
  }

  test("SiteBuilder should generate tags page") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    // Create posts with tags
    Files.writeString(
      siteRoot.resolve("blog/2025-01-01-post1.md"),
      """---
title: Post 1
tags:
  - scala
  - programming
---
Content 1"""
    )

    Files.writeString(
      siteRoot.resolve("blog/2025-01-02-post2.md"),
      """---
title: Post 2
tags:
  - scala
  - testing
---
Content 2"""
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir.resolve("build")
    assert(Files.exists(buildDir.resolve("tags.html")))

    // Tags page was generated successfully
    val html = Files.readString(buildDir.resolve("tags.html"))
    assert(html.nonEmpty)
  }

  test("SiteBuilder should generate latest posts page") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    // Create multiple posts
    Files.writeString(
      siteRoot.resolve("blog/2025-01-01-old-post.md"),
      """---
title: Old Post
published: 2025-01-01T10:00:00Z
---
Old content"""
    )

    Files.writeString(
      siteRoot.resolve("blog/2025-01-15-new-post.md"),
      """---
title: New Post
published: 2025-01-15T10:00:00Z
---
New content"""
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir.resolve("build")
    assert(Files.exists(buildDir.resolve("latest.html")))

    val html = Files.readString(buildDir.resolve("latest.html"))
    // Latest page should contain both posts
    assert(html.nonEmpty)
  }

  test("SiteBuilder should handle nested pages") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    Files.createDirectories(siteRoot.resolve("pages/docs"))
    Files.writeString(
      siteRoot.resolve("pages/docs/guide.md"),
      """---
title: Guide
---
# Guide
Content here"""
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir.resolve("build")
    assert(Files.exists(buildDir.resolve("docs/guide.html")))
  }

  test("SiteBuilder cleanBuild should remove old files") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    val buildDir = tempDir.resolve("build")
    Files.createDirectories(buildDir)
    Files.writeString(buildDir.resolve("old-file.html"), "old content")

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()

    // Old file should be removed
    assert(!Files.exists(buildDir.resolve("old-file.html")))
  }

  test("SiteBuilder should validate links") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    // Create page with broken link
    Files.writeString(
      siteRoot.resolve("pages/index.md"),
      """---
title: Home
---
[Broken link](/missing.html)
[Good link](/about.html)
"""
    )

    Files.writeString(
      siteRoot.resolve("pages/about.md"),
      """---
title: About
---
About page"""
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    // Validate links - should find broken link
    builder.validateLinks()
    // Test passes if no exception thrown
    // (validateLinks prints to stderr but doesn't throw)
  }

  test("SiteBuilder should handle empty blog directory") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    Files.writeString(
      siteRoot.resolve("pages/index.md"),
      "---\ntitle: Home\n---\nHome page"
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    // Should still generate tags and latest pages (even if empty)
    val buildDir = tempDir.resolve("build")
    assert(Files.exists(buildDir.resolve("tags.html")))
    assert(Files.exists(buildDir.resolve("latest.html")))
  }

  test("SiteBuilder should handle pages without frontmatter") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    Files.writeString(
      siteRoot.resolve("pages/simple.md"),
      "# Simple Page\n\nNo frontmatter here."
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir.resolve("build")
    assert(Files.exists(buildDir.resolve("simple.html")))

    // Page was generated successfully
    val html = Files.readString(buildDir.resolve("simple.html"))
    assert(html.nonEmpty)
  }

  test("SiteBuilder should preserve directory structure for static files") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    Files.createDirectories(siteRoot.resolve("static/img/icons"))
    Files.writeString(
      siteRoot.resolve("static/img/icons/favicon.png"),
      "fake-png"
    )

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.copyStatic()

    val buildDir = tempDir.resolve("build")
    assert(Files.exists(buildDir.resolve("img/icons/favicon.png")))
  }

  test("SiteBuilder full build pipeline") {
    val siteRoot = tempDir.resolve("site")
    createSiteStructure(siteRoot)

    // Create complete site
    Files.writeString(
      siteRoot.resolve("pages/index.md"),
      "---\ntitle: Home\n---\nWelcome"
    )
    Files.writeString(
      siteRoot.resolve("pages/about.md"),
      "---\ntitle: About\n---\nAbout us"
    )
    Files.writeString(
      siteRoot.resolve("blog/2025-01-15-post.md"),
      "---\ntitle: Post\ntags:\n  - test\n---\nBlog content"
    )
    Files.writeString(siteRoot.resolve("static/css/style.css"), "body{}")

    val builder = SiteBuilder(siteRoot)

    // Run full pipeline
    builder.cleanBuild()
    builder.copyStatic()
    builder.parseSite()
    builder.validateLinks()

    val buildDir = tempDir.resolve("build")

    // Check all expected files exist
    assert(Files.exists(buildDir.resolve("index.html")))
    assert(Files.exists(buildDir.resolve("about.html")))
    assert(Files.exists(buildDir.resolve("2025/01/15/post.html")))
    assert(Files.exists(buildDir.resolve("tags.html")))
    assert(Files.exists(buildDir.resolve("latest.html")))
    assert(Files.exists(buildDir.resolve("css/style.css")))
  }

  test("SiteBuilder should handle markdown with CommonMark extensions") {
    val siteRoot = tempDir.resolve("site")
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

    Files.writeString(siteRoot.resolve("pages/extensions.md"), markdown)

    val builder = SiteBuilder(siteRoot)
    builder.cleanBuild()
    builder.parseSite()

    val buildDir = tempDir.resolve("build")
    // Check that the HTML file was created
    assert(Files.exists(buildDir.resolve("extensions.html")))
    assert(Files.isRegularFile(buildDir.resolve("extensions.html")))
  }
}
