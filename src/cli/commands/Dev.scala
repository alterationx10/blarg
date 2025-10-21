package blarg.cli.commands

import blarg.core.server.BlargServer
import dev.alteration.blarg.core.BlargSite
import dev.alteration.branch.ursula.args.{Argument, Flag, Flags}
import dev.alteration.branch.ursula.command.{Command, CommandContext}

import java.nio.file.{Files, Path, Paths}

/**
 * Development server with file watching and hot reload.
 *
 * Features:
 * - Auto-rebuild on template/content changes
 * - Cache clearing for fresh renders
 * - WebView DevTools enabled
 *
 * Usage:
 *   blarg dev               # Start dev server on port 9000
 *   blarg dev -p 3000       # Start on custom port
 */
object Dev extends Command {
  // Modern flag definition
  val PortFlag = Flags.int(
    name = "port",
    shortKey = "p",
    description = "Server port",
    default = Some(9000)
  )

  override val trigger: String = "dev"
  override val description: String = "Start dev server with hot reload"
  override val usage: String = "dev [-p PORT]"
  override val examples: Seq[String] = Seq("dev", "dev -p 3000")
  override val flags: Seq[Flag[?]] = Seq(PortFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {
    // Type-safe flag access
    val port = ctx.requiredFlag(PortFlag)

    println("🛠️  Starting development server...")

    // Detect mode
    detectMode() match {
      case Some(site) =>
        // Framework Mode: Load BlargSite and start with dev mode
        println(s"🎯 Framework Mode")

        // Create a wrapper with dev config
        val devSite = new BlargSite {
          override def contentDir = site.contentDir
          override def templateDir = site.templateDir
          override def staticDir = site.staticDir
          override def staticPages = site.staticPages
          override def serverPages = site.serverPages
          override def registerWebViewRoutes(server: dev.alteration.branch.spider.webview.WebViewServer) = site.registerWebViewRoutes(server)
          override def hasWebViewRoutes = site.hasWebViewRoutes
          override def config = site.config.copy(port = port, devMode = true)
        }

        // Start file watcher in background
        startFileWatcher(devSite)

        // Start server
        BlargServer.start(devSite)

      case None =>
        // Simple Mode: Serve static files
        println(s"📄 Simple Mode")
        println("⚠️  File watching not yet implemented for Simple Mode")
        BlargServer.serveStatic(port)
    }
  }

  /**
   * Start a file watcher for auto-rebuild.
   *
   * Watches:
   * - Templates (*.mustache)
   * - Content (*.md)
   * - Scala files (*.scala) - for Framework Mode
   */
  private def startFileWatcher(site: BlargSite): Unit = {
    // TODO: Implement file watching
    // For now, just a placeholder
    println("📂 File watching: Not yet implemented")
    println("   Manual rebuild: Re-run `blarg dev` after changes")
  }

  /**
   * Detect which mode to use.
   *
   * TODO: Compile and load user's BlargSite
   */
  private def detectMode(): Option[BlargSite] = {
    val siteFile = Paths.get("site/BlargSite.scala")
    val projectFile = Paths.get("project.scala")

    if (Files.exists(siteFile) || Files.exists(projectFile)) {
      System.err.println("⚠️  Framework Mode detected but not yet implemented")
      System.err.println("   For now, using Simple Mode")
      None
    } else {
      None
    }
  }
}
