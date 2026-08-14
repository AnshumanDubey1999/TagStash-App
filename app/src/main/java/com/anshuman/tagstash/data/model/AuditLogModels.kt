package com.anshuman.tagstash.data.model

enum class AuditActionType {
    PASTE,
    DELETE,
    RESTORE,
    DELETE_PERMANENT,
    AUTO_DELETE
}

enum class AuditItemOutcome {
    COPIED,
    MOVED,
    REPLACED,
    RENAMED_COPY,
    SKIPPED,
    TRASHED,
    RESTORED,
    PERMANENTLY_DELETED,
    AUTO_DELETED_EXPIRED
}

data class AuditLogItemDetail(
    val sourcePath: String,
    val destinationPath: String?,
    val command: String, // "CUT" or "COPY"
    val outcome: AuditItemOutcome,
    val fileName: String,
    val fileSize: Long,
    val isDirectory: Boolean
)

data class AuditLogEntry(
    val id: Long = 0L,
    val timestamp: Long,
    val actionType: AuditActionType,
    val summary: String,
    val destinationDirectory: String,
    val totalItems: Int,
    val items: List<AuditLogItemDetail>
)
