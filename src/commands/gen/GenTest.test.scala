package commands.gen

import commands.build.FrontMatter

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

class GenTest extends munit.FunSuite {

  val dtf: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd")
    .withZone(ZoneId.systemDefault())

  val siteFixture = FunFixture[os.Path](
    setup = { _ =>
      val dir = os.temp.dir(dir = os.pwd, prefix = "blarg-gen-test-")
      os.makeDir.all(dir / "blog")
      os.makeDir.all(dir / "pages")
      dir
    },
    teardown = { dir =>
      os.remove.all(dir)
    }
  )

  siteFixture.test("gen -b creates a blog post with date prefix") { siteDir =>
    val rel = siteDir.relativeTo(os.pwd).toString
    Gen.action(Seq("-d", rel, "-b", "My Test Post"))

    val today = dtf.format(Instant.now())
    val expected = siteDir / "blog" / s"$today-my-test-post.md"
    assert(os.exists(expected), s"Blog post should exist at $expected")

    val content = os.read(expected)
    assert(content.contains("title: My Test Post"), s"Should contain title, got: $content")
    assert(content.startsWith("---"), "Should start with YAML delimiter")
  }

  siteFixture.test("gen -b does not overwrite existing blog post") { siteDir =>
    val rel = siteDir.relativeTo(os.pwd).toString
    val today = dtf.format(Instant.now())
    val existing = siteDir / "blog" / s"$today-duplicate.md"
    os.write(existing, "original content")

    Gen.action(Seq("-d", rel, "-b", "Duplicate"))

    assertEquals(os.read(existing), "original content", "Existing file should not be overwritten")
  }

  siteFixture.test("gen -p creates a page with frontmatter") { siteDir =>
    val rel = siteDir.relativeTo(os.pwd).toString
    Gen.action(Seq("-d", rel, "-p", "new-page.md"))

    val expected = siteDir / "pages" / "new-page.md"
    assert(os.exists(expected), s"Page should exist at $expected")

    val content = os.read(expected)
    assert(content.startsWith("---"), "Should start with YAML delimiter")
  }

  siteFixture.test("gen -p does not overwrite existing page") { siteDir =>
    val rel = siteDir.relativeTo(os.pwd).toString
    val existing = siteDir / "pages" / "existing.md"
    os.write(existing, "original")

    Gen.action(Seq("-d", rel, "-p", "existing.md"))

    assertEquals(os.read(existing), "original")
  }

  siteFixture.test("gen -fm prepends frontmatter to existing file") { siteDir =>
    val rel = siteDir.relativeTo(os.pwd).toString
    val target = siteDir / "content.md"
    os.write(target, "# Existing Content")

    Gen.action(Seq("-d", rel, "-fm", rel + "/content.md"))

    val content = os.read(target)
    assert(content.startsWith("---"), s"Should start with frontmatter, got: $content")
    assert(content.contains("# Existing Content"), "Original content should be preserved")
  }

  siteFixture.test("gen -fm on missing file does nothing") { siteDir =>
    val rel = siteDir.relativeTo(os.pwd).toString
    val missing = siteDir / "nope.md"

    Gen.action(Seq("-d", rel, "-fm", rel + "/nope.md"))

    assert(!os.exists(missing), "Missing file should not be created")
  }

}
