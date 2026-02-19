package commands.build

class ContentLoaderSuite extends munit.FunSuite {

  val tmpDir = FunFixture[os.Path](
    setup = { _ =>
      val dir = os.temp.dir(prefix = "blarg-cl-test")
      val templates = dir / "templates"
      os.makeDir.all(templates / "pages")
      os.makeDir.all(templates / "partials")
      os.write(templates / "site.mustache", "<html>{{>content}}</html>")
      os.write(templates / "pages" / "blog.mustache", "<blog>{{&content.content}}</blog>")
      os.write(templates / "pages" / "page.mustache", "<page>{{&content.content}}</page>")
      os.write(templates / "pages" / "tags.mustache", "<tags/>")
      os.write(templates / "pages" / "latest.mustache", "<latest/>")
      os.write(templates / "partials" / "header.mustache", "<header/>")
      os.write(templates / "partials" / "nav.mustache", "<nav/>")
      os.write(templates / "partials" / "footer.mustache", "<footer/>")
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

  tmpDir.test("loadBlogTemplate returns pages/blog.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadBlogTemplate(), "<blog>{{&content.content}}</blog>")
  }

  tmpDir.test("loadPageTemplate returns pages/page.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadPageTemplate(), "<page>{{&content.content}}</page>")
  }

  tmpDir.test("loadTagTemplate returns pages/tags.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadTagTemplate(), "<tags/>")
  }

  tmpDir.test("loadLatestTemplate returns pages/latest.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadLatestTemplate(), "<latest/>")
  }

  tmpDir.test("loadHeaderPartial returns partials/header.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadHeaderPartial(), "<header/>")
  }

  tmpDir.test("loadNavPartial returns partials/nav.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadNavPartial(), "<nav/>")
  }

  tmpDir.test("loadFooterPartial returns partials/footer.mustache") { dir =>
    val loader = ContentLoader(dir)
    assertEquals(loader.loadFooterPartial(), "<footer/>")
  }

}
