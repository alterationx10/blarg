package config

import upickle.default.ReadWriter

case class NavItem(
    label: String,
    href: String
) derives ReadWriter

case class SiteConfig(
    siteTitle: String,
    author: String,
    navigation: List[NavItem],
    dynamic: ujson.Value
) derives ReadWriter
