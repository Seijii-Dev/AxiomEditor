package auto.axiom.editor.core.ai

import android.content.Context
import auto.axiom.editor.core.settings.dataStore
import auto.axiom.editor.core.settings.Settings.AI
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.first
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float,
    @SerializedName("max_tokens") val maxTokens: Int,
    val stream: Boolean = false
)

private data class ChatMessage(val role: String, val content: String)

private data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
    val error: ChatError? = null
)

private data class ChatChoice(val message: ChatMessage? = null)
private data class ChatError(val message: String? = null, val code: Int? = null)

private interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun complete(@Body request: ChatRequest): Response<ChatResponse>
}

object OpenRouter {
    suspend fun generate(context: Context, prompt: String, systemPrompt: String? = null): Result<String> {
        val preferences = context.dataStore.data.first()
        val apiKey = preferences[AI.OPENROUTER_API_KEY].orEmpty().trim()
        if (apiKey.isEmpty()) {
            return Result.failure(IllegalStateException("OpenRouter API key is not configured. Open AI Settings to add it."))
        }

        val baseUrl = preferences[AI.OPENROUTER_BASE_URL]
            ?.trim()
            ?.let { if (it.endsWith("/")) it else "$it/" }
            ?: "https://openrouter.ai/api/v1/"
        val model = preferences[AI.OPENROUTER_MODEL]
            ?.trim()
            ?.ifEmpty { "qwen/qwen3.8-flash" }
            ?: "qwen/qwen3.8-flash"
        val temperature = preferences[AI.TEMPERATURE] ?: 0.3f
        val maxTokens = preferences[AI.MAX_OUTPUT_TOKENS] ?: 4096

        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $apiKey")
                        .header("HTTP-Referer", "https://github.com/Seijii-Dev/AxiomEditor")
                        .header("X-OpenRouter-Title", "Axiom Editor")
                        .build()
                )
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return runCatching {
            val api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenRouterApi::class.java)

            val messages = buildList {
                if (!systemPrompt.isNullOrBlank()) add(ChatMessage("system", systemPrompt))
                add(ChatMessage("user", prompt))
            }
            val response = api.complete(
                ChatRequest(
                    model = model,
                    messages = messages,
                    temperature = temperature,
                    maxTokens = maxTokens
                )
            )
            if (!response.isSuccessful) {
                val message = response.body()?.error?.message ?: "OpenRouter request failed (${response.code()})"
                error(message)
            }
            response.body()?.choices?.firstOrNull()?.message?.content
                ?.takeIf { it.isNotBlank() }
                ?: error("OpenRouter returned an empty response")
        }
    }

    fun stripMarkdownFence(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```") || !trimmed.endsWith("```")) return text
        val firstLineEnd = trimmed.indexOf('\n')
        return if (firstLineEnd >= 0) {
            trimmed.substring(firstLineEnd + 1, trimmed.length - 3).trim()
        } else {
            trimmed.substring(3, trimmed.length - 3).trim()
        }
    }
}
