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

package auto.axiom.editor.activities

import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.itsvks.monaco.MonacoEditor
import auto.axiom.editor.APP_EXTERNAL_DIR
import auto.axiom.editor.BuildConfig
import auto.axiom.editor.activities.Editor.LocalEditorDrawerNavController
import auto.axiom.editor.activities.Editor.LocalEditorDrawerState
import auto.axiom.editor.activities.Editor.LocalEditorSnackbarHostState
import auto.axiom.editor.activities.base.BaseComposeActivity
import auto.axiom.editor.activities.base.ObserveLifecycleEvents
import auto.axiom.editor.app.DoNothing
import auto.axiom.editor.app.MONACO_EDITOR_ARCHIVE
import auto.axiom.editor.app.noLocalProvidedFor
import auto.axiom.editor.app.rootView
import auto.axiom.editor.app.strings
import auto.axiom.editor.core.EventManager
import auto.axiom.editor.core.settings.Settings.Editor.rememberShowInputMethodPickerAtStart
import auto.axiom.editor.core.settings.Settings.General.rememberEnableGestureInDrawer
import auto.axiom.editor.editor.addBlockComment
import auto.axiom.editor.editor.addSingleComment
import auto.axiom.editor.editor.events.OnContentChangeEvent
import auto.axiom.editor.editor.events.OnKeyBindingEvent
import auto.axiom.editor.events.OnCreateFileEvent
import auto.axiom.editor.events.OnCreateFolderEvent
import auto.axiom.editor.events.OnOpenFolderEvent
import auto.axiom.editor.events.OnRefreshFolderEvent
import auto.axiom.editor.extensions.open
import auto.axiom.editor.file.File
import auto.axiom.editor.file.extension
import auto.axiom.editor.file.wrapFile
import auto.axiom.editor.github.auth.Api
import auto.axiom.editor.github.auth.UserInfo
import auto.axiom.editor.keyboard.CommandPaletteManager
import auto.axiom.editor.keyboard.model.Command.Companion.newCommand
import auto.axiom.editor.plugins.PluginLoader
import auto.axiom.editor.plugins.impl.PluginContextImpl
import auto.axiom.editor.ui.components.ai.GenerateContentDialog
import auto.axiom.editor.ui.screens.editor.EditorScreen
import auto.axiom.editor.ui.screens.editor.EditorViewModel
import auto.axiom.editor.ui.screens.editor.components.EditorDrawerSheet
import auto.axiom.editor.ui.screens.editor.components.EditorTopBar
import auto.axiom.editor.ui.screens.editor.components.view.CodeEditorView
import auto.axiom.editor.ui.screens.file.FileExplorerViewModel
import auto.axiom.editor.utils.createNomediaFile
import com.axiomeditor.plugins.event.FileCreateEvent
import kiwi.orbit.compose.ui.controls.Scaffold
import kiwi.orbit.compose.ui.controls.ToastHostState
import kiwi.orbit.compose.ui.controls.rememberToastHostState
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

object Editor {
    val LocalEditorDrawerState = compositionLocalOf<DrawerState> {
        noLocalProvidedFor("LocalEditorDrawerState")
    }

    val LocalCommandPaletteManager = staticCompositionLocalOf {
        CommandPaletteManager.instance
    }

    val LocalEditorDrawerNavController = compositionLocalOf<NavHostController> {
        noLocalProvidedFor("LocalEditorDrawerNavController")
    }

    val LocalEditorSnackbarHostState = compositionLocalOf<ToastHostState> {
        noLocalProvidedFor("LocalEditorSnackbarHostState")
    }
}

class EditorActivity : BaseComposeActivity() {
    companion object {
        private const val TAG = "EditorActivity"

        val LAST_OPENED_FILES_JSON_PATH =
            "${PathUtils.getExternalAppFilesPath()}/settings/lastOpenedFile.json"
    }

