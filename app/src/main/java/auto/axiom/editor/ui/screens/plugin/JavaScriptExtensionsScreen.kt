package auto.axiom.editor.ui.screens.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import auto.axiom.editor.extensions.InstalledExtension

@Composable
fun JavaScriptExtensionsScreen(
    extensions: List<InstalledExtension>,
    onInstall: () -> Unit,
    onSetEnabled: (InstalledExtension, Boolean) -> Unit,
    onUninstall: (InstalledExtension) -> Unit,
    modifier: Modifier = Modifier
) {
    var detailsExtension by remember { mutableStateOf<InstalledExtension?>(null) }
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("JavaScript Extensions", style = MaterialTheme.typography.headlineSmall)
                Text("Acode-style extensions for Axiom Editor", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onInstall) {
                Icon(Icons.Default.InstallMobile, contentDescription = null)
                Text("Install", modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (extensions.isEmpty()) {
            Text(
                "No JavaScript extensions installed. Install a ZIP package containing manifest.json and its main JavaScript file.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(extensions, key = { it.manifest.id }) { extension ->
                    ExtensionCard(
                        extension = extension,
                        onEnabledChange = { onSetEnabled(extension, it) },
                        onDetails = { detailsExtension = extension },
                        onUninstall = { onUninstall(extension) }
                    )
                }
            }
        }
    }
    detailsExtension?.let { extension ->
        AlertDialog(
            onDismissRequest = { detailsExtension = null },
            confirmButton = { Button(onClick = { detailsExtension = null }) { Text("Close") } },
            title = { Text(extension.manifest.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version ${extension.manifest.version}")
                    if (extension.manifest.author.isNotBlank()) Text("Author: ${extension.manifest.author}")
                    if (extension.manifest.description.isNotBlank()) Text(extension.manifest.description)
                    Text("Entry: ${extension.manifest.main}")
                    Text("Permissions: ${extension.manifest.permissions.ifEmpty { listOf("none") }.joinToString()}")
                }
            }
        )
    }
}

@Composable
private fun ExtensionCard(
    extension: InstalledExtension,
    onEnabledChange: (Boolean) -> Unit,
    onDetails: () -> Unit,
    onUninstall: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Extension, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(extension.manifest.name, style = MaterialTheme.typography.titleMedium)
                Text("v${extension.manifest.version} • ${extension.manifest.id}", style = MaterialTheme.typography.bodySmall)
                if (extension.manifest.description.isNotBlank()) Text(extension.manifest.description, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = extension.enabled, onCheckedChange = onEnabledChange)
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Extension actions")
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Details") }, onClick = { menuExpanded = false; onDetails() })
                    DropdownMenuItem(text = { Text("Uninstall") }, onClick = { menuExpanded = false; onUninstall() })
                }
            }
        }
    }
}
