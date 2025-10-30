package com.example.smartta

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.IOException
import javax.swing.*

/**
 * Git提交消息生成对话框
 * 显示生成的提交消息并提供复制功能
 */
class GitCommitMessageDialog(
    private val project: Project,
    private val gitDiff: String
) : DialogWrapper(project) {

    private val messageTextArea = JTextArea()
    private val loadingLabel = JLabel("正在生成提交消息...")
    private val contentPanel = JPanel(BorderLayout())
    private val copyButton = JButton("复制到剪贴板")
    private val regenerateButton = JButton("重新生成")

    init {
        title = "SmartTA - Git提交消息生成器"
        init()
        
        // 设置文本区域属性
        messageTextArea.lineWrap = true
        messageTextArea.wrapStyleWord = true
        messageTextArea.isEditable = true
        messageTextArea.font = messageTextArea.font.deriveFont(14f)
        
        // 初始显示加载状态
        showLoading()
        
        // 异步生成提交消息
        generateCommitMessage()
    }

    override fun createCenterPanel(): JComponent {
        contentPanel.preferredSize = Dimension(600, 300)
        return contentPanel
    }

    override fun createActions(): Array<Action> {
        // 返回空数组，我们使用自定义按钮
        return emptyArray()
    }

    override fun createSouthPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        
        // 复制按钮
        copyButton.isEnabled = false
        copyButton.addActionListener {
            copyToClipboard(messageTextArea.text)
        }
        
        // 重新生成按钮
        regenerateButton.isEnabled = false
        regenerateButton.addActionListener {
            regenerate()
        }
        
        // 关闭按钮
        val closeButton = JButton("关闭")
        closeButton.addActionListener {
            close(0)
        }
        
        panel.add(Box.createHorizontalGlue())
        panel.add(copyButton)
        panel.add(Box.createHorizontalStrut(10))
        panel.add(regenerateButton)
        panel.add(Box.createHorizontalStrut(10))
        panel.add(closeButton)
        panel.add(Box.createHorizontalGlue())
        
        return panel
    }

    /**
     * 显示加载状态
     */
    private fun showLoading() {
        contentPanel.removeAll()
        loadingLabel.horizontalAlignment = SwingConstants.CENTER
        contentPanel.add(loadingLabel, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()
    }

    /**
     * 显示生成的消息
     */
    private fun showMessage(message: String) {
        SwingUtilities.invokeLater {
            contentPanel.removeAll()
            messageTextArea.text = message
            val scrollPane = JBScrollPane(messageTextArea)
            contentPanel.add(scrollPane, BorderLayout.CENTER)
            contentPanel.revalidate()
            contentPanel.repaint()
            
            // 启用按钮
            copyButton.isEnabled = true
            regenerateButton.isEnabled = true
        }
    }

    /**
     * 显示错误消息
     */
    private fun showError(errorMessage: String) {
        SwingUtilities.invokeLater {
            contentPanel.removeAll()
            val errorLabel = JLabel("<html><center>生成失败<br><br>$errorMessage</center></html>")
            errorLabel.horizontalAlignment = SwingConstants.CENTER
            contentPanel.add(errorLabel, BorderLayout.CENTER)
            contentPanel.revalidate()
            contentPanel.repaint()
            
            // 启用重新生成按钮
            regenerateButton.isEnabled = true
        }
    }

    /**
     * 生成提交消息
     */
    private fun generateCommitMessage() {
        try {
            val requestData = mapOf("git_diff" to gitDiff)
            val json = SharedServices.gson.toJson(requestData)
            val body = json.toRequestBody(SharedServices.JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("http://localhost:8000/generate_commit_message")
                .post(body)
                .build()

            SharedServices.httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    showError("网络请求失败: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            showError("服务器返回错误: ${response.code}")
                            return
                        }

                        val responseText = response.body?.string() ?: ""
                        val result = SharedServices.gson.fromJson(
                            responseText,
                            Map::class.java
                        ) as Map<*, *>

                        val commitMessage = result["commit_message"] as? String
                        if (commitMessage != null) {
                            showMessage(commitMessage)
                        } else {
                            showError("无法解析服务器响应")
                        }

                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        showError("解析响应失败: ${ex.message}")
                    }
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
            showError("发生未知错误: ${e.message}")
        }
    }

    /**
     * 重新生成
     */
    private fun regenerate() {
        copyButton.isEnabled = false
        regenerateButton.isEnabled = false
        showLoading()
        generateCommitMessage()
    }

    /**
     * 复制到剪贴板
     */
    private fun copyToClipboard(text: String) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val stringSelection = StringSelection(text)
            clipboard.setContents(stringSelection, null)
            
            Messages.showInfoMessage(
                project,
                "提交消息已复制到剪贴板",
                "SmartTA"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Messages.showErrorDialog(
                project,
                "复制失败: ${e.message}",
                "SmartTA"
            )
        }
    }
}

