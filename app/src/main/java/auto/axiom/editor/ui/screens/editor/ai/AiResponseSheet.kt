package auto.axiom.editor.ui.screens.editor.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays a single AI response (explain / generate / etc.) in a
 * bottom-sheet, with proper markdown-aware code-block rendering and
 * an optional "Apply to editor" confirm button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiResponseSheet(
    title: String,
    response: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    /** When provided, shows an "Apply" button in code blocks */
    onApplyCode: ((String) -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState()
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    subtitle?.invoke()
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(response)) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy full response", modifier = Modifier.size(20.dp))
                }
            }

            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // ── Parsed segments ─────────────────────────────────────────────
            val segments = remember(response) { parseResponseSegments(response) }
            segments.forEach { seg ->
                when (seg) {
                    is ResponseSegment.PlainText -> {
                        if (seg.content.isNotBlank()) {
                            SelectionContainer {
                                Text(
                                    seg.content.trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                    is ResponseSegment.CodeFence -> {
                        ResponseCodeBlock(
                            language = seg.language,
                            code = seg.content,
                            onApply = onApplyCode?.let { cb -> { cb(seg.content) } }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Footer ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissRequest) { Text("Done") }
            }
        }
    }
}

// ─── Code block ──────────────────────────────────────────────────────────────

@Composable
private fun ResponseCodeBlock(
    language: String,
    code: String,
    onApply: (() -> Unit)?
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    language.ifBlank { "code" }.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onApply != null) {
                        AssistChip(
                            onClick = onApply,
                            label = { Text("Apply to editor", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
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
                        onClick = {
                            clipboard.setText(AnnotatedString(code))
                            copied = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = "Copy code",
                            modifier = Modifier.size(14.dp),
                            tint = if (copied) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
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

// ─── Parser ───────────────────────────────────────────────────────────────────

private sealed class ResponseSegment {
    data class PlainText(val content: String) : ResponseSegment()
    data class CodeFence(val language: String, val content: String) : ResponseSegment()
}

private fun parseResponseSegments(raw: String): List<ResponseSegment> {
    val result = mutableListOf<ResponseSegment>()
    val lines = raw.lines()
    var i = 0
    val buf = StringBuilder()

    fun flush() {
        val t = buf.toString().trimEnd()
        if (t.isNotEmpty()) result.add(ResponseSegment.PlainText(t))
        buf.clear()
    }

    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            flush()
            val lang = line.trimStart().removePrefix("```").trim()
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                code.appendLine(lines[i])
                i++
            }
            result.add(ResponseSegment.CodeFence(lang, code.toString().trimEnd()))
            i++
        } else {
            buf.appendLine(line)
            i++
        }
    }
    flush()
    return result
}
