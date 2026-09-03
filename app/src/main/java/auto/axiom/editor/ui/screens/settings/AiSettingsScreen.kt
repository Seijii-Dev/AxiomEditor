package auto.axiom.editor.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import auto.axiom.editor.core.settings.Settings.AI.rememberAiMaxOutputTokens
import auto.axiom.editor.core.settings.Settings.AI.rememberAiStreamResponses
import auto.axiom.editor.core.settings.Settings.AI.rememberAiTemperature
import auto.axiom.editor.core.settings.Settings.AI.rememberOpenRouterApiKey
import auto.axiom.editor.core.settings.Settings.AI.rememberOpenRouterBaseUrl
import auto.axiom.editor.core.settings.Settings.AI.rememberOpenRouterModel

@Composable
fun AiSettingsScreen(onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val apiKey = rememberOpenRouterApiKey()
    val model = rememberOpenRouterModel()
    val baseUrl = rememberOpenRouterBaseUrl()
    val temperature = rememberAiTemperature()
    val maxTokens = rememberAiMaxOutputTokens()
    val stream = rememberAiStreamResponses()

    BackHandler(onBack = onNavigateUp)
    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "OpenRouter AI",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Configure the model used by Axiom’s AI tools. The API key is intentionally visible and is stored locally on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        item {
            OutlinedTextField(
                value = apiKey.value,
                onValueChange = { apiKey.value = it },
                label = { Text("OpenRouter API key") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = model.value,
                onValueChange = { model.value = it },
                label = { Text("Model") },
                supportingText = { Text("Default: qwen/qwen3.8-flash") },
                leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = baseUrl.value,
                onValueChange = { baseUrl.value = it },
                label = { Text("Base URL") },
                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Column {
                Text("Temperature: %.1f".format(temperature.value), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = temperature.value,
                    onValueChange = { temperature.value = it },
                    valueRange = 0f..1.5f,
                    steps = 14
                )
            }
        }
        item {
            OutlinedTextField(
                value = maxTokens.value.toString(),
                onValueChange = { maxTokens.value = it.toIntOrNull()?.coerceIn(256, 32768) ?: maxTokens.value },
                label = { Text("Maximum output tokens") },
                leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stream responses", style = MaterialTheme.typography.titleMedium)
                    Text("Show model output as it arrives", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = stream.value, onCheckedChange = { stream.value = it })
            }
        }
    }
}
