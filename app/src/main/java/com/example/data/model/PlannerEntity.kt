package com.bimantara.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planner_items")
data class PlannerItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long = System.currentTimeMillis() + 3600000L,
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val category: String = "WORK", // "WORK", "PERSONAL", "STUDY", "URGENT"
    val isCompleted: Boolean = false,
    val alarmEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
