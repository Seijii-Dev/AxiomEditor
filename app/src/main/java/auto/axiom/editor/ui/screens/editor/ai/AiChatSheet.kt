package auto.axiom.editor.ui.screens.editor.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import auto.axiom.editor.core.ai.ChatMessage
import auto.axiom.editor.core.ai.OpenRouter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// ─── Data ────────────────────────────────────────────────────────────────────

private data class ChatBubble(
    val role: String,
    val text: String,
    val id: Long = System.currentTimeMillis()
)

/** Parsed segment from a markdown response – either plain text or a fenced code block */
private sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class Code(val language: String, val content: String) : MessageSegment()
}

// ─── Markdown parser (lightweight) ───────────────────────────────────────────

private fun parseMessageSegments(raw: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val lines = raw.lines()
    var i = 0
    val textBuf = StringBuilder()

    fun flushText() {
        val t = textBuf.toString().trimEnd()
        if (t.isNotEmpty()) segments.add(MessageSegment.Text(t))
        textBuf.clear()
    }

    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            flushText()
            val lang = line.trimStart().removePrefix("```").trim()
            val codeBuf = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeBuf.appendLine(lines[i])
                i++
            }
            segments.add(MessageSegment.Code(lang, codeBuf.toString().trimEnd()))
            i++ // skip closing ```
        } else {
            textBuf.appendLine(line)
            i++
        }
    }
    flushText()
    return segments
}

