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

package auto.axiom.editor.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import auto.axiom.editor.app.strings
import auto.axiom.editor.extensions.isNotNull
import auto.axiom.editor.extensions.isNull
import auto.axiom.editor.github.User
import auto.axiom.editor.github.auth.Api
import auto.axiom.editor.resources.R
import auto.axiom.editor.ui.navigateSingleTop
import auto.axiom.editor.ui.screens.SettingScreens
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val navController = rememberNavController()

    var user: User? by remember { mutableStateOf(null) }
    LaunchedEffect(key1 = true) {
        user = Api.getUserInfo()?.user
    }

    NavHost(navController, startDestination = SettingScreens.Default) {
        composable<SettingScreens.Default> {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                        Text("Settings", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "Customize your editing environment",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    SettingsCard(
                        Icons.Rounded.Tune,
                        stringResource(strings.pref_configure_general),
                        "App behavior, startup, updates, and system settings."
                    ) { navController.navigateSingleTop(SettingScreens.General) }
                }
                item {
                    SettingsCard(
                        Icons.Rounded.Code,
                        stringResource(strings.pref_configure_editor),
                        "Text editing, keybindings, line numbers, and code intelligence."
                    ) { navController.navigateSingleTop(SettingScreens.Editor) }
                }
                item {
                    SettingsCard(
                        Icons.Rounded.Folder,
                        stringResource(strings.pref_configure_file_explorer),
                        "File management, workspace, opening behavior, and file associations."
                    ) { navController.navigateSingleTop(SettingScreens.File) }
                }
                item {
                    SettingsCard(
                        Icons.Rounded.AccountTree,
                        if (user.isNull()) stringResource(R.string.login_with_github)
                        else stringResource(R.string.logged_in_as, user!!.username, user!!.name ?: ""),
                        if (user.isNotNull()) user!!.email ?: "" else "Repository settings, authentication, and diff behavior."
                    ) {
                        if (user.isNull()) Api.startLogin(uriHandler)
                        else navController.navigateSingleTop(SettingScreens.GitHub)
                    }
                }
            }
        }

        composable<SettingScreens.General> {
            ProvidePreferenceLocals {
                GeneralSettingsScreen(
                    modifier = modifier,
                    onNavigateUp = navController::navigateUp
                )
            }
        }

        composable<SettingScreens.File> {
            ProvidePreferenceLocals {
                FileSettingsScreen(
                    modifier = modifier,
                    onNavigateUp = navController::navigateUp
                )
            }
        }

        composable<SettingScreens.Editor> {
            ProvidePreferenceLocals {
                EditorSettingsScreen(
                    modifier = modifier,
                    onNavigateUp = navController::navigateUp,
                    onNavigateToMonacoEditorSettings = {
                        navController.navigateSingleTop(
                            SettingScreens.MonacoEditor
                        )
                    }
                )
            }
        }

        composable<SettingScreens.MonacoEditor> {
            ProvidePreferenceLocals {
                MonacoEditorSettingsScreen(
                    modifier = modifier,
                    onNavigateUp = { navController.navigateSingleTop(SettingScreens.Editor) }
                )
            }
        }

        composable<SettingScreens.GitHub> {
            GitHubSettingsScreen(
                modifier = modifier,
                onNavigateUp = navController::navigateUp,
                onLoggedOut = {
                    user = null
                    navController.navigateSingleTop(SettingScreens.Default)
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

object PreferenceShape {
    val Top = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 4.dp,
        bottomEnd = 4.dp
    )

    val Middle = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 4.dp,
        bottomStart = 4.dp,
        bottomEnd = 4.dp
    )

    val Bottom = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 4.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )

    val Alone = RoundedCornerShape(24.dp)
}
