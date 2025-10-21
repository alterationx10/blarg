package dev.alteration.blarg.core.server

import dev.alteration.blarg.core.BlargSite
import dev.alteration.blarg.core.pages.StaticPage
import dev.alteration.blarg.core.rendering.Blarg

import java.nio.file.{Files, Path, StandardCopyOption}
import java.io.IOException
import scala.jdk.StreamConverters.*

/**
 * Builder for static site generation.
 *
 * Renders StaticPage instances to HTML files and copies static assets.
 */
object SiteBuilder {

  /**
   * Build a static site from a BlargSite definition.
   *
   * Steps:
   * 1. Clean the build directory
   * 2. Render all StaticPages to HTML
   * 3. Copy static assets
   *
   * @param site The site definition
   * @param buildDir The output directory (default: ./build)
   */
  def build(site: BlargSite, buildDir: Path): Unit = {
    println(s"🏗️  Building static site...")

    // Step 1: Clean build directory
    cleanBuild(buildDir)

    // Step 2: Set template root for rendering
    Blarg.setTemplateRoot(site.templateDir)

    // Step 3: Render static pages
    site.staticPages.foreach { page =>
      buildPage(page, buildDir)
    }

    // Step 4: Copy static assets
    if (Files.exists(site.staticDir)) {
      copyStatic(site.staticDir, buildDir.resolve("static"))
    }

    println(s"✅ Built ${site.staticPages.length} pages to $buildDir")
  }

  /**
   * Build a single static page to an HTML file.
   */
  private def buildPage(page: StaticPage, buildDir: Path): Unit = {
    try {
      // Render the page
      val context = page.render()
      val html = Blarg.renderTemplate(page.template, context, page.layout)

      // Determine output path
      val outputPath = routeToPath(page.route, buildDir)

      // Create parent directories
      Files.createDirectories(outputPath.getParent)

      // Write HTML file
      Files.writeString(outputPath, html)

      println(s"   ✓ ${page.route} → ${buildDir.relativize(outputPath)}")

    } catch {
      case e: Exception =>
        System.err.println(s"   ✗ Failed to build ${page.route}: ${e.getMessage}")
        throw e
    }
  }

  /**
   * Convert a route to a file path.
   *
   * Examples:
   * - "/" → build/index.html
   * - "/about" → build/about.html
   * - "/blog/my-post" → build/blog/my-post.html
   */
  private def routeToPath(route: String, buildDir: Path): Path = {
    val normalized = if (route == "/") {
      "index"
    } else {
      route.stripPrefix("/")
    }

    val pathWithHtml = if (normalized.endsWith(".html")) {
      normalized
    } else {
      normalized + ".html"
    }

    buildDir.resolve(pathWithHtml)
  }

  /**
   * Clean the build directory.
   */
  private def cleanBuild(buildDir: Path): Unit = {
    if (Files.exists(buildDir)) {
      Files.walk(buildDir)
        .toScala(LazyList)
        .sorted(Ordering[Path].reverse)  // Delete files before directories
        .foreach(Files.deleteIfExists)
    }

    Files.createDirectories(buildDir)
  }

  /**
   * Copy static assets recursively.
   */
  private def copyStatic(sourceDir: Path, targetDir: Path): Unit = {
    if (!Files.exists(sourceDir)) {
      return
    }

    Files.walk(sourceDir)
      .toScala(LazyList)
      .foreach { source =>
        val target = targetDir.resolve(sourceDir.relativize(source))

        if (Files.isDirectory(source)) {
          Files.createDirectories(target)
        } else {
          Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
      }

    println(s"   📁 Copied static assets from ${sourceDir.getFileName}")
  }
}
