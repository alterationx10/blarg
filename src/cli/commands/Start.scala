package blarg.cli.commands

import blarg.core.server.BlargServer
import dev.alteration.blarg.core.BlargSite
import dev.alteration.branch.ursula.command.{Command, CommandContext}
import dev.alteration.branch.ursula.args.{Argument, Flag, Flags}

import java.nio.file.{Files, Paths}
import scala.util.{Failure, Success, Try}

/**
 * Start production server.
 *
 * Serves static pages, SSR pages, and WebView components on a single port.
 *
 * Usage:
 *   blarg start              # Start server on port 9000
 *   blarg start -p 3000      # Start on custom port
 */
object Start extends Command {
  // Modern flag definition
  val PortFlag = Flags.int(
    name = "port",
    shortKey = "p",
    description = "Server port",
    default = Some(9000)
  )

  override val trigger: String = "start"
  override val description: String = "Start production server (static + SSR + WebView)"
  override val usage: String = "start [-p PORT]"
  override val examples: Seq[String] = Seq("start", "start -p 3000")
  override val flags: Seq[Flag[?]] = Seq(PortFlag)
  override val arguments: Seq[Argument[?]] = Seq.empty

  override def actionWithContext(ctx: CommandContext): Unit = {
    // Type-safe flag access
    val port = ctx.requiredFlag(PortFlag)

    // Detect mode
    detectMode() match {
      case Some(site) =>
        // Framework Mode: Load BlargSite and start unified server
        println(s"🎯 Framework Mode detected")

        // Create a wrapper with custom port
        val configuredSite = new BlargSite {
          override def contentDir = site.contentDir
          override def templateDir = site.templateDir
          override def staticDir = site.staticDir
          override def staticPages = site.staticPages
          override def serverPages = site.serverPages
          override def registerWebViewRoutes(server: dev.alteration.branch.spider.webview.WebViewServer) = site.registerWebViewRoutes(server)
          override def hasWebViewRoutes = site.hasWebViewRoutes
          override def config = site.config.copy(port = port)
        }

        BlargServer.start(configuredSite)

      case None =>
        // Simple Mode: Just serve static files
        println(s"📄 Simple Mode: Serving static files")
        BlargServer.serveStatic(port)
    }
  }

  /**
   * Detect which mode to use:
   * - Framework Mode: If BlargSite.scala exists
   * - Simple Mode: Otherwise
   *
   * TODO: In the future, this will compile and load the user's BlargSite.
   * For now, it's a placeholder.
   */
  private def detectMode(): Option[BlargSite] = {
    val siteFile = Paths.get("site/BlargSite.scala")
    val projectFile = Paths.get("project.scala")

    if (Files.exists(siteFile) || Files.exists(projectFile)) {
      // Framework Mode detected, but not implemented yet
      System.err.println("⚠️  Framework Mode detected but not yet implemented")
      System.err.println("   For now, using Simple Mode")
      None
    } else {
      // Simple Mode
      None
    }
  }
}
