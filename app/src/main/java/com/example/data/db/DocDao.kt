package com.bimantara.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bimantara.data.model.ScannedDocEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocDao {
    @Query("SELECT * FROM scanned_docs ORDER BY createdAt DESC")
    fun getAllDocs(): Flow<List<ScannedDocEntity>>

    @Query("SELECT * FROM scanned_docs ORDER BY createdAt DESC")
    suspend fun getAllDocsDirect(): List<ScannedDocEntity>

    @Query("SELECT * FROM scanned_docs WHERE id = :id")
    suspend fun getDocById(id: Long): ScannedDocEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoc(doc: ScannedDocEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(docs: List<ScannedDocEntity>)

    @Update
    suspend fun updateDoc(doc: ScannedDocEntity)

    @Delete
    suspend fun deleteDoc(doc: ScannedDocEntity)
}
