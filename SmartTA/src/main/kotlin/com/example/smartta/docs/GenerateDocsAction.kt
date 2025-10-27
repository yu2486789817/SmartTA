package com.example.smartta.docs

import com.example.smartta.ChatWindowManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager
import java.io.File

class GenerateDocsAction : AnAction("Generate Project Docs") {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectPath = project.basePath ?: return

        // 自动打开 SmartTA 工具窗口
        ToolWindowManager.getInstance(project).getToolWindow("SmartTA")?.show()

        ChatWindowManager.appendMessageDirect("📘 开始扫描项目: $projectPath")

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val projectInfo = ProjectScanner.scan(projectPath) // 返回 ProjectInfo
                ChatWindowManager.appendMessageDirect("🧾 扫描完成：发现 ${projectInfo.totalFiles} 个 Java 文件。")

                // 异步请求后端并保存文档
                DocsGeneratorService.generateDocsAsync(projectPath, projectInfo)
            } catch (ex: Exception) {
                ChatWindowManager.appendMessageDirect("❌ 扫描失败: ${ex.message}")
            }
        }
    }
}
