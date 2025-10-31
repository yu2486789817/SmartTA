package com.example.smartta

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UploadSmartTAAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return

        // 打开 SmartTA 工具窗口
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("SmartTA")
        toolWindow?.show()

        val isDirectory = vFile.isDirectory
        val filePath = if (!isDirectory) vFile.path else null
        val directoryPath = if (isDirectory) vFile.path else null

        ChatWindowManager.sendMessage(MessageType.SYSTEM, "开始上传文件...")

        // 在后台线程执行上传操作
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

                // 添加文件或目录路径
                filePath?.let {
                    val file = File(it)
                    val mediaType = resolveMediaType(file)
                    builder.addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody(mediaType)
                    )
                }

                directoryPath?.let {
                    builder.addFormDataPart("directory", it)
                }

                val request = Request.Builder()
                    .url("http://localhost:8000/add_documents")
                    .post(builder.build())
                    .build()

                SharedServices.httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: "无返回内容"
                    ApplicationManager.getApplication().invokeLater {
                        // 解析后端返回的 JSON，提取友好的提示信息
                        val message = try {
                            val json = SharedServices.gson.fromJson(responseBody, Map::class.java) as? Map<*, *>
                            val status = json?.get("status")?.toString()
                            val msg = json?.get("message")?.toString()
                            when {
                                status == "成功" && msg != null -> msg
                                status == "失败" && msg != null -> "上传失败：$msg"
                                msg != null -> msg
                                else -> json.toString()
                            }
                        } catch (e: Exception) {
                            responseBody
                        }
                        ChatWindowManager.sendMessage(MessageType.SYSTEM, message)
                    }
                }

            } catch (ex: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    ChatWindowManager.sendMessage(MessageType.SYSTEM, "上传失败：${ex.message}")
                }
            }
        }
    }

    private fun resolveMediaType(file: File): MediaType {
        return when (file.extension.lowercase()) {
            "pdf" -> SharedServices.PDF_MEDIA_TYPE
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document".toMediaType()
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation".toMediaType()
            "txt" -> "text/plain; charset=utf-8".toMediaType()
            else -> "application/octet-stream".toMediaType()
        }
    }
}
