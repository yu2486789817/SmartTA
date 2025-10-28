package com.example.smartta

import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.swing.SwingUtilities

object ChatService {

    private const val API_URL = "http://localhost:8000/ask"

    data class Question(val question: String, val context_code: String)
    data class Answer(val answer: String)

    /**
     * 使用共享的 HttpClient 和 Gson 实例
     * 性能优化：避免重复创建对象
     */
    fun askAsync(question: String, contextCode: String = "", onResult: (String) -> Unit) {
        try {
            val json = SharedServices.gson.toJson(Question(question, contextCode))
            val body = json.toRequestBody(SharedServices.JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .build()

            SharedServices.httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    SwingUtilities.invokeLater {
                        onResult("请求失败，请检查网络连接")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val answer = try {
                        if (!response.isSuccessful) {
                            "服务器返回错误 ${response.code}"
                        } else {
                            val respText = response.body?.string() ?: ""
                            SharedServices.gson.fromJson(respText, Answer::class.java).answer
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        "解析响应失败"
                    }

                    SwingUtilities.invokeLater {
                        onResult(answer)
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            SwingUtilities.invokeLater {
                onResult("发生未知错误")
            }
        }
    }
}
