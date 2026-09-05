package com.bimantara.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val mode: String, // "TYPE" or "STYLUS"
    val stylusDrawingData: String = "", // serialized stroke paths
    val colorHex: String = "#1E293B",
    val tags: String = "General",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
