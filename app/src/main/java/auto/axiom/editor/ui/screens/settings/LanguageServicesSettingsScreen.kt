package auto.axiom.editor.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import auto.axiom.editor.activities.TerminalActivity
import auto.axiom.editor.core.settings.Settings.LanguageServices.rememberExternalEnabled
import auto.axiom.editor.core.settings.Settings.LanguageServices.rememberExternalHost
import auto.axiom.editor.core.settings.Settings.LanguageServices.rememberExternalPort
import auto.axiom.editor.core.settings.Settings.LanguageServices.rememberOfflineEnabled
import auto.axiom.editor.lsp.LanguageServiceManager
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference

@Composable
fun LanguageServicesSettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit
) {
    BackHandler(onBack = onNavigateUp)
    val offline = rememberOfflineEnabled()
    val external = rememberExternalEnabled()
    val host = rememberExternalHost()
    val port = rememberExternalPort()
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(external.value, host.value, port.value) {
        LanguageServiceManager.configureTcpServers(external.value, host.value, port.value)
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        preferenceCategory(
            key = "language_services_category",
            title = { Text("Language Services") }
        )
        switchPreference(
            key = "offline_language_services",
            title = { Text("Offline language intelligence") },
            summary = { Text(if (it) "Always-on completions and basic diagnostics" else "Disabled") },
            rememberState = { offline },
            defaultValue = offline.value,
            icon = { Icon(Icons.Rounded.Code, contentDescription = null) },
            modifier = Modifier.clip(PreferenceShape.Top).background(background)
        )
        switchPreference(
            key = "external_language_services",
            title = { Text("Use external language server") },
            summary = { Text(if (it) "Connect to the configured LSP endpoint" else "Offline mode only") },
            rememberState = { external },
            defaultValue = external.value,
            icon = { Icon(Icons.Rounded.Cloud, contentDescription = null) },
            modifier = Modifier.clip(PreferenceShape.Middle).background(background)
        )
        textFieldPreference(
            key = "language_server_host",
            title = { Text("External server host") },
            summary = { Text(it) },
            rememberState = { host },
            defaultValue = host.value,
            enabled = { external.value },
            textToValue = { it },
            modifier = Modifier.clip(PreferenceShape.Middle).background(background)
        )
        textFieldPreference(
            key = "language_server_port",
            title = { Text("External server port") },
            summary = { Text(it.toString()) },
            rememberState = { port },
            defaultValue = port.value,
            enabled = { external.value },
            textToValue = { it.toIntOrNull()?.coerceIn(1, 65535) ?: port.value },
            modifier = Modifier.clip(PreferenceShape.Bottom).background(background)
        )
        item {
            Text(
                "Supported offline languages: HTML, CSS, JavaScript, TypeScript, Python, Java, Kotlin, JSON, and XML. External servers can provide richer semantic features when a compatible bridge is available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        item {
            Text("Semantic language servers", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Install runtimes into Axiom’s built-in Linux environment. The terminal will show progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(
                    "Python — Pyright" to "apk add python3 py3-pip py3-virtualenv && python3 -m venv \$HOME/.venvs/pyright && \$HOME/.venvs/pyright/bin/python -m pip install --upgrade pip pyright",
                    "JavaScript / TypeScript — TypeScript LS" to "apk add nodejs npm && npm install -g typescript typescript-language-server",
                    "Java — Eclipse JDT LS" to "apk add openjdk17-jre"
                ).forEach { (name, command) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            context.startActivity(Intent(context, TerminalActivity::class.java).apply {
                                putExtra(TerminalActivity.KEY_INITIAL_COMMAND, command)
                            })
                        }) { Text("Install") }
                    }
                }
            }
        }
    }
}
