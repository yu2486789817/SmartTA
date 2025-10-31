package com.example.smartta

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.*
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

class SmartTAToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatPanel = JPanel(BorderLayout())

        // 创建消息显示区域，使用 JTextPane 支持富文本样式
        val chatPane = JTextPane().apply {
            isEditable = false
            font = Font("Microsoft YaHei", Font.PLAIN, 14)
            margin = Insets(10, 10, 10, 10)
        }
        val doc: StyledDocument = chatPane.styledDocument

        // 定义不同消息类型的样式
        val youStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(135, 206, 235)) // 天蓝色 - 用户消息
            StyleConstants.setFontSize(this, 18)
        }
        val smarttaStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(255, 182, 193)) // 浅粉色 - SmartTA 响应
            StyleConstants.setFontSize(this, 18)
        }
        val systemStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color(255, 207, 72)) // 淡橙色 - 系统消息
            StyleConstants.setFontSize(this, 17)
        }

        // 消息追加函数，支持不同样式和格式
        fun appendMessage(fullMessage: String) {
            val colonIndex = fullMessage.indexOf('：').takeIf { it >= 0 } ?: fullMessage.indexOf(':')
            val splitIndex = if (colonIndex >= 0) colonIndex + 1 else -1
            if (splitIndex <= 0) {
                doc.insertString(doc.length, fullMessage + "\n\n", null)
            } else {
                val prefix = fullMessage.substring(0, splitIndex - 1).trim() + fullMessage[colonIndex]
                val rest = fullMessage.substring(splitIndex).trim()

                val style = when {
                    prefix.startsWith("用户") -> youStyle
                    prefix.startsWith("SmartTA") -> smarttaStyle
                    prefix.startsWith("系统") -> systemStyle
                    else -> null
                }

                // 插入加粗的前缀
                val boldStyle = style?.let { 
                    SimpleAttributeSet(it).apply { 
                        StyleConstants.setBold(this, true)
                        StyleConstants.setFontSize(this, 18)
                    }
                }
                doc.insertString(doc.length, prefix, boldStyle)
                
                // 插入换行和正文内容
                doc.insertString(doc.length, "\n$rest\n\n", null)
            }
            chatPane.caretPosition = doc.length
        }

        // 注册到 ChatWindowManager
        ChatWindowManager.chatAreaPane = chatPane
        ChatWindowManager.appendMessage = ::appendMessage
        ChatWindowManager.setStyles(youStyle, smarttaStyle, systemStyle)

        val scrollPane = JBScrollPane(chatPane).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        // 创建输入框
        val inputField = JTextField().apply {
            font = Font("Microsoft YaHei", Font.PLAIN, 14)
        }

        // 创建发送按钮
        val askButton = JButton("Ask").apply {
            font = Font("Segoe UI", Font.BOLD, 13)
        }

        // 处理用户提问逻辑
        fun sendQuestion() {
            val question = inputField.text.trim()
            if (question.isNotEmpty()) {
                appendMessage("用户：\n$question\n")
                inputField.text = ""
                askButton.isEnabled = false
                askButton.text = "Asking..."

                val json = """{"question": "$question"}"""
                ChatService.askAsync(json) { answer ->
                    SwingUtilities.invokeLater {
                        appendMessage("SmartTA：\n$answer")
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
