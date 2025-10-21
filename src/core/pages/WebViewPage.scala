package dev.alteration.blarg.core.pages

import dev.alteration.branch.mustachio.Stache
import dev.alteration.branch.spider.webview.WebView
import dev.alteration.blarg.core.rendering.Blarg

/**
 * A reactive WebView page with real-time interactivity.
 *
 * WebView pages maintain server-side state and push updates to the client via WebSocket.
 * They provide app-like experiences with minimal JavaScript.
 *
 * Use for:
 * - Real-time dashboards
 * - Interactive forms
 * - Live chat
 * - Rich application UIs
 *
 * Example:
 * {{{
 * object CounterPage extends WebViewPage[Int, CounterEvent] {
 *   def route = "/counter"
 *   def template = "counter.mustache"
 *
 *   def mount(params, session) = 0
 *
 *   def handleEvent(event, state) = event match {
 *     case Increment => state + 1
 *     case Decrement => state - 1
 *   }
 *
 *   def renderState(count) = Stache.obj(
 *     "count" -> Stache.num(count)
 *   )
 * }
 *
 * enum CounterEvent derives EventCodec {
 *   case Increment, Decrement
 * }
 * }}}
 */
trait WebViewPage[State, Event] extends BlargPage with WebView[State, Event] {
  /**
   * Render the state to a Stache context.
   * Users implement this to return template data.
   */
  def renderState(state: State): Stache

  /**
   * Final render method that bridges to WebView.
   * Automatically loads template and applies layout.
   * Users should NOT override this - implement renderState instead.
   */
  final def render(state: State): String = {
    val context = renderState(state)
    Blarg.renderTemplate(template, context, layout)
  }
}
