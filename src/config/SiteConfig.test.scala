package config

import munit.FunSuite
import dev.alteration.branch.friday.Json

class SiteConfigSuite extends FunSuite {

  test("SiteConfig should decode from JSON") {
    val jsonStr = """
    {
      "siteTitle": "Test Site",
      "author": "Test Author",
      "navigation": [
        {"label": "Home", "href": "/"},
        {"label": "Blog", "href": "/blog.html"}
      ],
      "dynamic": {}
    }
    """

    val result = Json.decode[SiteConfig](jsonStr)

    assert(result.isSuccess)
    val config = result.get

    assertEquals(config.siteTitle, "Test Site")
    assertEquals(config.author, "Test Author")
    assertEquals(config.navigation.size, 2)
    assertEquals(config.navigation(0).label, "Home")
    assertEquals(config.navigation(0).href, "/")
    assertEquals(config.navigation(1).label, "Blog")
    assertEquals(config.navigation(1).href, "/blog.html")
  }

  test("SiteConfig should decode with custom dynamic field") {
    val jsonStr = """
    {
      "siteTitle": "Custom Dynamic Test",
      "author": "Tester",
      "navigation": [
        {"label": "Link1", "href": "/link1"},
        {"label": "Link2", "href": "/link2"}
      ],
      "dynamic": {"custom": "value", "another": 123}
    }
    """

    val decoded = Json.decode[SiteConfig](jsonStr)
    assert(decoded.isSuccess)

    val config = decoded.get
    assertEquals(config.siteTitle, "Custom Dynamic Test")
    assertEquals(config.author, "Tester")
    assertEquals(config.navigation.size, 2)
    assert(config.dynamic != null)
  }

  test("SiteConfig should handle empty navigation") {
    val jsonStr = """
    {
      "siteTitle": "No Nav",
      "author": "Author",
      "navigation": [],
      "dynamic": {}
    }
    """

    val decoded = Json.decode[SiteConfig](jsonStr)

    assert(decoded.isSuccess)
    assertEquals(decoded.get.navigation.size, 0)
  }

  test("SiteConfig should handle dynamic JSON object") {
    val jsonStr = """
    {
      "siteTitle": "Site",
      "author": "Author",
      "navigation": [],
      "dynamic": {
        "socialLinks": {
          "github": "https://github.com/user",
          "twitter": "https://twitter.com/user"
        },
        "analytics": {
          "enabled": true,
          "trackingId": "UA-12345"
        }
      }
    }
    """

    val result = Json.decode[SiteConfig](jsonStr)

    assert(result.isSuccess)
    val config = result.get

    // Dynamic field should decode successfully
    assert(config.dynamic != null)
  }

  test("SiteConfig should fail gracefully on invalid JSON") {
    val invalidJson = """
    {
      "siteTitle": "Missing Fields"
    }
    """

    val result = Json.decode[SiteConfig](invalidJson)

    assert(result.isFailure)
  }

  test("SiteConfig should fail on malformed JSON") {
    val malformedJson = """
    {
      "siteTitle": "Broken",
      "author": "Test"
      missing comma
    }
    """

    val result = Json.decode[SiteConfig](malformedJson)

    assert(result.isFailure)
  }

  test("NavItem should decode from JSON") {
    val jsonStr = """{"label": "About", "href": "/about.html"}"""

    val result = Json.decode[NavItem](jsonStr)

    assert(result.isSuccess)
    assertEquals(result.get.label, "About")
    assertEquals(result.get.href, "/about.html")
  }

  test("SiteConfig should handle special characters in strings") {
    val jsonStr = """
    {
      "siteTitle": "Site with quotes and apostrophes",
      "author": "O'Brien",
      "navigation": [
        {"label": "Q&A", "href": "/qa"}
      ],
      "dynamic": {}
    }
    """

    val decoded = Json.decode[SiteConfig](jsonStr)

    assert(decoded.isSuccess)
    assertEquals(decoded.get.siteTitle, "Site with quotes and apostrophes")
    assertEquals(decoded.get.author, "O'Brien")
  }

  test("SiteConfig should handle empty strings") {
    val jsonStr = """
    {
      "siteTitle": "",
      "author": "",
      "navigation": [],
      "dynamic": {}
    }
    """

    val result = Json.decode[SiteConfig](jsonStr)

    assert(result.isSuccess)
    assertEquals(result.get.siteTitle, "")
    assertEquals(result.get.author, "")
  }

