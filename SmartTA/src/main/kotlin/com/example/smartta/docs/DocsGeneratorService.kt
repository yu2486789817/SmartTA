package com.example.smartta.docs

import com.example.smartta.ChatWindowManager
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object DocsGeneratorService {
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)  // 建立连接超时
        .readTimeout(60, TimeUnit.SECONDS)     // 等待响应超时
        .writeTimeout(60, TimeUnit.SECONDS)    // 上传大文件超时
        .build()

    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * 异步生成文档并保存到 projectPath/SmartTA_Doc.md
     * - projectPath: 项目根路径，用于保存文件
     * - projectInfo: ProjectScanner.scan 返回的 ProjectInfo 对象
     */
    fun generateDocsAsync(projectPath: String, projectInfo: ProjectInfo) {
        // 将 ProjectInfo 序列化为 JSON（Gson 会正确处理 data class）
        val json = gson.toJson(projectInfo)
        val body = json.toRequestBody(JSON)

        ChatWindowManager.appendMessageDirect("🧠 正在将项目结构发送至后端生成文档...")

        val request = Request.Builder()
            .url("http://localhost:8000/generate_docs")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                ChatWindowManager.appendMessageDirect("❌ 文档生成失败（网络错误）: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        ChatWindowManager.appendMessageDirect("❌ 后端返回错误: HTTP ${it.code}")
                        return
                    }

                    val respText = it.body?.string()
                    if (respText == null) {
                        ChatWindowManager.appendMessageDirect("⚠️ 后端未返回内容")
                        return
                    }

                    // 假设后端返回 {"markdown": "..." }
                    try {
                        val map = gson.fromJson(respText, Map::class.java)
                        val markdown = map["markdown"] as? String
                        if (markdown.isNullOrEmpty()) {
                            ChatWindowManager.appendMessageDirect("⚠️ 后端返回的 markdown 为空")
                            return
                        }

                        // 保存到文件（覆盖同名文件）
                        // 保存到文件（覆盖同名文件）
                        try {
                            val projectFile = File(projectPath)
                            val projectName = projectFile.name.ifEmpty { "SmartTA_Project" }
                            val outFile = File(projectPath, "${projectName}_SmartTA_Doc.md")
                            outFile.writeText(markdown)

                            ChatWindowManager.appendMessageDirect("✅ 项目文档已生成并保存至: ${outFile.absolutePath}")
                        } catch (ioe: Exception) {
                            ChatWindowManager.appendMessageDirect("⚠️ 保存文档失败: ${ioe.message}")
                        }
                    } catch (ex: Exception) {
                        ChatWindowManager.appendMessageDirect("⚠️ 解析后端返回内容失败: ${ex.message}")
                    }
                }
            }
        })
    }
}
