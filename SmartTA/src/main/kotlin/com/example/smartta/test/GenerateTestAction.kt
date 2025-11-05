package com.example.smartta.test

import com.example.smartta.common.SharedServices
import com.example.smartta.qa.ChatWindowManager
import com.example.smartta.qa.MessageType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiClass
import okhttp3.RequestBody.Companion.toRequestBody

class GenerateTestAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        // 获取选中的代码或整个类
        val selectedText = editor.selectionModel.selectedText
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)

        var className = ""
        var methodName = ""
        var contextCode = ""

        // 分析选中的元素
        when (psiElement) {
            is PsiClass -> {
                className = psiElement.name ?: "未命名类"
                contextCode = psiElement.text ?: ""
            }
            is PsiMethod -> {
                methodName = psiElement.name
                val containingClass = psiElement.containingClass
                className = containingClass?.name ?: "未命名类"
                contextCode = psiElement.text ?: ""
            }
            else -> {
                // 如果没有选中特定元素，使用整个文件
                className = virtualFile.nameWithoutExtension
                contextCode = selectedText ?: psiFile.text
            }
        }

        // 显示需求输入对话框
        val dialog = GenerateTestDialog(className, methodName, contextCode)
        if (dialog.showAndGet()) {
            val requirement = dialog.getRequirement()

            if (requirement.isBlank()) {
                ChatWindowManager.sendMessage(MessageType.SYSTEM, "错误：请填写测试需求描述")
                return
            }

            // 自动打开 SmartTA 工具窗口
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("SmartTA")
            toolWindow?.show()

            // 显示用户需求和目标信息
            ChatWindowManager.sendMessage(MessageType.USER, "生成单元测试需求：$requirement")
            ChatWindowManager.sendMessage(MessageType.SYSTEM, "目标：$className${if (methodName.isNotEmpty()) ".$methodName" else ""}")

            // 异步生成测试
            generateTestAsync(requirement, contextCode, className, methodName) { result ->
                // 判断结果是否为错误消息
                val isError = result.startsWith("生成测试失败") || 
                             result.startsWith("HTTP错误") || 
                             result.startsWith("解析响应失败") ||
                             result.startsWith("生成测试时出现错误")
                
                if (isError) {
                    ChatWindowManager.sendMessage(MessageType.SYSTEM, result)
                } else {
                    ChatWindowManager.sendMessage(MessageType.SMARTTA, "生成的单元测试：\n```java\n$result\n```")
                }
            }
        }
    }

    /**
     * 异步生成单元测试
     * 使用共享的 HttpClient 和 Gson 以避免重复创建对象
     */
    private fun generateTestAsync(
        requirement: String,
        contextCode: String,
        className: String,
        methodName: String,
        onResult: (String) -> Unit
    ) {
        try {
            val payload = mapOf(
                "requirement" to requirement,
                "context_code" to contextCode,
                "class_name" to className,
                "method_name" to methodName
            )

            val json = SharedServices.gson.toJson(payload)
            val body = json.toRequestBody(SharedServices.JSON_MEDIA_TYPE)

            val request = okhttp3.Request.Builder()
                .url("http://localhost:8000/generate_test")
                .post(body)
                .build()

            SharedServices.httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    ApplicationManager.getApplication().invokeLater {
                        onResult("生成测试失败：${e.message}")
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val result = try {
                        if (!response.isSuccessful) {
                            "HTTP错误：${response.code}"
                        } else {
                            val responseBody = response.body?.string() ?: ""
                            val jsonResponse = SharedServices.gson.fromJson(responseBody, Map::class.java) as? Map<*, *>

                            if (jsonResponse?.get("status") == "成功") {
                                jsonResponse?.get("test_code") as? String ?: "未返回测试代码"
                            } else {
                                jsonResponse?.get("error") as? String ?: "未知错误"
                            }
                        }
                    } catch (ex: Exception) {
                        "解析响应失败：${ex.message}"
                    }

                    ApplicationManager.getApplication().invokeLater {
                        onResult(result)
                    }
                }
            })

        } catch (e: Exception) {
            ApplicationManager.getApplication().invokeLater {
                onResult("生成测试时出现错误：${e.message}")
            }
        }
    }
}
