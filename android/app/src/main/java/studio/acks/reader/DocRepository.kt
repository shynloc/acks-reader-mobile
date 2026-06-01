package studio.acks.reader

import java.io.File

class DocRepository(private val db: DocDatabase) {

    suspend fun saveDoc(state: DocState, sizeBytes: Long) {
        db.docDao().upsert(
            DocRecord(
                id = state.id,
                title = state.title,
                format = state.format.name.lowercase(),
                sandboxPath = state.sandboxPath,
                sizeBytes = sizeBytes,
                importedAt = System.currentTimeMillis(),
                lastThemeId = state.themeId,
                lastMode = state.mode,
                lastViewport = state.viewport,
                lastCustomWidth = state.customWidth,
                htmlMode = state.htmlMode
            )
        )
    }

    suspend fun updateSettings(state: DocState) {
        val existing = db.docDao().getById(state.id) ?: return
        db.docDao().upsert(
            existing.copy(
                lastThemeId = state.themeId,
                lastMode = state.mode,
                lastViewport = state.viewport,
                lastCustomWidth = state.customWidth,
                htmlMode = state.htmlMode
            )
        )
    }

    suspend fun getAll(): List<DocRecord> = db.docDao().getAll()

    suspend fun delete(record: DocRecord) {
        db.docDao().delete(record)
        try { File(record.sandboxPath).parentFile?.deleteRecursively() } catch (_: Exception) {}
    }
}
