package io.bluearchive.plana.command

import io.bluearchive.plana.Plana
import io.bluearchive.plana.Plana.reload
import io.bluearchive.plana.config.MainConfig
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.command.SimpleCommand

object ReloadMainConfigCommand: SimpleCommand(
  Plana, "reload-plana"
) {
  @Handler
  suspend fun ConsoleCommandSender.reloadPlana() {
    MainConfig.reload()
  }
}