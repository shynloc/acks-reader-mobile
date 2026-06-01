package studio.acks.reader

import android.content.Context
import androidx.room.*

@Dao
interface DocDao {
    @Query("SELECT * FROM documents ORDER BY importedAt DESC")
    suspend fun getAll(): List<DocRecord>

    // Return Long (rowId) and Int (rows affected) to avoid KSP "unexpected jvm signature V" bug
    @Upsert
    suspend fun upsert(doc: DocRecord): Long

    @Delete
    suspend fun delete(doc: DocRecord): Int

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DocRecord?
}

@Database(entities = [DocRecord::class], version = 2, exportSchema = false)
abstract class DocDatabase : RoomDatabase() {
    abstract fun docDao(): DocDao

    companion object {
        @Volatile private var instance: DocDatabase? = null

        fun getInstance(ctx: Context): DocDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext, DocDatabase::class.java, "acks-reader.db"
            )
            .addMigrations(MIGRATION_1_2)
            .build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN fontScale REAL NOT NULL DEFAULT 1.0")
            }
        }
    }
}
