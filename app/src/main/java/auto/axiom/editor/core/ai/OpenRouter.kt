package auto.axiom.editor.core.ai

import android.content.Context
import auto.axiom.editor.core.settings.Settings.AI
import auto.axiom.editor.core.settings.dataStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ─── Request / Response DTOs ──────────────────────────────────────────────────

data class ChatMessage(val role: String, val content: String)

private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float,
    @SerializedName("max_tokens") val maxTokens: Int,
    val stream: Boolean = false
)

private data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
    val error: ChatError? = null
)

private data class ChatChoice(val message: ChatMessage? = null)
private data class StreamDelta(val content: String? = null)
private data class StreamChoice(val delta: StreamDelta? = null, val text: String? = null)
private data class StreamResponse(val choices: List<StreamChoice> = emptyList())
private data class ChatError(val message: String? = null, val code: Int? = null)

// ─── Anthropic (claude.ai-compatible) DTOs ───────────────────────────────────

private data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @SerializedName("max_tokens") val maxTokens: Int,
    val temperature: Float,
    val stream: Boolean = false,
    val system: String? = null
)

private data class AnthropicMessage(
    val role: String,
    val content: String
)

private data class AnthropicResponse(
    val content: List<AnthropicContent>? = null,
    val error: AnthropicError? = null
)

private data class AnthropicContent(val type: String, val text: String?)
private data class AnthropicError(val type: String?, val message: String?)

private data class AnthropicStreamEvent(
    val type: String? = null,
    val delta: AnthropicDelta? = null,
    val index: Int? = null
)

private data class AnthropicDelta(val type: String? = null, val text: String? = null)

// ─── OpenRouter API interface ─────────────────────────────────────────────────

private interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun complete(@Body request: ChatRequest): Response<ChatResponse>
}

// ─── Anthropic API interface ──────────────────────────────────────────────────

private interface AnthropicApi {
    @POST("messages")
    suspend fun complete(@Body request: AnthropicRequest): Response<AnthropicResponse>
}

// ─── Main object ─────────────────────────────────────────────────────────────

object OpenRouter {

    // ── Non-streaming generate (OpenRouter or Anthropic) ─────────────────────

    suspend fun generate(
        context: Context,
        prompt: String,
        systemPrompt: String? = null
    ): Result<String> {
        val prefs = context.dataStore.data.first()
        val provider = prefs[AI.AI_PROVIDER] ?: "openrouter"

        return if (provider == "anthropic") {
            generateAnthropic(context, prompt, systemPrompt)
        } else {
            generateOpenRouter(context, prompt, systemPrompt)
        }
    }

