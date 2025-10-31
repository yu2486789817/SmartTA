package com.example.smartta

import javax.swing.SwingUtilities
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.JTextPane

/**
 * 消息类型枚举
 */
enum class MessageType {
    USER,      // 用户消息
    SMARTTA,   // SmartTA 响应
    SYSTEM     // 系统消息（提示、错误、状态等）
}

object ChatWindowManager {
    var chatAreaPane: JTextPane? = null
    var userStyle: SimpleAttributeSet? = null
    var smarttaStyle: SimpleAttributeSet? = null
    var systemStyle: SimpleAttributeSet? = null

    /**
     * 消息追加回调函数
     */
    var appendMessage: ((String) -> Unit)? = null

    /**
     * 设置不同消息类型的样式
     * 
     * @param userStyle 用户消息样式
     * @param smarttaStyle SmartTA 响应样式
     * @param systemStyle 系统消息样式
     */
    fun setStyles(
        userStyle: SimpleAttributeSet,
        smarttaStyle: SimpleAttributeSet,
        systemStyle: SimpleAttributeSet
    ) {
        this.userStyle = userStyle
        this.smarttaStyle = smarttaStyle
        this.systemStyle = systemStyle
    }

    /**
     * 发送消息到聊天窗口
     * 根据消息类型自动添加前缀并格式化
     * 
     * @param type 消息类型
     * @param message 消息内容
     */
    fun sendMessage(type: MessageType, message: String) {
        val prefix = when (type) {
            MessageType.USER -> "用户："
            MessageType.SMARTTA -> "SmartTA："
            MessageType.SYSTEM -> "系统："
        }
        appendMessageDirect("$prefix $message")
    }

    /**
     * 直接追加文本到聊天窗口
     * 在 Swing Event Dispatch Thread 中执行，确保线程安全
     * 
     * @param message 要追加的消息
     */
    fun appendMessageDirect(message: String) {
        val pane = chatAreaPane ?: return
        SwingUtilities.invokeLater {
            val doc: StyledDocument = pane.styledDocument

            // 分离前缀和正文
            val colonIndex = message.indexOf('：').takeIf { it >= 0 } ?: message.indexOf(':')
            val splitIndex = if (colonIndex >= 0) colonIndex + 1 else -1
            if (splitIndex <= 0) {
                // 没有前缀，使用默认样式
                doc.insertString(doc.length, message + "\n\n", null)
            } else {
                val prefix = message.substring(0, splitIndex).trim()
                val rest = message.substring(splitIndex).trim()

                val style: SimpleAttributeSet? = when {
                    prefix.startsWith("用户") -> userStyle
                    prefix.startsWith("SmartTA") -> smarttaStyle
                    prefix.startsWith("系统") -> systemStyle
                    else -> null
                }

                // 插入前缀（带样式）
                if (style != null && prefix.isNotEmpty()) {
                    val boldStyle = SimpleAttributeSet(style).apply {
                        StyleConstants.setBold(this, true)
                    }
                    doc.insertString(doc.length, prefix, boldStyle)
                } else {
                    doc.insertString(doc.length, prefix, null)
                }

                // 插入换行和正文
                doc.insertString(doc.length, "\n$rest\n\n", null)
            }

            pane.caretPosition = doc.length
        }
    }
}
