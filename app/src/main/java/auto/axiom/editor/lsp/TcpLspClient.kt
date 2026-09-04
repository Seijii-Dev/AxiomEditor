package auto.axiom.editor.lsp

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

/** Minimal Content-Length framed LSP client for a local or remote language server. */
class TcpLspClient(
    private val host: String,
    private val port: Int,
    private val language: SupportedLanguage
) : AutoCloseable {
    private val ids = AtomicInteger(1)
    private val socket = Socket(host, port)
    private val input = BufferedInputStream(socket.getInputStream())
    private val output = BufferedOutputStream(socket.getOutputStream())
    private var initialized = false

    @Synchronized
    fun initialize() {
        if (initialized) return
        request("initialize", JSONObject().apply {
            put("processId", JSONObject.NULL)
            put("rootUri", JSONObject.NULL)
            put("capabilities", JSONObject())
            put("workspaceFolders", JSONArray())
        })
        notify("initialized", JSONObject())
        initialized = true
    }

    @Synchronized
    fun didOpen(uri: String, text: String) {
        initialize()
        notify("textDocument/didOpen", JSONObject().apply {
            put("textDocument", JSONObject().apply {
                put("uri", uri)
                put("languageId", language.id)
                put("version", 1)
                put("text", text)
            })
        })
    }

    @Synchronized
    fun completion(uri: String, text: String, line: Int, character: Int): List<JSONObject> {
        didOpen(uri, text)
        val result = request("textDocument/completion", JSONObject().apply {
            put("textDocument", JSONObject().put("uri", uri))
            put("position", JSONObject().put("line", line).put("character", character))
            put("context", JSONObject().put("triggerKind", 1))
        })
        val values = when {
            result?.optJSONArray("items") != null -> result.optJSONArray("items")!!
            result is JSONArray -> result
            else -> JSONArray()
        }
        return List(values.length()) { values.optJSONObject(it) ?: JSONObject() }
    }

    private fun notify(method: String, params: JSONObject) {
        write(JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
        })
    }

    private fun request(method: String, params: JSONObject): Any? {
        val id = ids.getAndIncrement()
        write(JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", id)
            put("method", method)
            put("params", params)
        })
        while (true) {
            val message = read() ?: return null
            if (message.optInt("id", -1) == id) {
                return message.opt("result")
            }
        }
    }

    private fun write(message: JSONObject) {
        val bytes = message.toString().toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        output.write(header)
        output.write(bytes)
        output.flush()
    }

    private fun read(): JSONObject? {
        var contentLength = -1
        while (true) {
            val line = readLine() ?: return null
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0 && line.substring(0, separator).equals("Content-Length", true)) {
                contentLength = line.substring(separator + 1).trim().toIntOrNull() ?: -1
            }
        }
        if (contentLength < 0) return null
        val bytes = ByteArray(contentLength)
        var offset = 0
        while (offset < contentLength) {
            val count = input.read(bytes, offset, contentLength - offset)
            if (count < 0) return null
            offset += count
        }
        return JSONObject(String(bytes, StandardCharsets.UTF_8))
    }

    private fun readLine(): String? {
        val bytes = ArrayList<Byte>()
        while (true) {
            val next = input.read()
            if (next < 0) return if (bytes.isEmpty()) null else String(bytes.toByteArray(), StandardCharsets.US_ASCII)
            if (next == '\n'.code) {
                if (bytes.isNotEmpty() && bytes.last() == '\r'.code.toByte()) bytes.removeAt(bytes.lastIndex)
                return String(bytes.toByteArray(), StandardCharsets.US_ASCII)
            }
            bytes += next.toByte()
        }
    }

    override fun close() {
        runCatching { socket.close() }
    }
}