// ─── Main Sheet ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    /** When not null, the sheet starts with a rewrite task pre-loaded */
    rewriteSelectedText: String? = null,
    /** Callback invoked when the user confirms a rewrite suggestion */
    onConfirmRewrite: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val listState = rememberLazyListState()

    val messages = remember { mutableStateListOf<ChatBubble>() }
    var prompt by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamJob by remember { mutableStateOf<Job?>(null) }

    // Rewrite-confirm state
    var pendingRewrite by remember { mutableStateOf<String?>(null) }
    var rewritePrompt by remember { mutableStateOf(rewriteSelectedText?.let { "Rewrite the following code:\n\n```\n$it\n```" } ?: "") }

    val atBottom by remember { derivedStateOf { !listState.canScrollForward } }

    // Auto-scroll on new content
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            try { listState.animateScrollToItem(messages.lastIndex) } catch (_: Exception) {}
        }
    }

    // If a rewrite was requested, send it automatically
    LaunchedEffect(Unit) {
        if (rewriteSelectedText != null && rewritePrompt.isNotEmpty()) {
            val task = rewritePrompt
            rewritePrompt = ""
            sendMessage(
                context = context,
                scope = scope,
                text = task,
                messages = messages,
                isStreamingRef = { isStreaming },
                setStreaming = { isStreaming = it },
                setJob = { streamJob = it },
                onRewriteResult = if (onConfirmRewrite != null) { result -> pendingRewrite = result } else null
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            containerColor = Color.Transparent,
            topBar = {
                AiTopBar(
                    isStreaming = isStreaming,
                    onNewChat = { messages.clear(); prompt = ""; streamJob?.cancel() },
                    onDismiss = onDismissRequest
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ── Confirm-rewrite banner ────────────────────────────────
                AnimatedVisibility(
                    visible = pendingRewrite != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = fadeOut()
                ) {
                    pendingRewrite?.let { suggestion ->
                        RewriteConfirmBanner(
                            suggestion = suggestion,
                            onAccept = {
                                onConfirmRewrite?.invoke(suggestion)
                                pendingRewrite = null
                            },
                            onReject = { pendingRewrite = null }
                        )
                    }
                }

                // ── Message list ──────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    if (messages.isEmpty()) {
                        EmptyChat(
                            hasRewrite = rewriteSelectedText != null,
                            onPrompt = { text ->
                                sendMessage(
                                    context, scope, text, messages,
                                    { isStreaming }, { isStreaming = it }, { streamJob = it },
                                    if (onConfirmRewrite != null) { r -> pendingRewrite = r } else null
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(messages) { index, bubble ->
                                ChatBubbleView(
                                    bubble = bubble,
                                    isLast = index == messages.lastIndex,
                                    isStreaming = isStreaming && index == messages.lastIndex,
                                    onCopy = { clipboard.setText(AnnotatedString(bubble.text)) },
                                    onRetry = if (bubble.role == "assistant" && !isStreaming) {
                                        {
                                            val lastUser = messages.take(index).lastOrNull { it.role == "user" }
                                            if (lastUser != null) {
                                                messages.removeAt(index)
                                                sendMessage(
                                                    context, scope, lastUser.text, messages,
                                                    { isStreaming }, { isStreaming = it }, { streamJob = it },
                                                    if (onConfirmRewrite != null) { r -> pendingRewrite = r } else null
                                                )
                                            }
                                        }
                                    } else null,
                                    onConfirmRewrite = if (onConfirmRewrite != null && bubble.role == "assistant") {
                                        { extracted ->
                                            onConfirmRewrite(extracted)
                                        }
                                    } else null
                                )
                            }
                            if (isStreaming) {
                                item { TypingIndicator(modifier = Modifier.padding(start = 8.dp)) }
                            }
                        }

                        // Scroll-to-bottom FAB
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !atBottom,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    scope.launch {
                                        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Scroll to bottom", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // ── Input row ─────────────────────────────────────────────
                ChatInputRow(
                    prompt = prompt,
                    isStreaming = isStreaming,
                    onPromptChange = { prompt = it },
                    onSend = {
                        val trimmed = prompt.trim()
                        if (trimmed.isNotEmpty()) {
                            prompt = ""
                            sendMessage(
                                context, scope, trimmed, messages,
                                { isStreaming }, { isStreaming = it }, { streamJob = it },
                                if (onConfirmRewrite != null) { r -> pendingRewrite = r } else null
                            )
                        }
                    },
                    onStop = { streamJob?.cancel(); isStreaming = false }
                )
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun sendMessage(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    text: String,
    messages: androidx.compose.runtime.snapshots.SnapshotStateList<ChatBubble>,
    isStreamingRef: () -> Boolean,
    setStreaming: (Boolean) -> Unit,
    setJob: (Job?) -> Unit,
    onRewriteResult: ((String) -> Unit)?
) {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || isStreamingRef()) return
    messages.add(ChatBubble("user", trimmed))
    messages.add(ChatBubble("assistant", ""))
    setStreaming(true)

    val job = scope.launch {
        val history = messages.dropLast(1).map { ChatMessage(it.role, it.text) }
        val result = OpenRouter.stream(context, history) { delta ->
            val idx = messages.lastIndex
            if (idx >= 0) messages[idx] = messages[idx].copy(text = messages[idx].text + delta)
        }
        result.onFailure { err ->
            val idx = messages.lastIndex
            if (idx >= 0) messages[idx] = messages[idx].copy(text = "⚠️ ${err.message ?: "Request failed."}")
        }
        // Extract code for rewrite confirmation if needed
        if (onRewriteResult != null) {
            val lastMsg = messages.lastOrNull()?.text ?: ""
            val extracted = OpenRouter.stripMarkdownFence(lastMsg)
            if (extracted != lastMsg) onRewriteResult(extracted)
        }
        setStreaming(false)
        setJob(null)
    }
    setJob(job)
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiTopBar(isStreaming: Boolean, onNewChat: () -> Unit, onDismiss: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        navigationIcon = {
            IconButton(onClick = onNewChat) {
                Icon(Icons.Rounded.Add, contentDescription = "New chat")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isStreaming) MaterialTheme.colorScheme.primary else Color(0xFF22C55E))
                )
                Column {
                    Text("Axiom AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isStreaming) "Responding…" else "OpenRouter · ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

// ─── Rewrite Confirm Banner ───────────────────────────────────────────────────

@Composable
private fun RewriteConfirmBanner(
    suggestion: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                Text("AI rewrite ready", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Review the suggested rewrite below, then accept or reject.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onAccept,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF22C55E).copy(alpha = 0.18f),
                        contentColor = Color(0xFF16A34A)
                    )
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Accept")
                }
                TextButton(onClick = onReject) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyChat(
    hasRewrite: Boolean,
    onPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasRewrite) "Rewrite in progress…" else "Your AI pair programmer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Ask anything about your code. Get explanations, refactors, bug fixes, and more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        val chips = listOf(
            "Explain this code",
            "Find bugs & suggest fixes",
            "Refactor for readability",
            "Write unit tests",
            "Add documentation",
            "Optimise performance"
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { label ->
                        AssistChip(
                            onClick = { onPrompt(label) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ─── Chat Bubble ─────────────────────────────────────────────────────────────

@Composable
private fun ChatBubbleView(
    bubble: ChatBubble,
    isLast: Boolean,
    isStreaming: Boolean,
    onCopy: () -> Unit,
    onRetry: (() -> Unit)?,
    onConfirmRewrite: ((String) -> Unit)?
) {
    val isUser = bubble.role == "user"

    if (isUser) {
        // User bubble – right-aligned pill
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        bubble.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    } else {
        // Assistant bubble – left-aligned with segments
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text("Axiom AI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }

            if (bubble.text.isBlank() && isStreaming) {
                // Already shown by TypingIndicator
            } else {
                val segments = remember(bubble.text) { parseMessageSegments(bubble.text) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    segments.forEach { seg ->
                        when (seg) {
                            is MessageSegment.Text -> {
                                if (seg.content.isNotBlank()) {
                                    SelectionContainer {
                                        Text(
                                            seg.content.trim(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                            is MessageSegment.Code -> {
                                CodeBlock(
                                    language = seg.language,
                                    code = seg.content,
                                    onCopy = { /* copy just this block */ },
                                    onApplyRewrite = onConfirmRewrite?.let { cb -> { cb(seg.content) } }
                                )
                            }
                        }
                    }
                }

                // Action row
                if (!isStreaming || !isLast) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                    ) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (onRetry != null) {
                            IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Code Block ──────────────────────────────────────────────────────────────

@Composable
private fun CodeBlock(
    language: String,
    code: String,
    onCopy: () -> Unit,
    onApplyRewrite: (() -> Unit)?
) {
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    language.ifBlank { "code" }.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onApplyRewrite != null) {
                        AssistChip(
                            onClick = onApplyRewrite,
                            label = { Text("Apply", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.height(26.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF22C55E).copy(alpha = 0.15f),
                                labelColor = Color(0xFF16A34A),
                                leadingIconContentColor = Color(0xFF16A34A)
                            )
                        )
                    }
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(code)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Code body
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// ─── Typing Indicator ─────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1 by transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
    val dot2 by transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d2")
    val dot3 by transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d3")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        listOf(dot1, dot2, dot3).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ─── Input Row ───────────────────────────────────────────────────────────────

@Composable
private fun ChatInputRow(
    prompt: String,
    isStreaming: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Ask about your code…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            maxLines = 6,
            enabled = !isStreaming,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        val canSend = prompt.trim().isNotEmpty() && !isStreaming
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isStreaming) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary)
                .clickable { if (isStreaming) onStop() else if (canSend) onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isStreaming) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                contentDescription = if (isStreaming) "Stop" else "Send",
                tint = if (isStreaming) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
