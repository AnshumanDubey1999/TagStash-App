package com.anshuman.tagstash.data.clipboard

import androidx.compose.runtime.mutableStateListOf
import java.io.File

enum class ClipboardOpType {
    CUT,
    COPY
}

data class ClipboardItem(
    val file: File,
    val opType: ClipboardOpType
)

object AppClipboard {
    const val MAX_CAPACITY = 1000

    private val _items = mutableStateListOf<ClipboardItem>()
    val items: List<ClipboardItem> get() = _items
    val size: Int get() = _items.size

    /**
     * Adds or updates a file in the clipboard with the specified operation.
     * @return true if added/updated successfully, false if max capacity (1000) was reached for a new item.
     */
    fun addItem(file: File, opType: ClipboardOpType): Boolean {
        val existingIndex = _items.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex != -1) {
            _items[existingIndex] = ClipboardItem(file, opType)
            return true
        }
        if (_items.size >= MAX_CAPACITY) {
            return false
        }
        _items.add(ClipboardItem(file, opType))
        return true
    }

    fun removeItem(file: File) {
        _items.removeAll { it.file.absolutePath == file.absolutePath }
    }

    fun clear() {
        _items.clear()
    }

    fun contains(file: File): Boolean {
        return _items.any { it.file.absolutePath == file.absolutePath }
    }

    fun getOpType(file: File): ClipboardOpType? {
        return _items.firstOrNull { it.file.absolutePath == file.absolutePath }?.opType
    }
}
