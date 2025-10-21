package dev.alteration.blarg.cli

import dev.alteration.blarg.cli.commands.build.Build
import dev.alteration.blarg.cli.commands.gen.Gen
import dev.alteration.blarg.cli.commands.newsite.cli.commands.newsite.NewSite
import dev.alteration.blarg.cli.commands.serve.Serve
import dev.alteration.branch.ursula.UrsulaApp
import dev.alteration.branch.ursula.command.Command

object Main extends UrsulaApp {

  override val commands: Seq[Command] = Seq(
    Build,
    Serve,
    Gen,
    NewSite
  )

}
