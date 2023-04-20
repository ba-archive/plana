package io.bluearchive.plana

import io.kotest.assertions.print.print
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.test.Test

class PlanaKtTest {

  @Test
  fun testPostPlana() = testApplication {
    application {
      module()
    }
    client.post("/plana") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(MessageSegmentList(
        listOf(MessageSegment("file", "https://", "1.txt"))
      )))
    }
  }

  @Test
  fun testPathResolve() {
    print(Path("d:/").resolve("image.jpg").absolutePathString())
  }
}