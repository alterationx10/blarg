package commands.build

import os.*

trait ContentLoader {
  def load(path: Path): String
  def loadTemplate(fileName: String): String

  def loadSiteTemplate(): String   = loadTemplate("site.mustache")
  def loadBlogTemplate(): String   = loadTemplate("pages/blog.mustache")
  def loadPageTemplate(): String   = loadTemplate("pages/page.mustache")
  def loadTagTemplate(): String    = loadTemplate("pages/tags.mustache")
  def loadLatestTemplate(): String = loadTemplate("pages/latest.mustache")

  def loadHeaderPartial(): String = loadTemplate("partials/header.mustache")
  def loadNavPartial(): String    = loadTemplate("partials/nav.mustache")
  def loadFooterPartial(): String = loadTemplate("partials/footer.mustache")

}

object ContentLoader {

  def apply(root: Path): ContentLoader = new ContentLoader {

    val templates: Path = root / "templates"

    override def load(path: Path): String = {
      if !os.exists(path) then
        throw new RuntimeException(s"File not found: $path")
      os.read(path)
    }

    override def loadTemplate(fileName: String): String = {
      val templatePath = templates / os.RelPath(fileName)
      if !os.exists(templatePath) then {
        System.err.println(s"ERROR: Template not found: $templatePath")
        System.err.println(
          s"Please ensure the template file exists in your site/templates directory."
        )
        System.exit(1)
        throw new RuntimeException("unreachable")
      }
      load(templatePath)
    }
  }

}
