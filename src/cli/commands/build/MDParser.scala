package dev.alteration.blarg.cli.commands.build
import org.commonmark.Extension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.footnotes.FootnotesExtension
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension
import org.commonmark.ext.image.attributes.ImageAttributesExtension
import org.commonmark.ext.ins.InsExtension
import org.commonmark.parser.Parser

import scala.jdk.CollectionConverters.*

object MDParser {

  private lazy val extensionList: List[Extension] = List(
    AutolinkExtension.create(),
    StrikethroughExtension.create(),
    TablesExtension.create(),
    FootnotesExtension.create(),
    HeadingAnchorExtension.create(),
    InsExtension.create(),
    YamlFrontMatterExtension.create(),
    ImageAttributesExtension.create()
  )

  def apply(): Parser = {
    Parser
      .builder()
      .extensions(
        extensionList.asJava
      )
      .build()
  }

}
