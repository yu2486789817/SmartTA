package com.example.smartta

import javax.swing.SwingUtilities
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.JTextPane

object ChatWindowManager {
    var chatAreaPane: JTextPane? = null

    // 消息追加函数
    var appendMessage: ((String) -> Unit)? = null

    // 简单版本：直接追加文本，适用于外部调用
    fun appendMessageDirect(message: String) {
        val pane = chatAreaPane ?: return
        SwingUtilities.invokeLater {
            val doc: StyledDocument = pane.styledDocument

            // 分离前缀和正文
            val splitIndex = message.indexOf(":") + 1
            if (splitIndex <= 0) {
                doc.insertString(doc.length, message + "\n\n", null)
            } else {
                val prefix = message.substring(0, splitIndex)
                val rest = message.substring(splitIndex)

                val style: SimpleAttributeSet = SimpleAttributeSet()
                when {
                    prefix.startsWith("You") -> {
                        StyleConstants.setForeground(style, java.awt.Color.YELLOW)
                        StyleConstants.setFontSize(style, 18)
                    }
                    prefix.startsWith("SmartTA") -> {
                        StyleConstants.setForeground(style, java.awt.Color.MAGENTA)
                        StyleConstants.setFontSize(style, 18)
                    }
                    else -> StyleConstants.setForeground(style, java.awt.Color.WHITE)
                }
                StyleConstants.setBold(style, true)

                doc.insertString(doc.length, prefix, style)
                doc.insertString(doc.length, rest + "\n\n", null)
            }

            pane.caretPosition = doc.length
        }
    }
}
