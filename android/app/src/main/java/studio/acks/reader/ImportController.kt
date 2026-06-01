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

        val text = dest.readText()
        val fmt  = when (ext) {
            "md", "markdown" -> Format.MARKDOWN
            "html", "htm"    -> Format.HTML
            else             -> sniff(text)
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
            lifecycle      = if (fmt == Format.UNSUPPORTED) Lifecycle.UNSUPPORTED else Lifecycle.RENDERING
        )
        ImportResult(state, dest.length())
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
