package io.bluearchive.plana

import io.bluearchive.plana.command.ReloadMainConfigCommand
import io.bluearchive.plana.config.MainConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.GroupMessagePostSendEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.info
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object Plana : KotlinPlugin(
  JvmPluginDescription.loadFromResource()
) {

  private lateinit var plana: Bot
  private val planaTaskList = mutableListOf<suspend (plana: Bot) -> Unit>()

  override fun onEnable() {
    System.setProperty("java.awt.headless", "true")
    MainConfig.reload()
    ReloadMainConfigCommand.register()
    globalEventChannel()
      .filter { it is BotOnlineEvent && it.bot.id == MainConfig.qq }
      .subscribeOnce<BotOnlineEvent> {
        plana = it.bot
        planaTaskList.forEach {
          runSuspend {
            it(plana)
          }
        }
      }
    globalEventChannel()
      .filter { it is GroupMessagePostSendEvent && it.target.id == MainConfig.group }
      .subscribeAlways<GroupMessagePostSendEvent> {
        // 消息发送失败
        if (it.exception != null || it.receipt == null) {
          return@subscribeAlways
        }
        val imageList = it.message.filterIsInstance<Image>().map { im -> MessageSegment("image", im.queryUrl()) }
        val planText = MessageSegment("text", it.message.contentToString())
        postMessage(imageList + planText)
      }
    info("plana load success")
  }

  fun postMessage(data: List<MessageSegment>) {
    val url = URL(MainConfig.api)
    val postData = Json.encodeToString(data)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("Content-Length", postData.length.toString())
    conn.useCaches = false

    DataOutputStream(conn.outputStream).use { it.writeBytes(postData) }
    BufferedReader(InputStreamReader(conn.inputStream)).use {}
  }

  fun runSuspend(block: suspend () -> Unit) = launch(coroutineContext) {
    block()
  }

  fun runAsync(block: suspend (event: EventChannel<Event>) -> Unit) = async(coroutineContext) {
    block(globalEventChannel())
  }

  fun info(message: String?) = logger.info(message)

  fun warning(message: String?) = logger.warning(message)

  fun error(message: String?) = logger.error(message)

  fun verbose(message: String?) = logger.verbose(message)

  override fun onDisable() {
    ReloadMainConfigCommand.unregister()
  }
}

@Serializable
class MessageSegment(
  val type: String,
  val content: String
)
