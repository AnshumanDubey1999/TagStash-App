package com.anshuman.tagstash.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.anshuman.tagstash.data.model.RecycleBinItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class RecycleBinDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "tagstash_recycle_bin.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_RECYCLE_BIN = "recycle_bin"
        const val COLUMN_ID = "id"
        const val COLUMN_ORIGINAL_PATH = "original_path"
        const val COLUMN_TRASHED_PATH = "trashed_path"
        const val COLUMN_FILE_NAME = "file_name"
        const val COLUMN_FILE_SIZE = "file_size"
        const val COLUMN_IS_DIRECTORY = "is_directory"
        const val COLUMN_DELETED_TIMESTAMP = "deleted_timestamp"
        const val COLUMN_EXPIRY_TIMESTAMP = "expiry_timestamp"

        const val RETENTION_DURATION_MS = 3600_000L // 1 hour

        @Volatile
        private var instance: RecycleBinDatabaseHelper? = null

        fun getInstance(context: Context): RecycleBinDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: RecycleBinDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_RECYCLE_BIN (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ORIGINAL_PATH TEXT NOT NULL,
                $COLUMN_TRASHED_PATH TEXT NOT NULL,
                $COLUMN_FILE_NAME TEXT NOT NULL,
                $COLUMN_FILE_SIZE INTEGER NOT NULL,
                $COLUMN_IS_DIRECTORY INTEGER NOT NULL,
                $COLUMN_DELETED_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_EXPIRY_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECYCLE_BIN")
        onCreate(db)
    }

    suspend fun moveToRecycleBin(files: List<File>, context: Context): List<RecycleBinItem> = withContext(Dispatchers.IO) {
        val trashDir = File(context.filesDir, "recycle_bin").apply {
            if (!exists()) mkdirs()
        }

        val resultList = mutableListOf<RecycleBinItem>()
        val db = writableDatabase
        val currentTime = System.currentTimeMillis()
        val expiryTime = currentTime + RETENTION_DURATION_MS

        for (file in files) {
            if (!file.exists()) continue

            val isDir = file.isDirectory
            val fileSize = if (isDir) computeDirectorySize(file) else file.length()
            val uniqueName = "${currentTime}_${UUID.randomUUID().toString().take(8)}_${file.name}"
            val trashedFile = File(trashDir, uniqueName)

            val moveSuccess = moveFileOrDirectory(file, trashedFile)
            if (moveSuccess) {
                val values = ContentValues().apply {
                    put(COLUMN_ORIGINAL_PATH, file.absolutePath)
                    put(COLUMN_TRASHED_PATH, trashedFile.absolutePath)
                    put(COLUMN_FILE_NAME, file.name)
                    put(COLUMN_FILE_SIZE, fileSize)
                    put(COLUMN_IS_DIRECTORY, if (isDir) 1 else 0)
                    put(COLUMN_DELETED_TIMESTAMP, currentTime)
                    put(COLUMN_EXPIRY_TIMESTAMP, expiryTime)
                }

                val insertedId = db.insert(TABLE_RECYCLE_BIN, null, values)
                if (insertedId != -1L) {
                    resultList.add(
                        RecycleBinItem(
                            id = insertedId,
                            originalPath = file.absolutePath,
                            trashedPath = trashedFile.absolutePath,
                            fileName = file.name,
                            fileSize = fileSize,
                            isDirectory = isDir,
                            deletedTimestamp = currentTime,
                            expiryTimestamp = expiryTime
                        )
                    )
                }
            }
        }

        resultList
    }

    suspend fun getAllItems(): List<RecycleBinItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<RecycleBinItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RECYCLE_BIN,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_DELETED_TIMESTAMP DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val originalPath = it.getString(it.getColumnIndexOrThrow(COLUMN_ORIGINAL_PATH))
                val trashedPath = it.getString(it.getColumnIndexOrThrow(COLUMN_TRASHED_PATH))
                val fileName = it.getString(it.getColumnIndexOrThrow(COLUMN_FILE_NAME))
                val fileSize = it.getLong(it.getColumnIndexOrThrow(COLUMN_FILE_SIZE))
                val isDirectory = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_DIRECTORY)) == 1
                val deletedTimestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_DELETED_TIMESTAMP))
                val expiryTimestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_EXPIRY_TIMESTAMP))

                items.add(
                    RecycleBinItem(
                        id = id,
                        originalPath = originalPath,
                        trashedPath = trashedPath,
                        fileName = fileName,
                        fileSize = fileSize,
                        isDirectory = isDirectory,
                        deletedTimestamp = deletedTimestamp,
                        expiryTimestamp = expiryTimestamp
                    )
                )
            }
        }
        items
    }

    suspend fun getItemCount(): Int = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_RECYCLE_BIN", null)
        cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    suspend fun deleteItem(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val rows = db.delete(TABLE_RECYCLE_BIN, "$COLUMN_ID = ?", arrayOf(id.toString()))
        rows > 0
    }

    suspend fun clearAll(): Int = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_RECYCLE_BIN, null, null)
    }

    private fun moveFileOrDirectory(source: File, destination: File): Boolean {
        if (source.renameTo(destination)) {
            return true
        }

        // Fallback: Copy and Delete
        return try {
            if (source.isDirectory) {
                copyDirectory(source, destination)
                source.deleteRecursively()
            } else {
                copyFile(source, destination)
                source.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @Throws(IOException::class)
    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8192)
                var length: Int
                while (input.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun copyDirectory(source: File, destination: File) {
        if (!destination.exists()) {
            destination.mkdirs()
        }
        source.listFiles()?.forEach { file ->
            val destChild = File(destination, file.name)
            if (file.isDirectory) {
                copyDirectory(file, destChild)
            } else {
                copyFile(file, destChild)
            }
        }
    }

    private fun computeDirectorySize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }
}
