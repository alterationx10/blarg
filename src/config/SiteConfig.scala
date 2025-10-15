package config

import dev.alteration.branch.friday.JsonCodec

case class SiteConfig(
    siteTitle: String
) derives JsonCodec

object SiteConfig {
  def default: SiteConfig = SiteConfig(
    siteTitle = "My Blarg! Site"
  )
}
