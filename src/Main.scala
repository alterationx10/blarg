import commands.build.Build
import commands.serve.Serve
import dev.wishingtree.branch.ursula.UrsulaApp
import dev.wishingtree.branch.ursula.command.Command

object Main extends UrsulaApp {

  override val commands: Seq[Command] = Seq(
    Build,
    Serve
  )

}
