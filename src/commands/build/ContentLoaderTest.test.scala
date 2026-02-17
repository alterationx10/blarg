package commands.build

class ContentLoaderTest extends munit.FunSuite {

  val tmpDir = FunFixture[os.Path](
    setup = { _ =>
      val dir = os.temp.dir(prefix = "blarg-cl-test")
      val templates = dir / "templates"
      os.makeDir.all(templates)
      os.write(templates / "site.mustache", "<html>{{>content}}</html>")
      os.write(templates / "blog.mustache", "<blog>{{&content.content}}</blog>")
      os.write(templates / "page.mustache", "<page>{{&content.content}}</page>")
      os.write(templates / "tags.mustache", "<tags/>")
      os.write(templates / "latest.mustache", "<latest/>")
      os.write(dir / "test.md", "# Hello")
      dir
    },
    teardown = { dir =>
      os.remove.all(dir)
    }
  )

  tmpDir.test("load reads file content") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.load(dir / "test.md"), "# Hello")
  }

  tmpDir.test("loadTemplate reads from templates directory") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadTemplate("site.mustache"), "<html>{{>content}}</html>")
  }

  tmpDir.test("loadSiteTemplate returns site.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadSiteTemplate(), "<html>{{>content}}</html>")
  }

  tmpDir.test("loadBlogTemplate returns blog.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadBlogTemplate(), "<blog>{{&content.content}}</blog>")
  }

  tmpDir.test("loadPageTemplate returns page.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadPageTemplate(), "<page>{{&content.content}}</page>")
  }

  tmpDir.test("loadTagTemplate returns tags.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadTagTemplate(), "<tags/>")
  }

  tmpDir.test("loadLatestTemplate returns latest.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadLatestTemplate(), "<latest/>")
  }

}
