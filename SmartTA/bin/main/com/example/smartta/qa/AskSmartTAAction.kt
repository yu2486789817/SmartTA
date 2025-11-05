package com.example.smartta.qa

import com.example.smartta.qa.ChatService
import com.example.smartta.qa.ChatWindowManager
import com.example.smartta.qa.MessageType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager

class AskSmartTAAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val selectedText = editor.selectionModel.selectedText ?: ""
        
        // 如果没有选中文本，提示用户
        if (selectedText.isEmpty()) {
            Messages.showWarningDialog(
                project,
                "请先选中要提问的代码",
                "SmartTA"
            )
            return
        }

        // 显示输入对话框，让用户输入问题
        val question = Messages.showInputDialog(
            project,
            "请输入您对这段代码的问题：",
            "询问 SmartTA",
            Messages.getQuestionIcon(),
            "",  // 默认值
            null  // 输入验证器（可为null）
        )

        // 如果用户取消了输入或输入为空，则不执行
        if (question.isNullOrBlank()) {
            return
        }

        // 1. 自动打开 SmartTA 工具窗口
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("SmartTA")
        toolWindow?.show()

        // 2. 立即显示用户提问
        // 注意: ChatWindowManager.sendMessage 内部已处理线程切换
        ChatWindowManager.sendMessage(MessageType.USER, question)


        // 3. 异步调用后端获取回答
        // 注意: ChatService.askAsync 内部已处理线程切换
        ChatService.askAsync(question, selectedText) { answer ->
            ChatWindowManager.sendMessage(MessageType.SMARTTA, answer)
        }
    }
}
