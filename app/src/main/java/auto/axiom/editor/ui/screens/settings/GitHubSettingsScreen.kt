package auto.axiom.editor.ui.screens.settings

import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import auto.axiom.editor.github.Repository
import auto.axiom.editor.github.User
import auto.axiom.editor.github.FileCreateRequest
import auto.axiom.editor.github.FileUpdateRequest
import auto.axiom.editor.github.GitHubService
import auto.axiom.editor.github.auth.Api
import auto.axiom.editor.utils.awaitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Composable
fun GitHubSettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val userInfo = remember { Api.getUserInfo() }
    val user = userInfo?.user
    val api = remember(userInfo) { userInfo?.let { GitHubService.createGitHubApiService(it.accessToken.accessToken) } }
    var repositories by remember { mutableStateOf<List<Repository>>(emptyList()) }
    var selectedRepository by remember { mutableStateOf<Repository?>(null) }
    var branch by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Update from Axiom Editor") }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    fun loadRepositories() {
        val githubApi = api ?: return
        scope.launch {
            loading = true
            status = null
            withContext(Dispatchers.IO) { githubApi.getRepositories().awaitResult() }
                .onSuccess { repos ->
                    repositories = repos
                    if (selectedRepository == null) {
                        selectedRepository = repos.firstOrNull()
                        branch = selectedRepository?.defaultBranch.orEmpty()
                    }
                    status = if (repos.isEmpty()) "No repositories were returned for this account." else "${repos.size} repositories available"
                }
                .onFailure { status = it.userFacingMessage() }
            loading = false
        }
    }

    LaunchedEffect(api) { loadRepositories() }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                IconButton(onClick = onNavigateUp) { Icon(Icons.Rounded.ArrowBack, "Back") }
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("GitHub workspace", style = MaterialTheme.typography.headlineSmall)
                    Text("Connected as @${user?.username.orEmpty()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row {
                IconButton(onClick = { loadRepositories() }, enabled = !loading) { Icon(Icons.Rounded.Refresh, "Refresh repositories") }
                IconButton(onClick = { user?.let(Api.removeUserFromDevice); onLoggedOut() }) { Icon(Icons.Rounded.Logout, "Disconnect GitHub") }
            }
        }

        if (user == null || api == null) {
            Text("Connect GitHub from the settings page before using repository actions.")
        } else {
            Text("Repositories", style = MaterialTheme.typography.titleMedium)
            if (repositories.isEmpty() && loading) CircularProgressIndicator()
            repositories.forEach { repository ->
                Card(
                    onClick = { selectedRepository = repository; branch = repository.defaultBranch },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRepository?.id == repository.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(repository.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(if (repository.isPrivate) "Private · ${repository.defaultBranch}" else "Public · ${repository.defaultBranch}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Push a file", style = MaterialTheme.typography.titleMedium)
            Text("Creates a new path or updates it when it already exists. Content is sent directly to GitHub over HTTPS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = selectedRepository?.name.orEmpty(), onValueChange = {}, readOnly = true, label = { Text("Repository") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = branch, onValueChange = { branch = it }, label = { Text("Branch") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = path, onValueChange = { path = it }, label = { Text("File path, e.g. app/src/main/README.md") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Commit message") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("File content") }, modifier = Modifier.fillMaxWidth().height(180.dp), minLines = 6, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
            Button(
                onClick = {
                    val repository = selectedRepository ?: return@Button
                    scope.launch {
                        val githubApi = api ?: return@launch
                        val account = user ?: return@launch
                        loading = true
                        status = null
                        val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                        val result = withContext(Dispatchers.IO) {
                            val existing = githubApi.getFileContent(account.username, repository.name, path.trim('/'), branch.ifBlank { repository.defaultBranch }).awaitResult()
                            when {
                                existing.isSuccess -> githubApi.updateFile(account.username, repository.name, path.trim('/'), FileUpdateRequest(message.ifBlank { "Update from Axiom Editor" }, encoded, existing.getOrThrow().sha, branch.ifBlank { repository.defaultBranch })).awaitResult()
                                existing.exceptionOrNull() is HttpException && (existing.exceptionOrNull() as HttpException).code() == 404 -> githubApi.createFile(account.username, repository.name, path.trim('/'), FileCreateRequest(message.ifBlank { "Create from Axiom Editor" }, encoded, branch.ifBlank { repository.defaultBranch })).awaitResult()
                                else -> Result.failure(existing.exceptionOrNull() ?: IllegalStateException("Could not read existing file"))
                            }
                        }
                        result.onSuccess { status = "Pushed ${path.trim('/')} to ${repository.fullName}" }.onFailure { status = it.userFacingMessage() }
                        loading = false
                    }
                },
                enabled = !loading && selectedRepository != null && branch.isNotBlank() && path.isNotBlank() && content.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Rounded.CloudUpload, null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Pushing…" else "Push file to GitHub")
            }
            status?.let { Text(it, color = if (it.startsWith("Pushed")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            if (loading) CircularProgressIndicator()
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun Throwable.userFacingMessage(): String = when (this) {
    is HttpException -> "GitHub request failed (${code()}). Check repository permissions, branch, and path."
    else -> message ?: "GitHub request failed."
}
