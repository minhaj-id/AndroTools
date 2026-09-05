package com.bimantara.data.backup

import com.bimantara.data.model.NoteEntity
import com.bimantara.data.model.PlannerItemEntity
import com.bimantara.data.model.ScannedDocEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data model and serializer for encrypted Multi-Tools backup.
 */
data class BackupPayload(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: List<NoteEntity> = emptyList(),
    val plannerItems: List<PlannerItemEntity> = emptyList(),
    val scannedDocs: List<ScannedDocEntity> = emptyList()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("timestamp", timestamp)
        root.put("notesCount", notes.size)
        root.put("plannerCount", plannerItems.size)
        root.put("docsCount", scannedDocs.size)

        // Notes array
        val notesArray = JSONArray()
        for (note in notes) {
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("mode", note.mode)
            obj.put("stylusDrawingData", note.stylusDrawingData)
            obj.put("colorHex", note.colorHex)
            obj.put("tags", note.tags)
            obj.put("isPinned", note.isPinned)
            obj.put("createdAt", note.createdAt)
            obj.put("updatedAt", note.updatedAt)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        // Planner items array
        val plannerArray = JSONArray()
        for (item in plannerItems) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("description", item.description)
            obj.put("dueDateMillis", item.dueDateMillis)
            obj.put("priority", item.priority)
            obj.put("category", item.category)
            obj.put("isCompleted", item.isCompleted)
            obj.put("alarmEnabled", item.alarmEnabled)
            obj.put("createdAt", item.createdAt)
            plannerArray.put(obj)
        }
        root.put("plannerItems", plannerArray)

        // Scanned docs array
        val docsArray = JSONArray()
        for (doc in scannedDocs) {
            val obj = JSONObject()
            obj.put("id", doc.id)
            obj.put("title", doc.title)
            obj.put("imagePath", doc.imagePath)
            obj.put("extractedText", doc.extractedText)
            obj.put("pdfPath", doc.pdfPath ?: "")
            obj.put("filterApplied", doc.filterApplied)
            obj.put("createdAt", doc.createdAt)
            docsArray.put(obj)
        }
        root.put("scannedDocs", docsArray)

        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): BackupPayload {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            val notesList = mutableListOf<NoteEntity>()
            val notesArray = root.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    notesList.add(
                        NoteEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", ""),
                            content = obj.optString("content", ""),
                            mode = obj.optString("mode", "TYPE"),
                            stylusDrawingData = obj.optString("stylusDrawingData", ""),
                            colorHex = obj.optString("colorHex", "#1E293B"),
                            tags = obj.optString("tags", "General"),
                            isPinned = obj.optBoolean("isPinned", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val plannerList = mutableListOf<PlannerItemEntity>()
            val plannerArray = root.optJSONArray("plannerItems")
            if (plannerArray != null) {
                for (i in 0 until plannerArray.length()) {
                    val obj = plannerArray.getJSONObject(i)
                    plannerList.add(
                        PlannerItemEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            dueDateMillis = obj.optLong("dueDateMillis", System.currentTimeMillis()),
                            priority = obj.optString("priority", "MEDIUM"),
                            category = obj.optString("category", "WORK"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            alarmEnabled = obj.optBoolean("alarmEnabled", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val docsList = mutableListOf<ScannedDocEntity>()
            val docsArray = root.optJSONArray("scannedDocs")
            if (docsArray != null) {
                for (i in 0 until docsArray.length()) {
                    val obj = docsArray.getJSONObject(i)
                    val pdfPath = obj.optString("pdfPath", "")
                    docsList.add(
                        ScannedDocEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", ""),
                            imagePath = obj.optString("imagePath", ""),
                            extractedText = obj.optString("extractedText", ""),
                            pdfPath = if (pdfPath.isBlank()) null else pdfPath,
                            filterApplied = obj.optString("filterApplied", "ORIGINAL"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            return BackupPayload(
                version = version,
                timestamp = timestamp,
                notes = notesList,
                plannerItems = plannerList,
                scannedDocs = docsList
            )
        }
    }
}
