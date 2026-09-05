package com.bimantara.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bimantara.data.model.PlannerItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    @Query("SELECT * FROM planner_items ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getAllItems(): Flow<List<PlannerItemEntity>>

    @Query("SELECT * FROM planner_items ORDER BY isCompleted ASC, dueDateMillis ASC")
    suspend fun getAllItemsDirect(): List<PlannerItemEntity>

    @Query("SELECT * FROM planner_items WHERE id = :id")
    suspend fun getItemById(id: Long): PlannerItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PlannerItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlannerItemEntity>)

    @Update
    suspend fun updateItem(item: PlannerItemEntity)

    @Delete
    suspend fun deleteItem(item: PlannerItemEntity)

    @Query("UPDATE planner_items SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)
}
