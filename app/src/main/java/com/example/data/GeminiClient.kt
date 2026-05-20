package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Generates a dynamic explanation or quiz overlay using Gemini.
     * Falls back to high-quality procedural response if API fails or API key is not present.
     */
    suspend fun generateText(prompt: String, customApiKey: String? = null): String {
        val resolvedKey = when {
            !customApiKey.isNullOrBlank() && customApiKey != "MY_GEMINI_API_KEY" -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> null
        }

        if (resolvedKey == null) {
            Log.w(TAG, "No valid Gemini API key found. Using simulated educational generator.")
            return simulateResponseForPrompt(prompt)
        }

        val requestBody = GenerateContentRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )

        return try {
            val response = apiService.generateContent(resolvedKey, requestBody)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize, but I could not digest the learning concept at this moment."
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API Call failed: ${e.message}. Falling back to offline simulation.", e)
            simulateResponseForPrompt(prompt)
        }
    }

    /**
     * Simulation method providing incredibly realistic, rich transcriptions and quizzes
     * if the user is in an offline setting or hasn't added their API key.
     */
    private fun simulateResponseForPrompt(prompt: String): String {
        return when {
            prompt.contains("quiz", ignoreCase = true) -> {
                """
                {
                  "question": "Which of the following is the most vital advantage of micro-learning for African youth?",
                  "options": ["Requires high continuous data streaming", "Drives bite-sized learning retention in short focus windows", "Replaces universities entirely", "Only works online with high-bandwidth"],
                  "answer": "Drives bite-sized learning retention in short focus windows"
                }
                """.trimIndent()
            }
            prompt.contains("caption", ignoreCase = true) || prompt.contains("transcribe", ignoreCase = true) -> {
                "[00:02] Hey everyone! Elevate your tech game today. ✨\n" +
                "[00:08] Africa holds the highest rate of coding and tech interest. 💻\n" +
                "[00:15] Let's break down how variables impact code compiling.\n" +
                "[00:22] A variable is just a container holding data, like a container on a ship. 🚢\n" +
                "[00:30] Keep checking back as we study micro-algorithms next! 🌟"
            }
            else -> {
                "Education on SkillBite is designed to maximize visual weight, data-saver optimization, and rapid comprehension of coding, trading, and business. Start leveling up today!"
            }
        }
    }
}
