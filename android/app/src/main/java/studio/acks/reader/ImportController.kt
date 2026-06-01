package studio.acks.reader

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ImportResult(val docState: DocState, val sizeBytes: Long)

object ImportController {

    private const val MAX_SIZE_BYTES = 5 * 1024 * 1024L  // 5 MB

    suspend fun import(ctx: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val name = queryDisplayName(ctx, uri) ?: "document"
        val id   = UUID.randomUUID().toString().take(8)
        val dir  = File(ctx.filesDir, "documents/$id").apply { mkdirs() }
        val ext  = name.substringAfterLast('.', "").lowercase()
        val dest = File(dir, "source.${ext.ifEmpty { "txt" }}")

        ctx.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open file stream for $uri" }
            dest.outputStream().use { input.copyTo(it) }
        }

        // 3-A1: Empty file
        if (dest.length() == 0L) {
            dir.deleteRecursively()
            throw IllegalArgumentException("文件为空，请检查后重试。")
        }

        val sizeBytes = dest.length()
        val fmt: Format
        val text: String
        val lifecycle: Lifecycle

        if (sizeBytes > MAX_SIZE_BYTES) {
            // 3-A2: Large file — render only the first 5 MB, mark as LARGE for UI warning
            fmt  = when (ext) {
                "md", "markdown" -> Format.MARKDOWN
                "html", "htm"    -> Format.HTML
                else             -> Format.MARKDOWN
            }
            text      = readTruncated(dest, MAX_SIZE_BYTES)
            lifecycle = Lifecycle.LARGE
        } else {
            // 3-A3: Attempt to read; fall back to replacement-char decoding on garbage
            text = try {
                dest.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                dest.readText(Charsets.ISO_8859_1)
            }

            if (text.isBlank()) {
                dir.deleteRecursively()
                throw IllegalArgumentException("文件内容无法识别，可能已损坏。")
            }

            fmt = when (ext) {
                "md", "markdown" -> Format.MARKDOWN
                "html", "htm"    -> Format.HTML
                else             -> sniff(text)
            }
            lifecycle = if (fmt == Format.UNSUPPORTED) Lifecycle.UNSUPPORTED else Lifecycle.RENDERING
        }

        val state = DocState(
            id             = id,
            title          = name,
            format         = fmt,
            sandboxPath    = dest.absolutePath,
            markdownSource = text,
            themeId        = "aireport",
            mode           = "dark",
            htmlMode       = "safe",
            lifecycle      = lifecycle
        )
        ImportResult(state, sizeBytes)
    }

    private fun readTruncated(file: File, maxBytes: Long): String {
        val buf = ByteArray(maxBytes.toInt())
        file.inputStream().use { n -> n.read(buf) }
        return String(buf, Charsets.UTF_8)
    }

    private fun sniff(text: String): Format {
        val head = text.take(512).trimStart().lowercase()
        return when {
            head.startsWith("<!doctype html") || head.startsWith("<html") -> Format.HTML
            text.isNotBlank() -> Format.MARKDOWN
            else -> Format.UNSUPPORTED
        }
    }

    private fun queryDisplayName(ctx: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        var name: String? = null
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c: Cursor ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = c.getString(idx)
            }
        }
        return name
    }
}
