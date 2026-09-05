package com.bimantara.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_docs")
data class ScannedDocEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val imagePath: String,
    val extractedText: String = "",
    val pdfPath: String? = null,
    val filterApplied: String = "ORIGINAL", // ORIGINAL, MAGIC_COLOR, BW, GRAYSCALE
    val createdAt: Long = System.currentTimeMillis()
)
