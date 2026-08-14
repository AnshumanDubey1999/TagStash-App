package com.anshuman.tagstash.data.model

data class RecycleBinItem(
    val id: Long = 0L,
    val originalPath: String,
    val trashedPath: String,
    val fileName: String,
    val fileSize: Long,
    val isDirectory: Boolean,
    val deletedTimestamp: Long,
    val expiryTimestamp: Long
)
