package commands.gen

import commands.build.FrontMatter
import munit.FunSuite

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

class GenSuite extends FunSuite {

  // --- toSlug unit tests ---

  test("toSlug should convert simple title to lowercase slug") {
    assertEquals(Gen.toSlug("My First Post"), "my-first-post")
    assertEquals(Gen.toSlug("Hello World"), "hello-world")
  }

  test("toSlug should handle single words") {
    assertEquals(Gen.toSlug("Hello"), "hello")
    assertEquals(Gen.toSlug("UPPERCASE"), "uppercase")
  }

  test("toSlug should replace spaces with hyphens") {
    assertEquals(Gen.toSlug("Multiple   Spaces"), "multiple-spaces")
    assertEquals(Gen.toSlug("One Two Three Four"), "one-two-three-four")
  }

  test("toSlug should remove special characters") {
    assertEquals(Gen.toSlug("What's this?"), "what-s-this")
    assertEquals(Gen.toSlug("Hello, World!"), "hello-world")
    assertEquals(Gen.toSlug("Post #1"), "post-1")
  }

  test("toSlug should handle punctuation") {
    assertEquals(Gen.toSlug("My Post!!!"), "my-post")
    assertEquals(Gen.toSlug("Question?"), "question")
    assertEquals(Gen.toSlug("Wow...Amazing"), "wow-amazing")
  }

  test("toSlug should handle quotes and apostrophes") {
    assertEquals(Gen.toSlug("It's \"quoted\""), "it-s-quoted")
    assertEquals(Gen.toSlug("Don't Stop"), "don-t-stop")
  }

  test("toSlug should handle parentheses and brackets") {
    assertEquals(Gen.toSlug("Post (Updated)"), "post-updated")
    assertEquals(Gen.toSlug("Guide [2025]"), "guide-2025")
    assertEquals(Gen.toSlug("{Scala} Tips"), "scala-tips")
  }

  test("toSlug should handle slashes and backslashes") {
    assertEquals(Gen.toSlug("Part 1/2"), "part-1-2")
    assertEquals(Gen.toSlug("Windows\\Path"), "windows-path")
  }

  test("toSlug should remove consecutive hyphens") {
    assertEquals(Gen.toSlug("Too --- Many -- Hyphens"), "too-many-hyphens")
    assertEquals(Gen.toSlug("A - B - C"), "a-b-c")
  }

  test("toSlug should trim leading and trailing hyphens") {
    assertEquals(Gen.toSlug("- Leading hyphen"), "leading-hyphen")
    assertEquals(Gen.toSlug("Trailing hyphen -"), "trailing-hyphen")
    assertEquals(Gen.toSlug("- Both -"), "both")
  }

  test("toSlug should handle leading and trailing spaces") {
    assertEquals(Gen.toSlug("  Spaces  "), "spaces")
    assertEquals(Gen.toSlug("   Lots of spaces   "), "lots-of-spaces")
  }

  test("toSlug should handle numbers") {
    assertEquals(Gen.toSlug("Top 10 Tips"), "top-10-tips")
    assertEquals(Gen.toSlug("2025 Guide"), "2025-guide")
    assertEquals(Gen.toSlug("Version 1.2.3"), "version-1-2-3")
  }

  test("toSlug should handle mixed case") {
    assertEquals(Gen.toSlug("CamelCase Title"), "camelcase-title")
    assertEquals(Gen.toSlug("ALL CAPS"), "all-caps")
    assertEquals(Gen.toSlug("MiXeD CaSe"), "mixed-case")
  }

  test("toSlug should handle special symbols") {
    assertEquals(Gen.toSlug("Cost: $100"), "cost-100")
    assertEquals(Gen.toSlug("50% Off"), "50-off")
    assertEquals(
      Gen.toSlug("Email: test@example.com"),
      "email-test-example-com"
    )
  }

  test("toSlug should handle ampersands") {
    assertEquals(Gen.toSlug("Rock & Roll"), "rock-roll")
    assertEquals(Gen.toSlug("Fish & Chips"), "fish-chips")
  }

  test("toSlug should handle underscores") {
    assertEquals(Gen.toSlug("snake_case_title"), "snake-case-title")
    assertEquals(Gen.toSlug("file_name.txt"), "file-name-txt")
  }

  test("toSlug should handle emoji and unicode") {
    // Emoji get stripped as non-alphanumeric
    assertEquals(Gen.toSlug("Hello 🎉 World"), "hello-world")
    assertEquals(Gen.toSlug("Party 🎈🎊🎉"), "party")
  }

  test("toSlug should handle empty string") {
    assertEquals(Gen.toSlug(""), "")
  }

  test("toSlug should handle only special characters") {
    assertEquals(Gen.toSlug("!!!"), "")
    assertEquals(Gen.toSlug("@#$%"), "")
    assertEquals(Gen.toSlug("---"), "")
  }

  test("toSlug should handle very long titles") {
    val longTitle =
      "This Is A Very Long Title That Goes On And On And Contains Many Words"
    val slug      = Gen.toSlug(longTitle)
    assert(slug.startsWith("this-is-a-very-long"))
    assert(slug.contains("many-words"))
    assertEquals(
      slug,
      "this-is-a-very-long-title-that-goes-on-and-on-and-contains-many-words"
    )
  }

  test("toSlug should create URL-safe slugs") {
    val titles = List(
      "My First Post",
      "What's New?",
      "C++ Tutorial",
      "50/50 Split",
      "Version 2.0"
    )

    titles.foreach { title =>
      val slug = Gen.toSlug(title)
      assert(
        slug.matches("^[a-z0-9-]*$"),
        s"Slug '$slug' contains invalid characters"
      )
      assert(!slug.startsWith("-"), s"Slug '$slug' starts with hyphen")
      assert(!slug.endsWith("-"), s"Slug '$slug' ends with hyphen")
      assert(!slug.contains("--"), s"Slug '$slug' contains consecutive hyphens")
    }
  }

  test("toSlug edge cases: accented characters") {
    assertEquals(Gen.toSlug("Café"), "caf")
    assertEquals(Gen.toSlug("Naïve"), "na-ve")
  }

  test("toSlug real-world examples") {
    assertEquals(
      Gen.toSlug("How to Build a Static Site Generator"),
      "how-to-build-a-static-site-generator"
    )
    assertEquals(
      Gen.toSlug("Understanding Scala 3's New Features"),
      "understanding-scala-3-s-new-features"
    )
    assertEquals(
      Gen.toSlug("10 Tips for Better Code"),
      "10-tips-for-better-code"
    )
    assertEquals(
      Gen.toSlug("My Journey: From Beginner to Expert"),
      "my-journey-from-beginner-to-expert"
    )
    assertEquals(
      Gen.toSlug("What I Learned (The Hard Way)"),
      "what-i-learned-the-hard-way"
    )
  }

  test("toSlug should be idempotent for already-slugified strings") {
    val slug = "already-a-valid-slug"
    assertEquals(Gen.toSlug(slug), slug)
  }

  test("toSlug consistency: same input always produces same output") {
    val title = "Consistent Title!"
    val slug1 = Gen.toSlug(title)
    val slug2 = Gen.toSlug(title)
    val slug3 = Gen.toSlug(title)
    assertEquals(slug1, slug2)
    assertEquals(slug2, slug3)
  }

  // --- gen action integration tests ---

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
