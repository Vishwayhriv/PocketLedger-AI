package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
data class GeminiConfig(
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiService {

    private suspend fun callGemini(prompt: String, currency: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API key is not configured. Please add your GEMINI_API_KEY in the Secrets panel."
        }

        val systemPrompt = "You are PocketLedger AI. You are a financial coach. Never invent numbers. Only answer using supplied financial data. If information is missing say \"I don't have enough data.\" Never guess. Never provide investment advice. Max response size is 250 words. Use simple language, short bullet points, and ensure it is extremely easy to read. Always output any financial values or monetary figures using the $currency symbol (e.g. ${currency}1,234.56). Never use any other currency symbol like $."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiConfig(temperature = 0.5f),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "PocketLedger AI was unable to read the response. Please try again."
        } catch (e: Exception) {
            "PocketLedger AI Coach is currently resting offline: ${e.localizedMessage}"
        }
    }

    suspend fun generateFinancialStory(summary: String, currency: String): String {
        val prompt = "Based ONLY on this compressed summary:\n$summary\n\nWrite a 2-3 sentence 'Salary Story' explaining where the salary went and 1 main tip. Keep it encouraging, positive, and direct. The selected currency is $currency. Ensure all generated monetary figures use the $currency symbol."
        return callGemini(prompt, currency)
    }

    suspend fun generateSpendingRoast(summary: String, currency: String): String {
        val prompt = "Based ONLY on this compressed summary:\n$summary\n\nWrite a friendly, humorous 'Spending Roast'. Highlight some funny spending observations, micro leaks, or subscription habits. Keep it lighthearted but highly motivating. The selected currency is $currency. Ensure all generated monetary figures use the $currency symbol."
        return callGemini(prompt, currency)
    }

    suspend fun generateSavingsAdvice(summary: String, currency: String): String {
        val prompt = "Based ONLY on this compressed summary:\n$summary\n\nGive me 3 precise, highly tactical bullet points of savings advice. No intro or outro, just straight actionable steps. The selected currency is $currency. Ensure all generated monetary figures use the $currency symbol."
        return callGemini(prompt, currency)
    }

    suspend fun askFinancialCoach(
        summary: String,
        chatHistory: List<Pair<String, Boolean>>, // Pair(MessageText, IsUser)
        question: String,
        currency: String
    ): String {
        val historyStr = chatHistory.takeLast(6).joinToString("\n") { (msg, isUser) ->
            if (isUser) "User: $msg" else "Coach: $msg"
        }
        val prompt = "Context Summary:\n$summary\n\nChat History:\n$historyStr\n\nUser Question: $question\n\nAnswer the user question directly and concisely using ONLY the summary context. Do not speculate. The selected currency is $currency. Ensure all generated monetary figures use the $currency symbol."
        return callGemini(prompt, currency)
    }
}
