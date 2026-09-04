/*
 * This file is part of Axiom Editor.
 *
 * Axiom Editor is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Axiom Editor is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Axiom Editor.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package auto.axiom.editor.ui.screens.plugin

import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blankj.utilcode.util.UriUtils
import auto.axiom.editor.PluginConstants
import auto.axiom.editor.app.strings
import auto.axiom.editor.extensions.toFile
import auto.axiom.editor.extensions.ExtensionPackageManager
import auto.axiom.editor.extensions.InstalledExtension
import java.io.File
import auto.axiom.editor.plugins.PluginLoader
import auto.axiom.editor.plugins.internal.PluginInfo
import auto.axiom.editor.ui.LocalToastHostState
import auto.axiom.editor.ui.screens.PluginScreens
import auto.axiom.editor.ui.screens.plugin.components.InstalledPluginList
import auto.axiom.editor.ui.screens.plugin.JavaScriptExtensionsScreen
import auto.axiom.editor.ui.screens.plugin.components.NewPluginButton
import auto.axiom.editor.ui.screens.plugin.components.NewPluginSheet
import auto.axiom.editor.ui.screens.plugin.components.PluginTabs
import auto.axiom.editor.ui.screens.plugin.components.PluginTopBar
import auto.axiom.editor.utils.GradleJavaLibraryProjectCreator
import auto.axiom.editor.utils.launchWithProgressDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PluginsScreen(
    modifier: Modifier = Modifier,
    viewModel: PluginViewModel = viewModel(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) {
    val installedPluginListState = rememberLazyListState()
    val expandedFab by remember { derivedStateOf { installedPluginListState.firstVisibleItemIndex == 0 } }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    var showNewPluginDialog by remember { mutableStateOf(false) }

    val toastHostState = LocalToastHostState.current
    val context = LocalContext.current
    val extensionManager = remember { ExtensionPackageManager(context) }
    var installedExtensions by remember { mutableStateOf<List<InstalledExtension>>(emptyList()) }

    fun reloadExtensions() {
        installedExtensions = extensionManager.listInstalled()
    }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledPlugins(context)
        reloadExtensions()
    }

    val openExtensionFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    val temporaryZip = File(context.cacheDir, "extension-${System.currentTimeMillis()}.zip")
                    context.contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "Unable to read extension package" }
                        temporaryZip.outputStream().use { output -> input.copyTo(output) }
                    }
                    extensionManager.install(temporaryZip)
                    temporaryZip.delete()
                    reloadExtensions()
                }.onSuccess {
                    toastHostState.showToast("Extension installed", Icons.Rounded.Check)
                }.onFailure {
                    toastHostState.showToast(it.message ?: "Unable to install extension", Icons.Rounded.ErrorOutline)
                }
            }
        }
    }

    var pluginToUpdate: PluginInfo? by remember { mutableStateOf(null) }

    val openFile =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    pluginToUpdate?.let {
                        val file = "${PluginConstants.PLUGIN_HOME_PATH}/${it.name}".toFile()
                        if (file.exists()) {
                            file.deleteRecursively()
                        }
                        viewModel.loadInstalledPlugins(context)
                    }

                    val pluginDir = PluginLoader.extractPluginZip(UriUtils.uri2File(uri))
                    viewModel.loadInstalledPlugins(
                        context = context,
                        onSuccessfullyLoaded = {
                            toastHostState.showToast(
                                message = "Plugin ${if (pluginToUpdate != null) "updated" else "imported"} successfully",
                                icon = Icons.Rounded.Check
                            )
                            pluginToUpdate = null
                        },
                        onError = {
                            withContext(Dispatchers.Main) {
                                toastHostState.showToast(
                                    message = it.message ?: "Error loading plugin",
                                    icon = Icons.Rounded.ErrorOutline
                                )
                            }

                            if (pluginDir.exists()) pluginDir.deleteRecursively()
                            pluginToUpdate = null
                        }
                    )
                }
            }
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PluginTopBar()
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = (navBackStackEntry?.destination?.route == PluginScreens.Installed.route)
            ) {
                NewPluginButton(
                    expanded = expandedFab,
                    onCreatePlugin = { showNewPluginDialog = true },
                    onImportPlugin = {
                        openFile.launch(
                            arrayOf(
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip")
                                    ?: "application/zip"
                            )
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        val currentRoute = navBackStackEntry?.destination?.route ?: PluginScreens.Installed.route

        Column(
            modifier = modifier.padding(innerPadding)
        ) {
            PluginTabs(
                currentRoute = currentRoute,
                navController = navController
            )

            NavHost(
                navController = navController,
                startDestination = PluginScreens.Installed.route
            ) {
                composable(PluginScreens.Installed.route) {
                    InstalledPluginList(
                        viewModel = viewModel,
                        listState = installedPluginListState,
                        scope = coroutineScope,
                        onUpdateClick = {
                            pluginToUpdate = it
                            openFile.launch(
                                arrayOf(
                                    MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip")
                                        ?: "application/zip"
                                )
                            )
                        }
                    )
                }
                composable(PluginScreens.Extensions.route) {
                    JavaScriptExtensionsScreen(
                        extensions = installedExtensions,
                        onInstall = {
                            openExtensionFile.launch(arrayOf("application/zip", "application/octet-stream"))
                        },
                        onSetEnabled = { extension, enabled ->
                            extensionManager.setEnabled(extension.manifest.id, enabled)
                            reloadExtensions()
                        },
                        onUninstall = { extension ->
                            extensionManager.uninstall(extension.manifest.id)
                            reloadExtensions()
                        }
                    )
                }
            }
        }
    }

    if (showNewPluginDialog) {
        NewPluginSheet(
            onCreate = { pluginInfo, pluginDir ->
                coroutineScope.launchWithProgressDialog(
                    uiContext = context,
                    configureBuilder = {
                        it.apply {
                            setMessage("Creating plugin...")
                            setCancelable(false)
                        }
                    }
                ) { _, _ ->
                    GradleJavaLibraryProjectCreator.createGradleJavaLibraryProject(
                        context = context,
                        baseDir = pluginDir,
                        packageName = pluginInfo.packageName!!,
                        fullClassName = pluginInfo.mainClass!!
                    )
                }.invokeOnCompletion {
                    coroutineScope.launch {
                        toastHostState.showToast(
                            message = context.getString(strings.plugin_created_successfully),
                            icon = Icons.Rounded.Check
                        )
                    }
                }
            },
            onDismiss = { showNewPluginDialog = false }
        )
    }
}