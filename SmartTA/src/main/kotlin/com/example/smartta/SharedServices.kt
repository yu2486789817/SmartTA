package com.example.smartta

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 共享服务对象
 * 统一管理可重用的资源，避免重复创建以提升性能
 */
object SharedServices {
    
    /**
     * 共享的 HTTP 客户端实例
     * 使用懒加载模式，配置连接、读取和写入超时时间
     */
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * 共享的 JSON 序列化工具实例
     * Gson 是线程安全的，可以在多线程环境中安全使用
     */
    val gson: Gson = Gson()
    
    /**
     * JSON 媒体类型常量
     */
    val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    
    /**
     * PDF 媒体类型常量
     */
    val PDF_MEDIA_TYPE = "application/pdf".toMediaType()
}