    private suspend fun generateOpenRouter(
        context: Context,
        prompt: String,
        systemPrompt: String?
    ): Result<String> {
        val prefs = context.dataStore.data.first()
        val apiKey = prefs[AI.OPENROUTER_API_KEY].orEmpty().trim()
        if (apiKey.isEmpty()) return Result.failure(
            IllegalStateException("OpenRouter API key is not configured. Open AI Settings to add it.")
        )

        val baseUrl = prefs[AI.OPENROUTER_BASE_URL]
            ?.trim()?.let { if (it.endsWith("/")) it else "$it/" }
            ?: "https://openrouter.ai/api/v1/"
        val model = prefs[AI.OPENROUTER_MODEL]?.trim()?.ifEmpty { "qwen/qwen3.8-flash" } ?: "qwen/qwen3.8-flash"
        val temperature = prefs[AI.TEMPERATURE] ?: 0.3f
        val maxTokens = prefs[AI.MAX_OUTPUT_TOKENS] ?: 4096

        return runCatching {
            val api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(openRouterClient(apiKey))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenRouterApi::class.java)

            val msgs = buildList {
                if (!systemPrompt.isNullOrBlank()) add(ChatMessage("system", systemPrompt))
                add(ChatMessage("user", prompt))
            }
            val response = api.complete(ChatRequest(model, msgs, temperature, maxTokens))
            if (!response.isSuccessful) {
                error(response.body()?.error?.message ?: "OpenRouter request failed (${response.code()})")
            }
            response.body()?.choices?.firstOrNull()?.message?.content
                ?.takeIf { it.isNotBlank() }
                ?: error("OpenRouter returned an empty response")
        }
    }

    private suspend fun generateAnthropic(
        context: Context,
        prompt: String,
        systemPrompt: String?
    ): Result<String> {
        val prefs = context.dataStore.data.first()
        val apiKey = prefs[AI.ANTHROPIC_API_KEY].orEmpty().trim()
        if (apiKey.isEmpty()) return Result.failure(
            IllegalStateException("Anthropic API key is not configured. Open AI Settings to add it.")
        )
        val model = prefs[AI.ANTHROPIC_MODEL]?.trim()?.ifEmpty { "claude-sonnet-4-6" } ?: "claude-sonnet-4-6"
        val temperature = prefs[AI.TEMPERATURE] ?: 0.7f
        val maxTokens = prefs[AI.MAX_OUTPUT_TOKENS] ?: 4096

        return runCatching {
            val api = Retrofit.Builder()
                .baseUrl("https://api.anthropic.com/v1/")
                .client(anthropicClient(apiKey))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AnthropicApi::class.java)

            val msgs = listOf(AnthropicMessage("user", prompt))
            val response = api.complete(
                AnthropicRequest(
                    model = model,
                    messages = msgs,
                    maxTokens = maxTokens,
                    temperature = temperature,
                    system = systemPrompt?.takeIf { it.isNotBlank() }
                )
            )
            if (!response.isSuccessful) {
                error(response.body()?.error?.message ?: "Anthropic request failed (${response.code()})")
            }
            response.body()?.content?.firstOrNull { it.type == "text" }?.text
                ?.takeIf { it.isNotBlank() }
                ?: error("Anthropic returned an empty response")
        }
    }

    // ── Streaming (OpenRouter or Anthropic) ───────────────────────────────────

    suspend fun stream(
        context: Context,
        messages: List<ChatMessage>,
        onDelta: suspend (String) -> Unit
    ): Result<Unit> {
        val prefs = context.dataStore.data.first()
        val provider = prefs[AI.AI_PROVIDER] ?: "openrouter"

        return if (provider == "anthropic") {
            streamAnthropic(context, messages, onDelta)
        } else {
            streamOpenRouter(context, messages, onDelta)
        }
    }

    private suspend fun streamOpenRouter(
        context: Context,
        messages: List<ChatMessage>,
        onDelta: suspend (String) -> Unit
    ): Result<Unit> {
        val prefs = context.dataStore.data.first()
        val apiKey = prefs[AI.OPENROUTER_API_KEY].orEmpty().trim()
        if (apiKey.isEmpty()) return Result.failure(
            IllegalStateException("OpenRouter API key is not configured.")
        )
        val baseUrl = prefs[AI.OPENROUTER_BASE_URL]?.trim()
            ?.let { if (it.endsWith("/")) it else "$it/" }
            ?: "https://openrouter.ai/api/v1/"
        val model = prefs[AI.OPENROUTER_MODEL]?.trim().orEmpty().ifEmpty { "qwen/qwen3.8-flash" }
        val temperature = prefs[AI.TEMPERATURE] ?: 0.3f
        val maxTokens = prefs[AI.MAX_OUTPUT_TOKENS] ?: 4096

        return runCatching {
            withContext(Dispatchers.IO) {
                val body = Gson().toJson(ChatRequest(model, messages, temperature, maxTokens, stream = true))
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url("${baseUrl}chat/completions").post(body).build()
                openRouterClient(apiKey).newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("OpenRouter request failed (${response.code})")
                    val source = response.body?.source() ?: error("Empty response body")
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val payload = line.removePrefix("data:").trim()
                        if (payload == "[DONE]") break
                        try {
                            Gson().fromJson(payload, StreamResponse::class.java)
                                .choices.firstOrNull()?.delta?.content?.let { onDelta(it) }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private suspend fun streamAnthropic(
        context: Context,
        messages: List<ChatMessage>,
        onDelta: suspend (String) -> Unit
    ): Result<Unit> {
        val prefs = context.dataStore.data.first()
        val apiKey = prefs[AI.ANTHROPIC_API_KEY].orEmpty().trim()
        if (apiKey.isEmpty()) return Result.failure(
            IllegalStateException("Anthropic API key is not configured.")
        )
        val model = prefs[AI.ANTHROPIC_MODEL]?.trim()?.ifEmpty { "claude-sonnet-4-6" } ?: "claude-sonnet-4-6"
        val temperature = prefs[AI.TEMPERATURE] ?: 0.7f
        val maxTokens = prefs[AI.MAX_OUTPUT_TOKENS] ?: 4096

        // Separate system message if present
        val systemMsg = messages.firstOrNull { it.role == "system" }?.content
        val chatMsgs = messages.filter { it.role != "system" }
            .map { AnthropicMessage(it.role, it.content) }

        val requestBody = AnthropicRequest(
            model = model,
            messages = chatMsgs,
            maxTokens = maxTokens,
            temperature = temperature,
            stream = true,
            system = systemMsg
        )

        return runCatching {
            withContext(Dispatchers.IO) {
                val body = Gson().toJson(requestBody).toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .post(body)
                    .build()
                anthropicClient(apiKey).newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Anthropic request failed (${response.code})")
                    val source = response.body?.source() ?: error("Empty response body")
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val payload = line.removePrefix("data:").trim()
                        if (payload == "[DONE]" || payload.contains("message_stop")) break
                        try {
                            val event = Gson().fromJson(payload, AnthropicStreamEvent::class.java)
                            if (event.type == "content_block_delta") {
                                event.delta?.text?.let { onDelta(it) }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Strips a single outer markdown code-fence (```…```) from [text].
     * Returns the original text unchanged if no fence is detected.
     */
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

    // ── OkHttp clients ───────────────────────────────────────────────────────

    private fun openRouterClient(apiKey: String): OkHttpClient = OkHttpClient.Builder()
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

    private fun anthropicClient(apiKey: String): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .build()
            )
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}
