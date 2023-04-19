package io.bluearchive.plana.utils

import com.tencentcloudapi.cdn.v20180606.CdnClient
import com.tencentcloudapi.cdn.v20180606.models.PurgeUrlsCacheRequest
import com.tencentcloudapi.cdn.v20180606.models.PurgeUrlsCacheResponse
import com.tencentcloudapi.common.Credential
import io.bluearchive.plana.config.MainConfig

object CDN {

  private val cdnClient: CdnClient by lazy {
    val cred = Credential(MainConfig.secretId, MainConfig.secretKey)
    CdnClient(cred, "ap-shanghai")
  }

  fun purge(files: List<String>): PurgeUrlsCacheResponse {
    val req = PurgeUrlsCacheRequest()
    req.urls = files.map { "https://yuuka.cdn.diyigemt.com/image/story/vol3/$it" }.toTypedArray()
    return cdnClient.PurgeUrlsCache(req)
  }

}