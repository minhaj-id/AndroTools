package com.bimantara.data.repository

import com.bimantara.data.db.AppDatabase
import com.bimantara.data.model.NoteEntity
import com.bimantara.data.model.PlannerItemEntity
import com.bimantara.data.model.ScannedDocEntity
import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {
    val notes: Flow<List<NoteEntity>> = database.noteDao().getAllNotes()
    val plannerItems: Flow<List<PlannerItemEntity>> = database.plannerDao().getAllItems()
    val scannedDocs: Flow<List<ScannedDocEntity>> = database.docDao().getAllDocs()

    suspend fun insertNote(note: NoteEntity): Long = database.noteDao().insertNote(note)
    suspend fun updateNote(note: NoteEntity) = database.noteDao().updateNote(note)
    suspend fun deleteNote(note: NoteEntity) = database.noteDao().deleteNote(note)
    fun searchNotes(query: String) = database.noteDao().searchNotes(query)

    suspend fun insertPlannerItem(item: PlannerItemEntity): Long = database.plannerDao().insertItem(item)
    suspend fun updatePlannerItem(item: PlannerItemEntity) = database.plannerDao().updateItem(item)
    suspend fun deletePlannerItem(item: PlannerItemEntity) = database.plannerDao().deleteItem(item)
    suspend fun setPlannerCompleted(id: Long, completed: Boolean) = database.plannerDao().setCompleted(id, completed)

    suspend fun insertScannedDoc(doc: ScannedDocEntity): Long = database.docDao().insertDoc(doc)
    suspend fun updateScannedDoc(doc: ScannedDocEntity) = database.docDao().updateDoc(doc)
    suspend fun deleteScannedDoc(doc: ScannedDocEntity) = database.docDao().deleteDoc(doc)
}
