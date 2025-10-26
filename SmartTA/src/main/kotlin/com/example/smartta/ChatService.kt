package com.example.smartta

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

object ChatService {

    private const val API_URL = "http://localhost:8000/ask"
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    data class Question(val question: String, val context_code: String)
    data class Answer(val answer: String)

    fun askAsync(question: String, contextCode: String = "", onResult: (String) -> Unit) {
        try {
            val json = gson.toJson(Question(question, contextCode))
            val body = json.toRequestBody(JSON)

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    SwingUtilities.invokeLater {
                        onResult("SmartTA: 请求失败")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val answer = try {
                        if (!response.isSuccessful) {
                            "SmartTA: 服务器返回错误 ${response.code}"
                        } else {
                            val respText = response.body?.string() ?: ""
                            gson.fromJson(respText, Answer::class.java).answer
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        "SmartTA: 解析响应失败"
                    }

                    SwingUtilities.invokeLater {
                        onResult(answer)
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            SwingUtilities.invokeLater {
                onResult("SmartTA: 出现未知错误")
            }
        }
    }
}
