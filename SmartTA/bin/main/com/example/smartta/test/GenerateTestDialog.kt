package com.example.smartta.test

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField

class GenerateTestDialog(
    private val className: String,
    private val methodName: String = "",
    private val contextCode: String = ""
) : DialogWrapper(true) {

    private val requirementField = JTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        preferredSize = Dimension(400, 120)
    }

    init {
        title = "生成单元测试 - $className${if (methodName.isNotEmpty()) ".$methodName" else ""}"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))

        val description = """
            请描述您想要测试的具体场景：
            • 例如："测试用户ID不存在的情况"
            • 例如："验证空字符串输入的边界条件" 
            • 例如："测试数据库连接失败时的异常处理"
        """.trimIndent()

        panel.add(JLabel(description), BorderLayout.NORTH)
        panel.add(JBScrollPane(requirementField), BorderLayout.CENTER)

        return panel
    }

    fun getRequirement(): String = requirementField.text.trim()
}
