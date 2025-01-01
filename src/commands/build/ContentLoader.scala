package commands.build

import java.nio.file.{Files, Path}
import scala.util.Try
import dev.wishingtree.branch.macaroni.fs.PathOps.*

trait ContentLoader {
  def load(path: Path): Try[String]
  def loadTemplate(fileName: String): Try[String]
  
  def loadSiteTemplate(): Try[String] = loadTemplate("site.html")
  def loadBlogTemplate(): Try[String] = loadTemplate("blog.html")
  def loadPageTemplate(): Try[String] = loadTemplate("page.html")
  
}

object ContentLoader {

  def apply(root: Path): ContentLoader = new ContentLoader {
    
    val templates = root / "templates"
    
    override def load(path: Path): Try[String] =
      Try(Files.readString(path))
    
    override def loadTemplate(fileName: String): Try[String] =
      load(templates / fileName)
  }
  
}
