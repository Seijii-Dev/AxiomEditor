package auto.axiom.editor.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import auto.axiom.editor.core.settings.Settings.AI.rememberAiMaxOutputTokens
import auto.axiom.editor.core.settings.Settings.AI.rememberAiProvider
import auto.axiom.editor.core.settings.Settings.AI.rememberAiStreamResponses
import auto.axiom.editor.core.settings.Settings.AI.rememberAiTemperature
import auto.axiom.editor.core.settings.Settings.AI.rememberAnthropicApiKey
import auto.axiom.editor.core.settings.Settings.AI.rememberAnthropicModel
import auto.axiom.editor.core.settings.Settings.AI.rememberOpenRouterApiKey
import auto.axiom.editor.core.settings.Settings.AI.rememberOpenRouterBaseUrl
import auto.axiom.editor.core.settings.Settings.AI.rememberOpenRouterModel

@Composable
fun AiSettingsScreen(onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val provider = rememberAiProvider()

    val openRouterApiKey = rememberOpenRouterApiKey()
    val openRouterModel = rememberOpenRouterModel()
    val openRouterBaseUrl = rememberOpenRouterBaseUrl()

    val anthropicApiKey = rememberAnthropicApiKey()
    val anthropicModel = rememberAnthropicModel()

    val temperature = rememberAiTemperature()
    val maxTokens = rememberAiMaxOutputTokens()
    val stream = rememberAiStreamResponses()

    BackHandler(onBack = onNavigateUp)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("AI Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Configure your AI provider and model.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Provider selector ────────────────────────────────────────────────
        item {
            SettingsSection(title = "Provider") {
                Text("Choose how Axiom connects to an AI model.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = provider.value == "openrouter",
                        onClick = { provider.value = "openrouter" },
                        label = { Text("OpenRouter") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = provider.value == "anthropic",
                        onClick = { provider.value = "anthropic" },
                        label = { Text("Anthropic (Claude)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── OpenRouter settings (conditional) ────────────────────────────────
        item {
            AnimatedVisibility(
                visible = provider.value == "openrouter",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SettingsSection(title = "OpenRouter") {
                    Text(
                        "Your API key is stored locally on this device and never sent to Axiom servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = openRouterApiKey.value,
                        onValueChange = { openRouterApiKey.value = it },
                        label = { Text("API Key") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = openRouterModel.value,
                        onValueChange = { openRouterModel.value = it },
                        label = { Text("Model") },
                        supportingText = { Text("Default: qwen/qwen3.8-flash") },
                        leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = openRouterBaseUrl.value,
                        onValueChange = { openRouterBaseUrl.value = it },
                        label = { Text("Base URL") },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── Anthropic settings (conditional) ─────────────────────────────────
        item {
            AnimatedVisibility(
                visible = provider.value == "anthropic",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SettingsSection(title = "Anthropic / Claude") {
                    Text(
                        "Your API key is stored locally on this device and never sent to Axiom servers. Get a key at console.anthropic.com.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = anthropicApiKey.value,
                        onValueChange = { anthropicApiKey.value = it },
                        label = { Text("Anthropic API Key") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = anthropicModel.value,
                        onValueChange = { anthropicModel.value = it },
                        label = { Text("Model") },
                        supportingText = { Text("e.g. claude-sonnet-4-6  ·  claude-opus-5  ·  claude-haiku-4-5-20251001") },
                        leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── Shared parameters ────────────────────────────────────────────────
        item {
            SettingsSection(title = "Generation") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Temperature: %.2f".format(temperature.value), style = MaterialTheme.typography.titleSmall)
                    Text("Lower = more focused, Higher = more creative", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = temperature.value,
                        onValueChange = { temperature.value = it },
                        valueRange = 0f..1.5f,
                        steps = 14
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = maxTokens.value.toString(),
                    onValueChange = {
                        maxTokens.value = it.toIntOrNull()?.coerceIn(256, 32768) ?: maxTokens.value
                    },
                    label = { Text("Maximum output tokens") },
                    leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stream responses", style = MaterialTheme.typography.titleSmall)
                        Text("Show model output as it arrives", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = stream.value, onCheckedChange = { stream.value = it })
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
