package commands.build

import org.commonmark.node.{FencedCodeBlock, Node}
import org.commonmark.renderer.html.{
  CoreHtmlNodeRenderer,
  HtmlNodeRendererContext,
  HtmlNodeRendererFactory,
  HtmlRenderer,
  HtmlWriter
}
import org.commonmark.renderer.NodeRenderer

import java.util.{Collections, Set as JSet}

class MermaidNodeRenderer(private val context: HtmlNodeRendererContext)
    extends NodeRenderer {

  private val html: HtmlWriter               = context.getWriter
  private val fallback: CoreHtmlNodeRenderer = CoreHtmlNodeRenderer(context)

  override def getNodeTypes: JSet[Class[? <: Node]] =
    Collections.singleton(classOf[FencedCodeBlock])

  override def render(node: Node): Unit = {
    val codeBlock = node.asInstanceOf[FencedCodeBlock]
    if codeBlock.getInfo != null && codeBlock.getInfo.trim == "mermaid" then {
      html.line()
      html.tag("pre", Collections.singletonMap("class", "mermaid"))
      html.text(codeBlock.getLiteral)
      html.tag("/pre")
      html.line()
    } else {
      fallback.render(node)
    }
  }
}

object MermaidExtension {
  def create(): HtmlRenderer.HtmlRendererExtension =
    (builder: HtmlRenderer.Builder) => {
      builder.nodeRendererFactory((context: HtmlNodeRendererContext) => {
        new MermaidNodeRenderer(context)
      })
    }
}
