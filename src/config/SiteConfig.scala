package config

import dev.alteration.branch.friday.{Json, JsonCodec}

case class NavItem(
    label: String,
    href: String
) derives JsonCodec

case class SiteConfig(
    siteTitle: String,
    author: String,
    navigation: List[NavItem],
    dynamic: Json
) derives JsonCodec
