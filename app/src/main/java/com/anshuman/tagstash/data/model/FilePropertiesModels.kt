package com.anshuman.tagstash.data.model

data class FileTag(
    val id: Long = 0,
    val name: String,
    val colorHex: Int = 0xFF81C784.toInt()
)

data class MetadataItem(
    val label: String,
    val value: String
)

data class MetadataGroup(
    val title: String,
    val items: List<MetadataItem>
)

data class FilePropertiesData(
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val isDirectory: Boolean,
    val groups: List<MetadataGroup>,
    val tags: List<FileTag> = emptyList()
)
