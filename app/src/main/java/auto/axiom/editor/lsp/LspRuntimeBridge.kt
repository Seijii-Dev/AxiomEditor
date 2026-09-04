package auto.axiom.editor.lsp

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

/** Acode-style runtime bridge: use an app-local process when available, otherwise ask Termux to start one. */
object LspRuntimeBridge {
    data class ServerSpec(
        val language: SupportedLanguage,
        val command: List<String>,
        val workingDirectory: File? = null
    )

    fun startLocal(spec: ServerSpec): ProcessLspClient? = runCatching {
        val process = ProcessBuilder(spec.command)
            .apply { spec.workingDirectory?.let { directory(it) } }
            .redirectErrorStream(false)
            .start()
        ProcessLspClient(process, spec.language)
    }.getOrNull()

    /** Requires Termux:API. The command is started in Termux and should expose an LSP TCP bridge. */
    fun startInTermux(context: Context, spec: ServerSpec, tcpPort: Int): Boolean = runCatching {
        val command = "${spec.command.joinToString(" ") { shellQuote(it) }} --stdio"
        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setData(Uri.parse("content://com.termux.files/usr/bin/"))
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_WORKDIR", spec.workingDirectory?.absolutePath ?: "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_STDIN", "")
            putExtra("axiom.lsp.port", tcpPort)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}

/** JSON-RPC/LSP client backed by a language-server process' stdin/stdout. */
class ProcessLspClient(
    private val process: Process,
    private val language: SupportedLanguage
) : AutoCloseable {
    private val ids = AtomicInteger(1)
    private val input = BufferedInputStream(process.inputStream)
    private val output = BufferedOutputStream(process.outputStream)
    private var initialized = false
    private var version = 0

    @Synchronized
    fun completion(uri: String, text: String, line: Int, character: Int): List<JSONObject> {
        if (!initialized) {
            request("initialize", JSONObject().apply {
                put("processId", android.os.Process.myPid())
                put("rootUri", JSONObject.NULL)
                put("capabilities", JSONObject())
            })
            notify("initialized", JSONObject())
            initialized = true
        }
        version++
        notify("textDocument/didOpen", JSONObject().apply {
            put("textDocument", JSONObject().apply {
                put("uri", uri)
                put("languageId", language.id)
                put("version", version)
                put("text", text)
            })
        })
        val result = request("textDocument/completion", JSONObject().apply {
            put("textDocument", JSONObject().put("uri", uri))
            put("position", JSONObject().put("line", line).put("character", character))
        })
        val values = when (result) {
            is JSONObject -> result.optJSONArray("items") ?: JSONArray()
            is JSONArray -> result
            else -> JSONArray()
        }
        return List(values.length()) { values.optJSONObject(it) ?: JSONObject() }
    }

    private fun notify(method: String, params: JSONObject) = write(JSONObject().apply {
        put("jsonrpc", "2.0"); put("method", method); put("params", params)
    })

    private fun request(method: String, params: JSONObject): Any? {
        val id = ids.getAndIncrement()
        write(JSONObject().apply {
            put("jsonrpc", "2.0"); put("id", id); put("method", method); put("params", params)
        })
        while (true) {
            val response = read() ?: return null
            if (response.optInt("id", -1) == id) return response.opt("result")
        }
    }

    private fun write(message: JSONObject) {
        val body = message.toString().toByteArray(StandardCharsets.UTF_8)
        output.write("Content-Length: ${body.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
        output.write(body); output.flush()
    }

    private fun read(): JSONObject? {
        var length = -1
        while (true) {
            val line = readLine() ?: return null
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon > 0 && line.substring(0, colon).equals("Content-Length", true)) {
                length = line.substring(colon + 1).trim().toIntOrNull() ?: -1
            }
        }
        if (length < 0) return null
        val body = ByteArray(length); var offset = 0
        while (offset < length) {
            val count = input.read(body, offset, length - offset)
            if (count < 0) return null
            offset += count
        }
        return JSONObject(String(body, StandardCharsets.UTF_8))
    }

    private fun readLine(): String? {
        val bytes = ArrayList<Byte>()
        while (true) {
            val value = input.read()
            if (value < 0) return null
            if (value == '\n'.code) {
                if (bytes.lastOrNull() == '\r'.code.toByte()) bytes.removeAt(bytes.lastIndex)
                return String(bytes.toByteArray(), StandardCharsets.US_ASCII)
            }
            bytes += value.toByte()
        }
    }

    override fun close() = process.destroy()
}
