package io.bluearchive.plana

import io.bluearchive.plana.command.ReloadMainConfigCommand
import io.bluearchive.plana.config.MainConfig
import io.bluearchive.plana.utils.CDN
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule
import kotlin.io.path.absolutePathString

object Plana : KotlinPlugin(
  JvmPluginDescription.loadFromResource()
) {

  private var plana: Bot? = null
  private val planaTaskList = mutableListOf<suspend (plana: Bot) -> Unit>()
  private val uploadFileRegex = Regex("\\[[已未待]校对](\\d{5,7}.json)")
  private val uploadFileList = mutableListOf<Pair<String, String>>()
  private val timer = Timer("story", true)
  private var timerTask: TimerTask? = null
  private val lock = Mutex()
  private lateinit var server: ApplicationEngine

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
            it(plana!!)
          }
        }
      }
//    globalEventChannel()
//      .filter { it is GroupMessagePostSendEvent && it.target.id == MainConfig.group }
//      .subscribeAlways<GroupMessagePostSendEvent> {
//        // 消息发送失败
//        if (it.exception != null || it.receipt == null) {
//          return@subscribeAlways
//        }
//        val imageList = it.message.filterIsInstance<Image>().map { im -> MessageSegment("image", im.queryUrl()) }
//        val map = it.message.map { message ->
//          when(message) {
//            is At -> PlainText("@${it.target[message.target]!!.remarkOrNameCardOrNick}")
//            else -> message
//          }
//        }
//        val planText = MessageSegment("text", URLEncoder.encode(map.toMessageChain().contentToString()))
//        postMessage(imageList + planText)
//      }
    globalEventChannel()
      .filter { it is GroupMessageEvent && it.group.id == MainConfig.group }
      .subscribeAlways<GroupMessageEvent> {
        val fileList = it.message.filterIsInstance<FileMessage>().filter { file -> file.name.contains(uploadFileRegex) }
        if (fileList.isEmpty()) {
          return@subscribeAlways
        }
        val newList = fileList.map { file ->
          val url = file.toAbsoluteFile(it.group)!!.getUrl()!!
          file.name to url
        }
        timerTask?.cancel()
        lock.withLock {
          uploadFileList.addAll(
            newList.filter { file ->
              !uploadFileList.any { exist -> exist.first == file.first }
            }
          )
        }
        timerTask = timer.schedule(MainConfig.uploadDelay) {
          runSuspend {
            lock.withLock {
              doUpload()
            }
          }
        }
      }
    startHttpServer()
    info("plana load success")
  }

  private fun startHttpServer() {
    runSuspend {
      server = embeddedServer(Netty, port = MainConfig.httpPort, module = Application::module).start(true)
    }
  }

  private fun doUpload() {
    val mapName = uploadFileList.map {
      val name = it.first
      val match = uploadFileRegex.matchEntire(name)!!
      val jsonName = match.groupValues[1]
      jsonName to it.second
    }
    mapName.forEach {
      URL(it.second).openStream().use { stream ->
        Files.copy(stream, Paths.get(MainConfig.storyPath, it.first), StandardCopyOption.REPLACE_EXISTING)
      }
    }
    sendToGroup("发送了${mapName.size}个文件:\n${mapName.joinToString("\n") { it.first }}")
    CDN.purge(mapName.map { it.first })
    // 通知nonebot
    postMessage(uploadFileList.map { file -> MessageSegment(
      "file",
      file.second,
      Base64.getEncoder().encodeToString(file.first.toByteArray())
    ) })
    uploadFileList.clear()
  }

  private fun postMessage(data: List<MessageSegment>) {
    val url = URL(MainConfig.api)
    val conn = url.openConnection() as HttpURLConnection
    val postData = Json.encodeToString(MessageSegmentList(data))
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.useCaches = false
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("User-Agent", "PostmanRuntime/7.32.2")
    conn.setRequestProperty("Accept", "*/*")
    conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br")
    conn.setRequestProperty("Connection", "keep-alive")
    conn.setRequestProperty("Content-Length", postData.toByteArray().size.toString())

    DataOutputStream(conn.outputStream).use { it.writeBytes(postData) }
    BufferedReader(InputStreamReader(conn.inputStream)).use {}

  }

  fun runSuspend(block: suspend () -> Unit) = launch(coroutineContext) {
    block()
  }

  fun runAsync(block: suspend (event: EventChannel<Event>) -> Unit) = async(coroutineContext) {
    block(globalEventChannel())
  }

  fun sendToGroup(message: String) {
    runWithGroup { group ->
      group.sendMessage(MessageChainBuilder().also { it.add(message) }.build())
    }
  }

  fun runWithGroup(block: suspend (group: Group) -> Unit) {
    runWithPlana { plana ->
      val group = plana.getGroup(MainConfig.group) ?: return@runWithPlana
      block(group)
    }
  }

  fun runWithPlana(block: suspend (plana: Bot) -> Unit) {
    if (plana == null) {
      planaTaskList.add(block)
    } else {
      runSuspend {
        block(plana!!)
      }
    }
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
data class MessageSegment(
  val type: String,
  val content: String,
  val extra: String = ""
)

@Serializable
data class MessageSegmentList(
  val data: List<MessageSegment>
)

fun Application.configureSerialization() {
  install(ContentNegotiation) {
    json()
  }
}

fun Application.configureRouting() {
  routing {
    post("/plana") {
      val data = call.receive<MessageSegmentList>()
      Plana.runWithGroup { group ->
        val segment = data.data
          .filter { it.type == "file" }
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", MainConfig.proxyPort))
        segment
          .forEach {
            val fileName = it.extra
            val localPath = Plana.dataFolderPath.resolve(fileName)
            val conn = URL(it.content).openConnection(proxy)
            conn.doInput = true
            conn.inputStream.use { stream ->
              Files.copy(stream, localPath, StandardCopyOption.REPLACE_EXISTING)
              val file = File(localPath.absolutePathString())
              file.toExternalResource().use { fileObj ->
                group.files.root.resolveFolder("tg")?.uploadNewFile(fileName, fileObj)
              }
              file.delete()
            }
          }
        Plana.sendToGroup("tg上传了文件(${segment.size}个):\n${segment.joinToString("\n") { it.extra }}")
      }
      call.respondText("Hello World!")
    }
  }
}

fun Application.module() {
  configureSerialization()
  configureRouting()
}
