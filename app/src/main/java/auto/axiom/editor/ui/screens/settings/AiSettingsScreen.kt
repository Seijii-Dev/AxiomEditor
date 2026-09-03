package auto.axiom.editor.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import auto.axiom.editor.core.settings.Settings.AI.MAX_OUTPUT_TOKENS
import auto.axiom.editor.core.settings.Settings.AI.OPENROUTER_API_KEY
import auto.axiom.editor.core.settings.Settings.AI.OPENROUTER_BASE_URL
import auto.axiom.editor.core.settings.Settings.AI.OPENROUTER_MODEL
import auto.axiom.editor.core.settings.Settings.AI.STREAM_RESPONSES
import auto.axiom.editor.core.settings.Settings.AI.TEMPERATURE
import auto.axiom.editor.core.settings.Settings.AI.rememberAiMaxOutputTokens
import auto.axiom.editor.core.settings.Settings.AI.rememberAiStreamResponses
import auto.axiom.editor.core.settings.Settings.AI.rememberAiTemperature
import auto.axiom.editor.core.settings.Settings.AI.rememberOpenRouterApiKey
import auto.axiom.editor.core.settings.Settings.AI.rememberOpenRouterBaseUrl
import auto.axiom.editor.core.settings.Settings.AI.rememberOpenRouterModel
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference

@Composable
fun AiSettingsScreen(onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val apiKey = rememberOpenRouterApiKey()
    val model = rememberOpenRouterModel()
    val baseUrl = rememberOpenRouterBaseUrl()
    val temperature = rememberAiTemperature()
    val maxTokens = rememberAiMaxOutputTokens()
    val stream = rememberAiStreamResponses()

    BackHandler(onBack = onNavigateUp)
    ProvidePreferenceLocals {
        LazyColumn(
            modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item {
                Text(
                    text = "OpenRouter powers Axiom’s AI tools. Your key is stored on this device and is visible here by design. Do not reuse an unrestricted key if the APK is distributed.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            preferenceCategory(key = "openrouter_connection", title = { Text("OpenRouter") })
            textFieldPreference(
                key = OPENROUTER_API_KEY.name,
                title = { Text("API key") },
                summary = { Text(if (it.isBlank()) "Add your OpenRouter API key" else "API key configured") },
                rememberState = { apiKey },
                defaultValue = apiKey.value,
                icon = { androidx.compose.material3.Icon(Icons.Default.Key, null) },
                modifier = Modifier.clip(PreferenceShape.Top)
            )
            textFieldPreference(
                key = OPENROUTER_MODEL.name,
                title = { Text("Model") },
                summary = { Text(it) },
                rememberState = { model },
                defaultValue = model.value,
                icon = { androidx.compose.material3.Icon(Icons.Default.Memory, null) },
                modifier = Modifier.clip(PreferenceShape.Middle)
            )
            textFieldPreference(
                key = OPENROUTER_BASE_URL.name,
                title = { Text("Base URL") },
                summary = { Text(it) },
                rememberState = { baseUrl },
                defaultValue = baseUrl.value,
                icon = { androidx.compose.material3.Icon(Icons.Default.Tune, null) },
                modifier = Modifier.clip(PreferenceShape.Bottom)
            )
            preferenceCategory(key = "generation_settings", title = { Text("Generation") })
            sliderPreference(
                key = TEMPERATURE.name,
                title = { Text("Temperature") },
                valueText = { Text(String.format("%.1f", it)) },
                rememberState = { temperature },
                defaultValue = temperature.value,
                valueRange = 0f..1.5f,
                valueSteps = 14,
                icon = { androidx.compose.material3.Icon(Icons.Default.Tune, null) },
                modifier = Modifier.clip(PreferenceShape.Top)
            )
            textFieldPreference(
                key = MAX_OUTPUT_TOKENS.name,
                title = { Text("Maximum output tokens") },
                summary = { Text(it.toString()) },
                rememberState = { maxTokens },
                defaultValue = maxTokens.value,
                textToValue = { it.toIntOrNull()?.coerceIn(256, 32768) ?: maxTokens.value },
                icon = { androidx.compose.material3.Icon(Icons.Default.Memory, null) },
                modifier = Modifier.clip(PreferenceShape.Middle)
            )
            switchPreference(
                key = STREAM_RESPONSES.name,
                title = { Text("Stream responses") },
                summary = { Text(if (it) "Show responses as they are generated" else "Wait for the complete response") },
                rememberState = { stream },
                defaultValue = stream.value,
                icon = { androidx.compose.material3.Icon(Icons.Default.Tune, null) },
                modifier = Modifier.clip(PreferenceShape.Bottom)
            )
        }
    }
}
