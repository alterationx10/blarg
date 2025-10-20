package blarg.core.routing

/**
 * Request context for server-rendered pages.
 *
 * Contains URL parameters, query strings, and other request metadata.
 *
 * @param path The full request path (e.g., "/products/123")
 * @param params Path parameters extracted from the route pattern (e.g., Map("id" -> "123"))
 * @param query Query string parameters (e.g., Map("sort" -> "price", "order" -> "asc"))
 */
case class PageRequest(
  path: String,
  params: Map[String, String],
  query: Map[String, String]
)
