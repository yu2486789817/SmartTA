package com.example.smartta.docs

import com.example.smartta.ChatWindowManager
import com.example.smartta.MessageType
import com.example.smartta.SharedServices
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

object DocsGeneratorService {
    
    /**
     * 性能优化：使用共享的 HttpClient 和 Gson
     */
    private val client = SharedServices.httpClient
    private val gson = SharedServices.gson
    private val JSON = SharedServices.JSON_MEDIA_TYPE

    /**
     * 异步生成文档并保存到 projectPath/SmartTA_Doc.md
     * - projectPath: 项目根路径，用于保存文件
     * - projectInfo: ProjectScanner.scan 返回的 ProjectInfo 对象
     */
    fun generateDocsAsync(projectPath: String, projectInfo: ProjectInfo) {
        // 将 ProjectInfo 序列化为 JSON（Gson 会正确处理 data class）
        val json = gson.toJson(projectInfo)
        val body = json.toRequestBody(JSON)

        ChatWindowManager.sendMessage(MessageType.SYSTEM, "正在将项目结构发送至后端生成文档...")

        val request = Request.Builder()
            .url("http://localhost:8000/generate_docs")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                ChatWindowManager.sendMessage(MessageType.SYSTEM, "文档生成失败（网络错误）: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        ChatWindowManager.sendMessage(MessageType.SYSTEM, "后端返回错误: HTTP ${it.code}")
                        return
                    }

                    val respText = it.body?.string()
                    if (respText == null) {
                        ChatWindowManager.sendMessage(MessageType.SYSTEM, "后端未返回内容")
                        return
                    }

                    // 假设后端返回 {"markdown": "..." }
                    try {
                        val map = gson.fromJson(respText, Map::class.java)
                        val markdown = map["markdown"] as? String
                        if (markdown.isNullOrEmpty()) {
                            ChatWindowManager.sendMessage(MessageType.SYSTEM, "后端返回的 markdown 为空")
                            return
                        }

                        // 保存到文件（覆盖同名文件）
                        try {
                            val projectFile = File(projectPath)
                            val projectName = projectFile.name.ifEmpty { "SmartTA_Project" }
                            val outFile = File(projectPath, "${projectName}_SmartTA_Doc.md")
                            outFile.writeText(markdown)

                            ChatWindowManager.sendMessage(MessageType.SYSTEM, 
                                "项目文档已生成并保存至:\n${outFile.absolutePath}")
                        } catch (ioe: Exception) {
                            ChatWindowManager.sendMessage(MessageType.SYSTEM, "保存文档失败: ${ioe.message}")
                        }
                    } catch (ex: Exception) {
                        ChatWindowManager.sendMessage(MessageType.SYSTEM, "解析后端返回内容失败: ${ex.message}")
                    }
                }
            }
        })
    }
}
