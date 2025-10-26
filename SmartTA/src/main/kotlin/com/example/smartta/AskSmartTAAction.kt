package com.example.smartta

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiJavaFile

class AskSmartTAAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

        val selectedText = editor.selectionModel.selectedText ?: ""
        val fileName = virtualFile?.name ?: "unknown"
        val fileType = virtualFile?.fileType?.name ?: "unknown"
        val packageName = (psiFile as? PsiJavaFile)?.packageName ?: ""

        val question = "结合文件名和文件类型解释这段Java代码的作用"
        val payload = mapOf(
            "question" to question,
            "context_code" to selectedText,
            "file_name" to fileName,
            "file_type" to fileType,
            "package_name" to packageName
        )


        val json = Gson().toJson(payload)

        // ✅ 1. 自动打开 SmartTA 工具窗口
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("SmartTA")
        toolWindow?.show()

        // ✅ 2. 立即显示 "You:" 提问，表示问题已提交
        ApplicationManager.getApplication().invokeLater {
            ChatWindowManager.appendMessageDirect("You: \n$question")
        }

        // ✅ 3. 异步调用后端
        ApplicationManager.getApplication().executeOnPooledThread {
            ChatService.askAsync(json) { answer ->
                ApplicationManager.getApplication().invokeLater {
                    ChatWindowManager.appendMessageDirect("SmartTA: \n$answer")
                }
            }
        }
    }
}
