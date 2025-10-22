package config

import dev.alteration.branch.friday.{JsonCodec, Json}

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
