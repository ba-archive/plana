package io.bluearchive.plana.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object MainConfig: AutoSavePluginConfig("main") {

  val qq: Long by value()

  val group: Long by value()

  val api: String by value("http://127.0.0.1:8080/plana")

  val uploadDelay: Long by value(1000L * 30)

  val storyPath: String by value("/srv/live2d/story/vol3/")

  val secretId: String by value()

  val secretKey: String by value()

  val httpPort: Int by value(13502)

  val proxyPort: Int by value(7890)

}