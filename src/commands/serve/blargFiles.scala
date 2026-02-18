package commands.serve

import cask.*
import cask.endpoints.QueryParamReader
import cask.model.Response.Raw
import cask.router.Result

import java.nio.file

class blargFiles(val path: String, headers: Seq[(String, String)] = Nil)
    extends HttpEndpoint[String, Seq[String]] {
  val methods = Seq("get")
  type InputParser[T] = QueryParamReader[T]

  override def subpath = true

  private def buildPathsAndContentType(
      srvPath: String,
      ctx: Request
  ): List[(String, Option[String])] = {
    val leadingSlash = if (srvPath.startsWith("/")) "/" else ""
    val path         = leadingSlash + (cask.internal.Util.splitPath(
      srvPath
    ) ++ ctx.remainingPathSegments.flatMap(cask.internal.Util.splitPath))
      .filter(s => s != "." && s != "..")
      .mkString("/")
    List(
      path,
      path.stripSuffix("/") + ".html",
      path + "/index.html"
    ).map { p =>
      p -> Option(
        java.nio.file.Files.probeContentType(java.nio.file.Paths.get(p))
      )
    }
  }

  private def checkVariant(
      srvPath: String,
      ctx: Request
  ): Option[(String, Option[String])] = {

    buildPathsAndContentType(srvPath, ctx)
      .find { (p, _) =>
        val path = file.Paths.get(p)
        java.nio.file.Files.exists(path) && java.nio.file.Files
          .isRegularFile(path)
      }
  }

  def wrapFunction(ctx: Request, delegate: Delegate): Result[Raw] = {
    delegate(ctx, Map()).map { t =>
      checkVariant(t, ctx) match {
        case Some((path, contentTypeOpt)) =>
          cask.model.StaticFile(
            path,
            headers ++ contentTypeOpt.map("Content-Type" -> _)
          )
        case None                         => Response("", 404, headers)
      }
    }
  }

  def wrapPathSegment(s: String): Seq[String] = Seq(s)
}
