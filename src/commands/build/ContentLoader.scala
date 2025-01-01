package commands.build

import dev.wishingtree.branch.macaroni.fs.PathOps.*

import java.nio.file.{Files, Path}

trait ContentLoader {
  def load(path: Path): String
  def loadTemplate(fileName: String): String

  def loadSiteTemplate(): String = loadTemplate("site.html")
  def loadBlogTemplate(): String = loadTemplate("blog.html")
  def loadPageTemplate(): String = loadTemplate("page.html")

}

object ContentLoader {

  def apply(root: Path): ContentLoader = new ContentLoader {

    val templates: Path = root / "templates"

    override def load(path: Path): String =
      Files.readString(path)

    override def loadTemplate(fileName: String): String =
      load(templates / fileName)
  }

}
