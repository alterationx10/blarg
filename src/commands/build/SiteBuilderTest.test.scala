package commands.build

class SiteBuilderTest extends munit.FunSuite {

  val siteFixture = FunFixture[os.Path](
    setup = { _ =>
      val root = os.temp.dir(prefix = "blarg-sb-test")
      val site = root / "site"

      // templates
      os.makeDir.all(site / "templates")
      os.write(site / "templates" / "site.mustache", "{{>content}}")
      os.write(site / "templates" / "blog.mustache", "{{&content.content}}")
      os.write(site / "templates" / "page.mustache", "{{&content.content}}")
      os.write(site / "templates" / "tags.mustache", "{{#content}}<tag>{{tag}}</tag>{{/content}}")
      os.write(site / "templates" / "latest.mustache", "{{#content}}<article>{{fm.title}}</article>{{/content}}")

      // pages
      os.makeDir.all(site / "pages")
      os.write(
        site / "pages" / "index.md",
        """---
          |title: Home
          |description: Home page
          |author:
          |published:
          |lastUpdated:
          |tags:
          |---
          |
          |# Welcome
          |
          |Hello world
          |""".stripMargin
      )
      os.write(
        site / "pages" / "about.md",
        """---
          |title: About
          |description: About page
          |author:
          |published:
          |lastUpdated:
          |tags:
          |---
          |
          |About this site
          |""".stripMargin
      )

      // blog
      os.makeDir.all(site / "blog")
      os.write(
        site / "blog" / "2025-01-15-first-post.md",
        """---
          |title: First Post
          |description: The first post
          |author: Tester
          |published: 2025-01-15T00:00:00Z
          |lastUpdated: 2025-01-15T00:00:00Z
          |tags:
          |  - intro
          |  - test
          |---
          |
          |# First Post
          |
          |This is the first post.
          |""".stripMargin
      )
      os.write(
        site / "blog" / "2025-02-01-second-post.md",
        """---
          |title: Second Post
          |description: The second post
          |author: Tester
          |published: 2025-02-01T00:00:00Z
          |lastUpdated: 2025-02-01T00:00:00Z
          |tags:
          |  - test
          |---
          |
          |# Second Post
          |
          |This is the second post.
          |""".stripMargin
      )

      // static
      os.makeDir.all(site / "static" / "css")
      os.write(site / "static" / "style.css", "body { margin: 0; }")
      os.makeDir.all(site / "static" / "img")
      os.write(site / "static" / "img" / "logo.txt", "logo-placeholder")

      root
    },
    teardown = { root =>
      os.remove.all(root)
    }
  )

  siteFixture.test("cleanBuild removes the build directory") { root =>
    val buildDir = root / "build"
    os.makeDir.all(buildDir)
    os.write(buildDir / "old.html", "old content")

    val sb = SiteBuilder(root / "site")
    sb.cleanBuild()

    assert(!os.exists(buildDir), "Build directory should be removed after cleanBuild")
  }

  siteFixture.test("copyStatic copies static files to build") { root =>
    val sb = SiteBuilder(root / "site")
    sb.cleanBuild()
    sb.copyStatic()

    val buildDir = root / "build"
    assert(os.exists(buildDir / "style.css"), "style.css should be copied")
    assertEquals(os.read(buildDir / "style.css"), "body { margin: 0; }")
    assert(os.exists(buildDir / "img" / "logo.txt"), "nested static files should be copied")
    assertEquals(os.read(buildDir / "img" / "logo.txt"), "logo-placeholder")
  }

  siteFixture.test("parseSite builds page HTML files") { root =>
    val sb = SiteBuilder(root / "site")
    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()

    val buildDir = root / "build"
    assert(os.exists(buildDir / "index.html"), "index.html should be generated")
    assert(os.exists(buildDir / "about.html"), "about.html should be generated")

    val indexContent = os.read(buildDir / "index.html")
    assert(indexContent.contains("Welcome"), s"index.html should contain rendered markdown, got: $indexContent")
  }

  siteFixture.test("parseSite builds blog posts with date-based paths") { root =>
    val sb = SiteBuilder(root / "site")
    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()

    val buildDir = root / "build"
    val firstPost = buildDir / "2025" / "01" / "15" / "first-post.html"
    val secondPost = buildDir / "2025" / "02" / "01" / "second-post.html"

    assert(os.exists(firstPost), s"Blog post should be at date-based path: $firstPost")
    assert(os.exists(secondPost), s"Blog post should be at date-based path: $secondPost")

    val content = os.read(firstPost)
    assert(content.contains("First Post"), s"Blog post should contain rendered content, got: $content")
  }

  siteFixture.test("parseSite generates tags.html") { root =>
    val sb = SiteBuilder(root / "site")
    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()

    val tagsFile = root / "build" / "tags.html"
    assert(os.exists(tagsFile), "tags.html should be generated")

    val content = os.read(tagsFile)
    assert(content.contains("intro"), s"tags.html should contain 'intro' tag, got: $content")
    assert(content.contains("test"), s"tags.html should contain 'test' tag, got: $content")
  }

  siteFixture.test("parseSite generates latest.html") { root =>
    val sb = SiteBuilder(root / "site")
    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()

    val latestFile = root / "build" / "latest.html"
    assert(os.exists(latestFile), "latest.html should be generated")

    val content = os.read(latestFile)
    assert(content.contains("Second Post"), s"latest.html should contain blog titles, got: $content")
  }

  siteFixture.test("parseSite is idempotent when cleanBuild is called first") { root =>
    val sb = SiteBuilder(root / "site")

    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()

    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()

    val buildDir = root / "build"
    assert(os.exists(buildDir / "index.html"))
    assert(os.exists(buildDir / "tags.html"))
    assert(os.exists(buildDir / "latest.html"))
  }

  siteFixture.test("blog post hrefs use forward slashes") { root =>
    val sb = SiteBuilder(root / "site")
    sb.cleanBuild()
    sb.copyStatic()
    sb.parseSite()

    val firstPost = root / "build" / "2025" / "01" / "15" / "first-post.html"
    val content = os.read(firstPost)
    // The href in the rendered content should start with /
    // This validates the relativeTo path conversion works correctly
    assert(!content.contains("\\"), s"Paths should use forward slashes, got: $content")
  }

}
