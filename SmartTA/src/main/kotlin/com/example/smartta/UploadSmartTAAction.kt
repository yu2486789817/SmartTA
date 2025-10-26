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

        // ✅ 自动打开 SmartTA 工具窗口
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("SmartTA")
        toolWindow?.show()

        val isDirectory = vFile.isDirectory
        val filePath = if (!isDirectory) vFile.path else null
        val directoryPath = if (isDirectory) vFile.path else null



        ChatWindowManager.appendMessageDirect("开始上传 ${vFile.name}...")

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val client = OkHttpClient()
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

                filePath?.let {
                    val file = File(it)
                    builder.addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("application/pdf".toMediaType())
                    )
                }

                directoryPath?.let {
                    builder.addFormDataPart("directory", it)
                }

                val request = Request.Builder()
                    .url("http://localhost:8000/add_pdfs")
                    .post(builder.build())
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: "无返回内容"
                    ApplicationManager.getApplication().invokeLater {
                        ChatWindowManager.appendMessageDirect("上传完成: $body")
                    }
                }

            } catch (ex: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    ChatWindowManager.appendMessageDirect("上传失败: ${ex.message}")
                }
            }
        }
    }
}
