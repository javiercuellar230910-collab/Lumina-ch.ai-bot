package com.example.data.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

object GeminiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Calls Gemini API to generate content.
     * @param apiKey The API key to use. If empty, falls back to BuildConfig.GEMINI_API_KEY.
     * @param systemPrompt System instructions (role rules, tone, etc.)
     * @param conversationHistory List of previous turns (role alternating: user, model, user, model)
     */
    suspend fun generateContent(
        apiKey: String,
        systemPrompt: String,
        conversationHistory: List<Content>
    ): String {
        val finalApiKey = apiKey.trim().ifEmpty { BuildConfig.GEMINI_API_KEY }
        if (finalApiKey.isEmpty() || finalApiKey == "MY_GEMINI_API_KEY") {
            return "Error: No se ha configurado una Gemini API Key. Por favor, configúrala en Ajustes o en el panel de secretos."
        }

        val requestBodyData = GeminiRequest(
            contents = conversationHistory,
            systemInstruction = Content(parts = listOf(Part(systemPrompt))),
            generationConfig = GenerationConfig(
                temperature = 0.9f,
                topP = 0.95f
            )
        )

        val jsonRequest = requestAdapter.toJson(requestBodyData)
        val requestBody = jsonRequest.toRequestBody(jsonMediaType)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$finalApiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString == null) {
                    val errorMsg = try {
                        val parsed = responseAdapter.fromJson(bodyString ?: "")
                        parsed?.error?.message ?: "Código de error: ${response.code}"
                    } catch (e: Exception) {
                        "Código de error: ${response.code}"
                    }
                    return "Error de la API de Gemini: $errorMsg"
                }

                val apiResponse = responseAdapter.fromJson(bodyString)
                val responseText = apiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                responseText ?: "Error: El modelo no devolvió ninguna respuesta."
            }
        } catch (e: IOException) {
            "Error de red: ${e.localizedMessage ?: "No se pudo conectar con el servidor"}"
        } catch (e: Exception) {
            "Error inesperado: ${e.localizedMessage ?: "Ocurrió un error al procesar la respuesta"}"
        }
    }
}
