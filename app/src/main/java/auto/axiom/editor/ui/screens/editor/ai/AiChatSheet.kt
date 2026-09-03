package auto.axiom.editor.ui.screens.editor.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import auto.axiom.editor.core.ai.ChatMessage
import auto.axiom.editor.core.ai.OpenRouter
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class ChatBubble(val role: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val messages = remember { mutableStateListOf<ChatBubble>() }
    var prompt by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamJob by remember { mutableStateOf<Job?>(null) }

    fun send(text: String = prompt) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || isStreaming) return
        prompt = ""
        messages.add(ChatBubble("user", trimmed))
        messages.add(ChatBubble("assistant", ""))
        isStreaming = true
        streamJob = scope.launch {
            val history = messages.dropLast(1).map { ChatMessage(it.role, it.text) }
            val result = OpenRouter.stream(context, history) { delta ->
                val index = messages.lastIndex
                messages[index] = messages[index].copy(text = messages[index].text + delta)
            }
            result.onFailure { error ->
                val index = messages.lastIndex
                messages[index] = messages[index].copy(text = "Unable to complete the request.\n\n${error.message}")
            }
            isStreaming = false
            streamJob = null
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                SmallTopAppBar(
                    title = {
                        Column {
                            Text("AI Chat")
                            Text("OpenRouter · ready", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            messages.clear()
                            prompt = ""
                        }) { Icon(Icons.Rounded.Add, contentDescription = "New chat") }
                    },
                    actions = {
                        TextButton(onClick = onDismissRequest) { Text("Done") }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (messages.isEmpty()) {
                    EmptyChat(onPrompt = ::send, modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages) { bubble ->
                            ChatBubbleView(bubble, onCopy = { clipboard.setText(AnnotatedString(bubble.text)) })
                        }
                        if (isStreaming) item { TypingIndicator() }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about your code…") },
                        maxLines = 5,
                        enabled = !isStreaming,
                        shape = RoundedCornerShape(22.dp)
                    )
                    IconButton(
                        onClick = { if (isStreaming) streamJob?.cancel() else send() },
                        modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            if (isStreaming) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                            contentDescription = if (isStreaming) "Stop response" else "Send message",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChat(onPrompt: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Build with an AI pair programmer", style = MaterialTheme.typography.headlineSmall)
        Text("Ask questions, refactor code, or generate a solution.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { onPrompt("Explain the current code") }, label = { Text("Explain code") })
            AssistChip(onClick = { onPrompt("Find bugs and suggest fixes") }, label = { Text("Find bugs") })
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { onPrompt("Refactor this code for readability") }, label = { Text("Refactor") })
            AssistChip(onClick = { onPrompt("Write tests for this code") }, label = { Text("Write tests") })
        }
    }
}

@Composable
private fun ChatBubbleView(bubble: ChatBubble, onCopy: () -> Unit) {
    val isUser = bubble.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.86f else 0.96f)
                .background(
                    if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Text(if (isUser) "You" else "Axiom AI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (!isUser && bubble.text.isNotBlank()) {
                MarkdownText(
                    markdown = bubble.text,
                    modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
                    isTextSelectable = true
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy response", modifier = Modifier.size(16.dp))
                }
            } else {
                Text(bubble.text, modifier = Modifier.padding(top = 6.dp), fontFamily = if (bubble.text.startsWith("```")) FontFamily.Monospace else FontFamily.Default)
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        Text("  AI is typing…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
