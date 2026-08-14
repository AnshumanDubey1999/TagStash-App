package com.anshuman.tagstash.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.anshuman.tagstash.data.model.AuditActionType
import com.anshuman.tagstash.data.model.AuditItemOutcome
import com.anshuman.tagstash.data.model.AuditLogEntry
import com.anshuman.tagstash.data.model.AuditLogItemDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AuditLogDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "tagstash_audit_logs.db"
        const val DATABASE_VERSION = 1

        const val TABLE_AUDIT_LOGS = "audit_logs"
        const val COLUMN_ID = "id"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_ACTION_TYPE = "action_type"
        const val COLUMN_SUMMARY = "summary"
        const val COLUMN_DEST_DIR = "destination_directory"
        const val COLUMN_TOTAL_ITEMS = "total_items"
        const val COLUMN_ITEMS_JSON = "items_json"

        @Volatile
        private var instance: AuditLogDatabaseHelper? = null

        fun getInstance(context: Context): AuditLogDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AuditLogDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_AUDIT_LOGS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_ACTION_TYPE TEXT NOT NULL,
                $COLUMN_SUMMARY TEXT NOT NULL,
                $COLUMN_DEST_DIR TEXT NOT NULL,
                $COLUMN_TOTAL_ITEMS INTEGER NOT NULL,
                $COLUMN_ITEMS_JSON TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AUDIT_LOGS")
        onCreate(db)
    }

    suspend fun insertLog(entry: AuditLogEntry): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TIMESTAMP, entry.timestamp)
            put(COLUMN_ACTION_TYPE, entry.actionType.name)
            put(COLUMN_SUMMARY, entry.summary)
            put(COLUMN_DEST_DIR, entry.destinationDirectory)
            put(COLUMN_TOTAL_ITEMS, entry.totalItems)
            put(COLUMN_ITEMS_JSON, serializeItems(entry.items))
        }
        db.insert(TABLE_AUDIT_LOGS, null, values)
    }

    suspend fun getAllLogs(): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<AuditLogEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_AUDIT_LOGS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COLUMN_ID)
            val tsIdx = c.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val typeIdx = c.getColumnIndexOrThrow(COLUMN_ACTION_TYPE)
            val summaryIdx = c.getColumnIndexOrThrow(COLUMN_SUMMARY)
            val destIdx = c.getColumnIndexOrThrow(COLUMN_DEST_DIR)
            val totalIdx = c.getColumnIndexOrThrow(COLUMN_TOTAL_ITEMS)
            val jsonIdx = c.getColumnIndexOrThrow(COLUMN_ITEMS_JSON)

            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val timestamp = c.getLong(tsIdx)
                val actionType = try {
                    AuditActionType.valueOf(c.getString(typeIdx))
                } catch (_: Exception) {
                    AuditActionType.PASTE
                }
                val summary = c.getString(summaryIdx)
                val destDir = c.getString(destIdx)
                val totalItems = c.getInt(totalIdx)
                val itemsJson = c.getString(jsonIdx)
                val items = deserializeItems(itemsJson)

                logs.add(
                    AuditLogEntry(
                        id = id,
                        timestamp = timestamp,
                        actionType = actionType,
                        summary = summary,
                        destinationDirectory = destDir,
                        totalItems = totalItems,
                        items = items
                    )
                )
            }
        }
        logs
    }

    suspend fun deleteLog(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val rows = db.delete(TABLE_AUDIT_LOGS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        rows > 0
    }

    suspend fun clearAllLogs(): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val rows = db.delete(TABLE_AUDIT_LOGS, null, null)
        rows >= 0
    }

    private fun serializeItems(items: List<AuditLogItemDetail>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("sourcePath", item.sourcePath)
                put("destinationPath", item.destinationPath ?: "")
                put("command", item.command)
                put("outcome", item.outcome.name)
                put("fileName", item.fileName)
                put("fileSize", item.fileSize)
                put("isDirectory", item.isDirectory)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeItems(json: String): List<AuditLogItemDetail> {
        val list = mutableListOf<AuditLogItemDetail>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val dest = obj.optString("destinationPath", "")
                val outcomeName = obj.optString("outcome", AuditItemOutcome.COPIED.name)
                val outcome = try {
                    AuditItemOutcome.valueOf(outcomeName)
                } catch (_: Exception) {
                    AuditItemOutcome.COPIED
                }

                list.add(
                    AuditLogItemDetail(
                        sourcePath = obj.optString("sourcePath", ""),
                        destinationPath = if (dest.isEmpty()) null else dest,
                        command = obj.optString("command", "COPY"),
                        outcome = outcome,
                        fileName = obj.optString("fileName", ""),
                        fileSize = obj.optLong("fileSize", 0L),
                        isDirectory = obj.optBoolean("isDirectory", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
