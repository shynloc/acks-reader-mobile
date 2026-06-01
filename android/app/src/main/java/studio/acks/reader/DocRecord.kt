package studio.acks.reader

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocRecord(
    @PrimaryKey val id: String,
    val title: String,
    val format: String,          // "markdown" | "html" | "unsupported"
    val sandboxPath: String,     // absolute path to source file in sandbox
    val sizeBytes: Long = 0L,
    val importedAt: Long = System.currentTimeMillis(),
    val lastThemeId: String = "aireport",
    val lastMode: String = "dark",
    val lastViewport: String = "phone",
    val lastCustomWidth: Int = 600,
    val htmlMode: String = "safe",
    val fontScale: Float = 1.0f
)
