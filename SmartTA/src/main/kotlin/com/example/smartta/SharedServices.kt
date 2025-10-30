package com.example.smartta

import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 共享服务对象
 * 单例模式，提供全局共享的 HTTP 客户端和 JSON 解析器
 * 避免重复创建对象，提高性能
 */
object SharedServices {
    
    /**
     * JSON 媒体类型
     */
    val JSON_MEDIA_TYPE: MediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * PDF 媒体类型
     */
    val PDF_MEDIA_TYPE: MediaType = "application/pdf".toMediaType()
    
    /**
     * 共享的 Gson 实例
     * 用于 JSON 序列化和反序列化
     */
    val gson: Gson = Gson()
    
    /**
     * 共享的 OkHttpClient 实例
     * 配置了合理的超时时间
     */
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}

