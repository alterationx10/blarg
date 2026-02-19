import commands.build.Build
import commands.gen.Gen
import commands.newsite.NewSite
import commands.serve.Serve
import ursula.UrsulaApp
import ursula.command.Command

object Main extends UrsulaApp {

  override val commands: Seq[Command] = Seq(
    Build,
    Serve,
    Gen,
    NewSite
  )

}