  test("SiteConfig should handle long navigation lists") {
    val navItems = (1 to 20)
      .map(i => s"""{"label": "Link $i", "href": "/link$i"}""")
      .mkString("[", ",", "]")
    val jsonStr  = s"""
    {
      "siteTitle": "Many Links",
      "author": "Author",
      "navigation": $navItems,
      "dynamic": {}
    }
    """

    val decoded = Json.decode[SiteConfig](jsonStr)

    assert(decoded.isSuccess)
    assertEquals(decoded.get.navigation.size, 20)
    assertEquals(decoded.get.navigation(0).label, "Link 1")
    assertEquals(decoded.get.navigation(19).label, "Link 20")
  }

  test("SiteConfig with real-world example") {
    val jsonStr = """
    {
      "siteTitle": "My Awesome Blog",
      "author": "Jane Developer",
      "navigation": [
        {
          "label": "Home",
          "href": "/"
        },
        {
          "label": "Blog",
          "href": "/latest.html"
        },
        {
          "label": "Tags",
          "href": "/tags.html"
        },
        {
          "label": "About",
          "href": "/about.html"
        }
      ],
      "dynamic": {
        "socialLinks": {
          "github": "https://github.com/janedev",
          "twitter": "https://twitter.com/janedev"
        },
        "analytics": {
          "enabled": true,
          "trackingId": "UA-123456-1"
        },
        "seo": {
          "description": "A blog about software development",
          "keywords": ["programming", "scala", "web development"]
        }
      }
    }
    """

    val result = Json.decode[SiteConfig](jsonStr)

    assert(result.isSuccess)
    val config = result.get

    assertEquals(config.siteTitle, "My Awesome Blog")
    assertEquals(config.author, "Jane Developer")
    assertEquals(config.navigation.size, 4)
    assertEquals(config.navigation(0).label, "Home")
    assertEquals(config.navigation(1).label, "Blog")
  }

  test("NavItem with URL-encoded href") {
    val jsonStr = """{"label": "Search", "href": "/search?q=test&lang=en"}"""

    val decoded = Json.decode[NavItem](jsonStr)

    assert(decoded.isSuccess)
    assertEquals(decoded.get.href, "/search?q=test&lang=en")
  }

  test("SiteConfig equality") {
    val jsonStr1 = """
    {
      "siteTitle": "Title",
      "author": "Author",
      "navigation": [{"label": "Home", "href": "/"}],
      "dynamic": {}
    }
    """

    val jsonStr2 = """
    {
      "siteTitle": "Title",
      "author": "Author",
      "navigation": [{"label": "Home", "href": "/"}],
      "dynamic": {}
    }
    """

    val config1 = Json.decode[SiteConfig](jsonStr1).get
    val config2 = Json.decode[SiteConfig](jsonStr2).get

    assertEquals(config1.siteTitle, config2.siteTitle)
    assertEquals(config1.author, config2.author)
    assertEquals(config1.navigation, config2.navigation)
  }

  test("NavItem equality") {
    val nav1 = NavItem("Test", "/test")
    val nav2 = NavItem("Test", "/test")
    val nav3 = NavItem("Other", "/other")

    assertEquals(nav1, nav2)
    assertNotEquals(nav1, nav3)
  }

  test("SiteConfig should handle nested dynamic objects") {
    val jsonStr = """
    {
      "siteTitle": "Test",
      "author": "Test",
      "navigation": [],
      "dynamic": {
        "level1": {
          "level2": {
            "level3": "deep value"
          }
        }
      }
    }
    """

    val result = Json.decode[SiteConfig](jsonStr)
    assert(result.isSuccess)
  }

  test("SiteConfig should handle dynamic arrays") {
    val jsonStr = """
    {
      "siteTitle": "Test",
      "author": "Test",
      "navigation": [],
      "dynamic": {
        "tags": ["tag1", "tag2", "tag3"],
        "numbers": [1, 2, 3]
      }
    }
    """

    val result = Json.decode[SiteConfig](jsonStr)
    assert(result.isSuccess)
  }

  test("SiteConfig should handle complex nested structures in dynamic") {
    val jsonStr = """
    {
      "siteTitle": "Test",
      "author": "Test",
      "navigation": [],
      "dynamic": {
        "metadata": {
          "version": "1.0",
          "lastUpdated": "2025-01-15",
          "features": [
            "blog",
            "pages",
            "tags"
          ]
        },
        "theme": {
          "colors": {
            "primary": "#007bff",
            "secondary": "#6c757d"
          },
          "fonts": ["Arial", "Helvetica"]
        }
      }
    }
    """

    val result = Json.decode[SiteConfig](jsonStr)
    assert(result.isSuccess)
  }
}
