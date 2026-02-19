package commands.newsite

import config.SiteConfig
import upickle.default.read

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

class NewSiteSuite extends munit.FunSuite {

  val siteFixture = FunFixture[os.Path](
    setup = { _ =>
      os.temp.dir(dir = os.pwd, prefix = "blarg-newsite-test-")
    },
    teardown = { dir =>
      os.remove.all(dir)
    }
  )

  siteFixture.test("creates expected directory structure") { tmpDir =>
    val rel = tmpDir.relativeTo(os.pwd).toString
    NewSite.action(Seq("my-site", "-d", rel))

    val site = tmpDir / "my-site" / "site"
    assert(os.isDir(site / "blog"), "blog dir should exist")
    assert(os.isDir(site / "pages"), "pages dir should exist")
    assert(os.isDir(site / "static" / "js"), "static/js dir should exist")
    assert(os.isDir(site / "static" / "css"), "static/css dir should exist")
    assert(os.isDir(site / "static" / "img"), "static/img dir should exist")
    assert(os.isDir(site / "templates"), "templates dir should exist")
  }

  siteFixture.test("writes a valid blarg.json config") { tmpDir =>
    val rel = tmpDir.relativeTo(os.pwd).toString
    NewSite.action(Seq("my-site", "-d", rel))

    val configPath = tmpDir / "my-site" / "site" / "blarg.json"
    assert(os.exists(configPath), "blarg.json should exist")

    val config = read[SiteConfig](os.read(configPath))
    assert(config.siteTitle.nonEmpty, "siteTitle should not be empty")
  }

  siteFixture.test("copies all mustache templates from resources") { tmpDir =>
    val rel = tmpDir.relativeTo(os.pwd).toString
    NewSite.action(Seq("my-site", "-d", rel))

    val templates = tmpDir / "my-site" / "site" / "templates"
    assert(os.exists(templates / "site.mustache"), "site.mustache should exist")
    List("blog", "latest", "page", "tags").foreach { name =>
      val f = templates / "pages" / s"$name.mustache"
      assert(os.exists(f), s"pages/$name.mustache should exist")
      assert(os.read(f).nonEmpty, s"pages/$name.mustache should not be empty")
    }
    List("header", "nav", "footer").foreach { name =>
      val f = templates / "partials" / s"$name.mustache"
      assert(os.exists(f), s"partials/$name.mustache should exist")
    }
  }

  siteFixture.test("copies static resources") { tmpDir =>
    val rel = tmpDir.relativeTo(os.pwd).toString
    NewSite.action(Seq("my-site", "-d", rel))

    val staticDir = tmpDir / "my-site" / "site" / "static"
    assert(os.exists(staticDir / "img" / "favicon.png"), "favicon.png should exist")
    assert(os.exists(staticDir / "img" / "logo.png"), "logo.png should exist")
  }

  siteFixture.test("copies default pages") { tmpDir =>
    val rel = tmpDir.relativeTo(os.pwd).toString
    NewSite.action(Seq("my-site", "-d", rel))

    val pages = tmpDir / "my-site" / "site" / "pages"
    assert(os.exists(pages / "about.md"), "about.md should exist")
    assert(os.exists(pages / "index.md"), "index.md should exist")
  }

  siteFixture.test("generates a first blog post with date prefix") { tmpDir =>
    val rel = tmpDir.relativeTo(os.pwd).toString
    NewSite.action(Seq("my-site", "-d", rel))

    val dtf = DateTimeFormatter
      .ofPattern("yyyy-MM-dd")
      .withZone(ZoneId.systemDefault())
    val today = dtf.format(Instant.now())

    val blogDir = tmpDir / "my-site" / "site" / "blog"
    val blogPost = blogDir / s"$today-first-post.md"
    assert(os.exists(blogPost), s"First blog post should exist at $blogPost")

    val content = os.read(blogPost)
    assert(content.contains("title: First Post"), s"Should contain title, got: $content")
    assert(content.contains("first-post"), "Should have first-post tag")
    assert(content.contains("blarg"), "Should have blarg tag")
    assert(content.contains("# First Post"), "Should contain markdown heading")
  }

  siteFixture.test("creates .gitignore in project root") { tmpDir =>
    val rel = tmpDir.relativeTo(os.pwd).toString
    NewSite.action(Seq("my-site", "-d", rel))

    val gitignore = tmpDir / "my-site" / ".gitignore"
    assert(os.exists(gitignore), ".gitignore should exist")
  }

}
