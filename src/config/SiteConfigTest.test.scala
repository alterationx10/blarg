package config

import upickle.default.{read, write}

class SiteConfigTest extends munit.FunSuite {

  test("SiteConfig serializes to JSON with upickle") {
    val config = SiteConfig("My Site")
    val json = write(config)
    assert(json.contains("My Site"), s"JSON should contain site title, got: $json")
  }

  test("SiteConfig round-trips through JSON") {
    val config = SiteConfig("Test Blog")
    val json = write(config)
    val parsed = read[SiteConfig](json)
    assertEquals(parsed, config)
  }

  test("SiteConfig.default has expected title") {
    val config = SiteConfig.default
    assertEquals(config.siteTitle, "My Blarg! Site")
  }

  test("SiteConfig deserializes from JSON string") {
    val json = """{"siteTitle":"From JSON"}"""
    val config = read[SiteConfig](json)
    assertEquals(config.siteTitle, "From JSON")
  }

}
