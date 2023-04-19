package io.bluearchive.plana.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object MainConfig: AutoSavePluginConfig("main") {

  val qq: Long by value()

  val group: Long by value()

  val api: String by value()

}