    private val editorViewModel: EditorViewModel by viewModels()
    val fileExplorerViewModel: FileExplorerViewModel by viewModels()

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onContentChangeEvent(event: OnContentChangeEvent) {
        Log.d("EditorActivity", "Content change event received: ${event.file?.name}")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onKeyBindingEvent(event: OnKeyBindingEvent) {
        editorViewModel.setCanEditorHandleCurrentKeyBinding(event.canEditorHandle)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshFolderEvent(event: OnRefreshFolderEvent) {
        fileExplorerViewModel.loadFileTree(event.openedFolder)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCreateFileEvent(event: OnCreateFileEvent) {
        fileExplorerViewModel.loadFileTree(event.openedFolder)
        EventManager.instance.postEvent(FileCreateEvent(java.io.File(event.file.absolutePath)))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCreateFolderEvent(event: OnCreateFolderEvent) {
        fileExplorerViewModel.loadFileTree(event.openedFolder)
        EventManager.instance.postEvent(FileCreateEvent(java.io.File(event.file.absolutePath)))
    }

    private fun onCreate() {
        initCommands()
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onFolderOpened(event: OnOpenFolderEvent) {
        currentEditor ?: return
    }

    @Composable
    override fun MainScreen() {
        ProvideEditorCompositionLocals {
            val fileExplorerViewModel: FileExplorerViewModel = viewModel()
            val editorViewModel: EditorViewModel = viewModel()

            DoLifecycleThings(editorViewModel)

            val showInputMethodPickerAtStart by rememberShowInputMethodPickerAtStart()
            LifecycleStartEffect(showInputMethodPickerAtStart) {
                if (showInputMethodPickerAtStart) {
                    getSystemService(InputMethodManager::class.java).showInputMethodPicker()
                }

                onStopOrDispose { }
            }

            val drawerState = LocalEditorDrawerState.current

            val enableGestureInDrawer by rememberEnableGestureInDrawer()

            ModalNavigationDrawer(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                drawerState = drawerState,
                gesturesEnabled = if (enableGestureInDrawer) drawerState.isOpen else false,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerState = drawerState,
                        modifier = Modifier.fillMaxWidth(fraction = 0.85f),
                        drawerContainerColor = MaterialTheme.colorScheme.background,
                        drawerContentColor = contentColorFor(MaterialTheme.colorScheme.background)
                    ) {
                        EditorDrawerSheet(
                            fileExplorerViewModel = fileExplorerViewModel,
                            editorViewModel = editorViewModel
                        )
                    }
                }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        EditorTopBar(
                            editorViewModel = editorViewModel
                        )
                    },
                    backgroundColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    toastHostState = LocalEditorSnackbarHostState.current
                ) { innerPadding ->
                    EditorScreen(
                        viewModel = editorViewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    private fun ProvideEditorCompositionLocals(content: @Composable () -> Unit) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val drawerNavController = rememberNavController()
        val snackbarHostState = rememberToastHostState()

        CompositionLocalProvider(
            LocalEditorDrawerState provides drawerState,
            LocalEditorDrawerNavController provides drawerNavController,
            LocalEditorSnackbarHostState provides snackbarHostState,
            content = content
        )
    }

    private fun clearCache(): Boolean {
        return FileUtils.deleteAllInDir(cacheDir).also {
            Log.i(TAG, "Cache cleared 😊")
        }
    }

    @Composable
    private fun DoLifecycleThings(editorViewModel: EditorViewModel) {
        val toastHostState = LocalEditorSnackbarHostState.current
        val compositionContext = rememberCompositionContext()

        ObserveLifecycleEvents { event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    onCreate()
                    EventBus.getDefault().register(this@EditorActivity)
                    createNomediaFile(APP_EXTERNAL_DIR)

                    lifecycleScope.launch {
                        //fileExplorerViewModel.openFolder(PathUtils.getInternalAppFilesPath().toFile().wrapFile())
                        runCatching {
                            PluginLoader.loadPlugins(this@EditorActivity)
                        }.onSuccess { plugins ->
                            val pluginContext =
                                PluginContextImpl(
                                    this@EditorActivity,
                                    editorViewModel,
                                    compositionContext
                                )
                            plugins.forEach { pluginEntry ->
                                if (pluginEntry.first.enabled) {
                                    runCatching {
                                        pluginEntry.second.onPluginLoaded(pluginContext)
                                    }.onFailure {
                                        toastHostState.showToast(
                                            it.message ?: "Error loading plugin"
                                        )
                                    }
                                }
                            }
                        }.onFailure {
                            it.printStackTrace()
                            toastHostState.showToast(it.message ?: "Error loading plugin")
                        }
                    }

                    val externalFileUri = intent.data
                    if (externalFileUri != null &&
                        !externalFileUri.toString().startsWith(BuildConfig.OAUTH_REDIRECT_URL)
                    ) {
                        editorViewModel.addFile(UriUtils.uri2File(externalFileUri).wrapFile())
                    }
                }

                Lifecycle.Event.ON_PAUSE -> {
                    editorViewModel.rememberLastFiles()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    editorViewModel.rememberLastFiles()
                    EventBus.getDefault().unregister(this@EditorActivity)
                    clearCache()
                }

                Lifecycle.Event.ON_START -> DoNothing
                Lifecycle.Event.ON_RESUME -> {
                    val code = intent?.data?.getQueryParameter("code")

                    if (!code.isNullOrEmpty()) {
                        runCatching {
                            lifecycleScope.launch {
                                Api.exchangeCodeForToken(
                                    code = code,
                                    onSuccess = { accessToken ->
                                        Api.getUser(
                                            token = accessToken.accessToken,
                                            onSuccess = { user ->
                                                runCatching {
                                                    Api.saveUser(UserInfo(user, accessToken))
                                                    toastHostState.showToast("Logged in as ${user.username}")
                                                }.onFailure {
                                                    toastHostState.showToast("Error: ${it.message}")
                                                }
                                            },
                                            onFailure = {
                                                it.printStackTrace()
                                                ToastUtils.showShort("Error: ${it.message}")
                                            }
                                        )
                                    },
                                    onFailure = {
                                        it.printStackTrace()
                                        ToastUtils.showShort("Error: ${it.message}")
                                    }
                                )
                            }
                        }.onFailure {
                            it.printStackTrace()
                            lifecycleScope.launch {
                                toastHostState.showToast(it.message ?: "")
                            }
                        }
                    }
                }

                Lifecycle.Event.ON_STOP -> DoNothing
                Lifecycle.Event.ON_ANY -> DoNothing
            }
        }
    }

    val currentEditor get() = editorViewModel.getSelectedEditor()
    val selectedFileIndex get() = editorViewModel.uiState.value.selectedFileIndex
    val canEditorHandleCurrentKeyBinding get() = editorViewModel.canEditorHandleCurrentKeyBinding.value

    val editorForFile: (File) -> CodeEditorView? =
        { file -> editorViewModel.getEditorForFile(file) }

    @JvmField
    val openFile: (File) -> Unit = { file -> editorViewModel.addFile(file) }

    @JvmField
    val closeFile: (Int) -> Unit = { index -> editorViewModel.closeFile(index) }

    @JvmField
    val closeAll = { editorViewModel.closeAll() }

    @JvmField
    val closeOthers: (Int) -> Unit = { index -> editorViewModel.closeOthers(index) }

    @JvmField
    val saveAll = { lifecycleScope.launch { editorViewModel.saveAll() } }

    val openedFolder = { fileExplorerViewModel.openedFolder.value }

    @JvmOverloads
    fun saveFile(codeEditorView: CodeEditorView? = null) {
        lifecycleScope.launch {
            editorViewModel.saveFile(codeEditorView)
        }
    }

    private fun initCommands() {
        //CommandPaletteManager.instance.clear()
        CommandPaletteManager.instance.addCommand(
            newCommand("Paste", "Ctrl+V") {
                if (currentEditor !is CodeEditorView) return@newCommand

                val editor = (currentEditor as CodeEditorView).editor
                val canEditorHandle = canEditorHandleCurrentKeyBinding

                if (!canEditorHandle) {
                    editor.pasteText()
                }
            },
            newCommand("Copy", "Ctrl+C") {
                if (currentEditor !is CodeEditorView) return@newCommand

                val editor = (currentEditor as CodeEditorView).editor
                val canEditorHandle = canEditorHandleCurrentKeyBinding

                if (!canEditorHandle && editor.cursor?.isSelected == true) {
                    editor.copyText()
                }
            },
            newCommand("Cut", "Ctrl+X") {
                if (currentEditor !is CodeEditorView) return@newCommand

                val editor = (currentEditor as CodeEditorView).editor
                val canEditorHandle = canEditorHandleCurrentKeyBinding

                if (!canEditorHandle && editor.cursor?.isSelected == true) {
                    editor.cutText()
                }
            },
            newCommand("Copy Path of Active File", "Alt+Shift+C") {
                if (currentEditor !is CodeEditorView) return@newCommand

                val editor = (currentEditor as CodeEditorView).editor
                val file = editor.file
                ClipboardUtils.copyText(file?.absolutePath)
                ToastUtils.showShort("Copied path of active file: ${file?.name}")
            },
            newCommand("Copy File Name", "Alt+Shift+F") {
                if (currentEditor !is CodeEditorView) return@newCommand

                val editor = (currentEditor as CodeEditorView).editor
                val file = editor.file
                ClipboardUtils.copyText(file?.name)
                ToastUtils.showShort("Copied file name: ${file?.name}")
            },
            newCommand("Undo", "Ctrl+Z") {
                if (currentEditor !is CodeEditorView) return@newCommand

                val editor = (currentEditor as CodeEditorView).editor
                val canEditorHandle = canEditorHandleCurrentKeyBinding

                if (!canEditorHandle) {
                    editor.undo()
                }
            },
            newCommand("Redo", "Ctrl+Y") {
                if (currentEditor !is CodeEditorView) return@newCommand

                val editor = (currentEditor as CodeEditorView).editor
                val canEditorHandle = canEditorHandleCurrentKeyBinding

                if (!canEditorHandle) {
                    editor.redo()
                }
            },
            newCommand("Toggle Line Comment", "Ctrl+/") {
                if (currentEditor !is CodeEditorView) return@newCommand

                val editor = (currentEditor as CodeEditorView).editor
                val canEditorHandle = canEditorHandleCurrentKeyBinding

                val commentRule = editor.commentRule
                if (!canEditorHandle) {
                    if (!editor.cursor.isSelected) {
                        addSingleComment(commentRule, editor.text)
                    } else {
                        addBlockComment(commentRule, editor.text)
                    }
                }
            },
            newCommand("Settings", "Ctrl+,") {
                open(SettingsActivity::class.java)
            },
            newCommand("Manage Plugins", "Ctrl+P") {
                open(PluginsActivity::class.java)
            },
            newCommand(getString(strings.editor_action_import_components), "Ctrl+Shift+I") {
                if (currentEditor !is CodeEditorView) return@newCommand

                val editor = (currentEditor as CodeEditorView).editor
                editor.onImportComponentListener?.onImport(editor.text)
            },
            newCommand(getString(strings.generate_code), "Alt+I") { compositionContext ->
                currentEditor ?: return@newCommand
                val editor = currentEditor!!

                val composeView = ComposeView(this@EditorActivity).apply {
                    setParentCompositionContext(compositionContext)
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setContent {
                        val uiState by editorViewModel.uiState.collectAsStateWithLifecycle()
                        val currentFile = uiState.selectedFile?.file

                        GenerateContentDialog(
                            editor = editor,
                            fileExtension = currentFile?.extension
                        )
                    }
                }

                rootView().addView(composeView)
            },
            newCommand("Download monaco", null) {
                MonacoEditor.downloadMonaco(this@EditorActivity, MONACO_EDITOR_ARCHIVE)
            }
        )
    }
}
