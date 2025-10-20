package blarg.core.rendering

import dev.alteration.branch.mustachio.{Mustachio, Stache}

import java.nio.file.{Files, Path, Paths}
import scala.jdk.StreamConverters.*

/**
 * Core rendering utilities for Blarg.
 *
 * Provides automatic template loading, partial discovery, and layout wrapping.
 */
object Blarg {
  private var templateRoot: Path = Paths.get("site/templates")
  private var partialCache: Option[Stache] = None

  /**
   * Set the template root directory.
   * Default: "site/templates"
   */
  def setTemplateRoot(path: Path): Unit = {
    templateRoot = path
    partialCache = None  // Clear cache when root changes
  }

  /**
   * Render a template with automatic partial loading and optional layout wrapping.
   *
   * @param template Template file path (relative to template root, e.g., "blog.mustache")
   * @param context Stache context for the template
   * @param layout Optional layout template to wrap the content
   * @return Rendered HTML string
   */
  def renderTemplate(
    template: String,
    context: Stache,
    layout: Option[String] = None
  ): String = {
    // Load partials if not already cached
    if (partialCache.isEmpty) {
      partialCache = Some(loadPartials())
    }

    // Load and render the main template
    val templatePath = templateRoot.resolve(template)
    val templateContent = readFile(templatePath)
    val content = Mustachio.render(templateContent, context, partialCache)

    // Wrap in layout if specified
    layout match {
      case Some(layoutTemplate) =>
        val layoutPath = templateRoot.resolve(layoutTemplate)
        val layoutContent = readFile(layoutPath)
        val layoutContext = Stache.obj("content" -> Stache.str(content))
        Mustachio.render(layoutContent, layoutContext, partialCache)

      case None => content
    }
  }

  /**
   * Clear the partial cache.
   * Useful for development mode when templates change.
   */
  def clearCache(): Unit = {
    partialCache = None
  }

  private def loadPartials(): Stache = {
    val partialDir = templateRoot.resolve("partials")

    if (Files.exists(partialDir) && Files.isDirectory(partialDir)) {
      val partials = Files.walk(partialDir)
        .toScala(LazyList)
        .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".mustache"))
        .map { p =>
          val name = partialDir.relativize(p).toString.stripSuffix(".mustache")
          name -> Stache.str(readFile(p))
        }
        .toMap

      Stache.obj(partials.toSeq*)
    } else {
      Stache.obj()
    }
  }

  private def readFile(path: Path): String = {
    if (!Files.exists(path)) {
      throw new IllegalArgumentException(s"Template not found: $path")
    }
    Files.readString(path)
  }
}
