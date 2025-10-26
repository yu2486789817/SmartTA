package com.example.smartta

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.*
import javax.swing.*
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

class SmartTAToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatPanel = JPanel(BorderLayout())

        // ✅ 使用 JTextPane 代替 JTextArea，支持不同前缀样式
        val chatPane = JTextPane().apply {
            isEditable = false
            font = Font("Microsoft YaHei", Font.PLAIN, 14)
            margin = Insets(10, 10, 10, 10)
        }
        val doc: StyledDocument = chatPane.styledDocument

        // 样式定义
        val youStyle = chatPane.addStyle("you", null).apply {
            StyleConstants.setForeground(this, Color.YELLOW)
            StyleConstants.setBold(this, true)
            StyleConstants.setFontSize(this, 18)
        }
        val smarttaStyle = chatPane.addStyle("smartta", null).apply {
            StyleConstants.setForeground(this, Color.MAGENTA)
            StyleConstants.setBold(this, true)
            StyleConstants.setFontSize(this, 18)
        }

        // 消息追加函数
        fun appendMessage(fullMessage: String) {
            val splitIndex = fullMessage.indexOf(":") + 1
            if (splitIndex <= 0) {
                doc.insertString(doc.length, fullMessage + "\n", null)
            } else {
                val prefix = fullMessage.substring(0, splitIndex)
                val rest = fullMessage.substring(splitIndex)
                val style = when {
                    prefix.startsWith("You") -> youStyle
                    prefix.startsWith("SmartTA") -> smarttaStyle
                    else -> null
                }
                doc.insertString(doc.length, prefix, style)
                doc.insertString(doc.length, rest + "\n", null)
            }
            chatPane.caretPosition = doc.length
        }

        // 注册到 ChatWindowManager
        ChatWindowManager.chatAreaPane = chatPane
        ChatWindowManager.appendMessage = ::appendMessage

        val scrollPane = JBScrollPane(chatPane).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        // 输入框
        val inputField = JTextField().apply {
            font = Font("Microsoft YaHei", Font.PLAIN, 14)
        }

        // Ask 按钮
        val askButton = JButton("Ask").apply {
            font = Font("Segoe UI", Font.BOLD, 13)
        }

        // 提问逻辑
        fun sendQuestion() {
            val question = inputField.text.trim()
            if (question.isNotEmpty()) {
                appendMessage("You: \n$question\n")
                inputField.text = ""
                askButton.isEnabled = false
                askButton.text = "Asking..."

                val json = """{"question": "$question"}"""
                ChatService.askAsync(json) { answer ->
                    SwingUtilities.invokeLater {
                        appendMessage("SmartTA: \n$answer")
                        askButton.isEnabled = true
                        askButton.text = "Ask"
                    }
                }
            }
        }

        // Enter 提交
        inputField.addActionListener { sendQuestion() }
        // 点击按钮提交
        askButton.addActionListener { sendQuestion() }

        // 输入区域布局
        val inputPanel = JPanel(BorderLayout(8, 0)).apply {
            add(inputField, BorderLayout.CENTER)
            add(askButton, BorderLayout.EAST)
        }

        chatPanel.add(scrollPane, BorderLayout.CENTER)
        chatPanel.add(inputPanel, BorderLayout.SOUTH)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(chatPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
