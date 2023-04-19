import io.bluearchive.plana.MessageSegment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TestPost {

  @Test
  fun testPost() {
    val url = URL("http://127.0.0.1:8080/plana")
    val conn = url.openConnection() as HttpURLConnection
    val postData = Json.encodeToString((mapOf("data" to listOf(MessageSegment("text", URLEncoder.encode("@折腾\n\n1d2=1", "utf-8"))))))
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.useCaches = false
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("Content-Length", postData.toByteArray().size.toString())

    DataOutputStream(conn.outputStream).use { it.writeBytes(postData) }
    BufferedReader(InputStreamReader(conn.inputStream)).use {}
  }

  @Test
  fun testRegex() {
    println("[未校对]31051.json".contains(Regex("\\[[已未待]校对](\\d{6,7}.json)")))
  }

}