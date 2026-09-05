package com.example.feature.notes

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class NoteMode {
    TYPE, STYLUS
}

enum class NoteDateFilter {
    ANY, TODAY, PAST_7_DAYS, PAST_30_DAYS
}

enum class NoteSizeFilter {
    ANY, SHORT_UNDER_100, MEDIUM_100_TO_500, LONG_OVER_500
}

data class NoteSearchResult(
    val note: NoteEntity,
    val matchedInTitle: Boolean = false,
    val matchedInContent: Boolean = false,
    val matchedInTags: Boolean = false,
    val snippet: String? = null
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getDatabase(application))

    val allNotes = repository.notes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dateFilter = MutableStateFlow(NoteDateFilter.ANY)
    val dateFilter: StateFlow<NoteDateFilter> = _dateFilter.asStateFlow()

    private val _sizeFilter = MutableStateFlow(NoteSizeFilter.ANY)
    val sizeFilter: StateFlow<NoteSizeFilter> = _sizeFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL") // "ALL", "TYPE", "STYLUS", "PINNED"
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    val filteredNotes: StateFlow<List<NoteSearchResult>> = combine(
        repository.notes,
        _searchQuery,
        _dateFilter,
        _sizeFilter,
        _typeFilter
    ) { notes, query, dateOpt, sizeOpt, typeOpt ->
        val q = query.trim().lowercase(Locale.getDefault())
        val now = System.currentTimeMillis()

        notes.mapNotNull { note ->
            // 1. Type / Mode filter
            val matchesType = when (typeOpt) {
                "TYPE" -> note.mode == "TYPE"
                "STYLUS" -> note.mode == "STYLUS"
                "PINNED" -> note.isPinned
                else -> true
            }
            if (!matchesType) return@mapNotNull null

            // 2. Date filter
            val matchesDate = when (dateOpt) {
                NoteDateFilter.ANY -> true
                NoteDateFilter.TODAY -> note.updatedAt >= now - (24 * 60 * 60 * 1000L)
                NoteDateFilter.PAST_7_DAYS -> note.updatedAt >= now - (7 * 24 * 60 * 60 * 1000L)
                NoteDateFilter.PAST_30_DAYS -> note.updatedAt >= now - (30 * 24 * 60 * 60 * 1000L)
            }
            if (!matchesDate) return@mapNotNull null

            // 3. Size / Length filter (based on content length)
            val contentLength = note.content.length
            val matchesSize = when (sizeOpt) {
                NoteSizeFilter.ANY -> true
                NoteSizeFilter.SHORT_UNDER_100 -> contentLength < 100
                NoteSizeFilter.MEDIUM_100_TO_500 -> contentLength in 100..500
                NoteSizeFilter.LONG_OVER_500 -> contentLength > 500
            }
            if (!matchesSize) return@mapNotNull null

            // 4. Query text matching across title, content, and tags
            val titleLower = note.title.lowercase(Locale.getDefault())
            val contentLower = note.content.lowercase(Locale.getDefault())
            val tagsLower = note.tags.lowercase(Locale.getDefault())

            val matchedInTitle = q.isNotEmpty() && titleLower.contains(q)
            val matchedInTags = q.isNotEmpty() && tagsLower.contains(q)
            var matchedInContent = false
            var snippet: String? = null

            if (q.isNotEmpty()) {
                val idx = contentLower.indexOf(q)
                if (idx >= 0) {
                    matchedInContent = true
                    val start = (idx - 25).coerceAtLeast(0)
                    val end = (idx + q.length + 35).coerceAtMost(note.content.length)
                    val rawSnippet = note.content.substring(start, end).replace('\n', ' ').trim()
                    snippet = "...$rawSnippet..."
                }

                if (!matchedInTitle && !matchedInContent && !matchedInTags) {
                    return@mapNotNull null
                }
            }

            NoteSearchResult(
                note = note,
                matchedInTitle = matchedInTitle,
                matchedInContent = matchedInContent,
                matchedInTags = matchedInTags,
                snippet = snippet
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDateFilter(filter: NoteDateFilter) {
        _dateFilter.value = filter
    }

    fun setSizeFilter(filter: NoteSizeFilter) {
        _sizeFilter.value = filter
    }

    fun setTypeFilter(filter: String) {
        _typeFilter.value = filter
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _dateFilter.value = NoteDateFilter.ANY
        _sizeFilter.value = NoteSizeFilter.ANY
        _typeFilter.value = "ALL"
    }

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _activeNoteId = MutableStateFlow<Long?>(null)
    val activeNoteId: StateFlow<Long?> = _activeNoteId.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _currentMode = MutableStateFlow(NoteMode.TYPE)
    val currentMode: StateFlow<NoteMode> = _currentMode.asStateFlow()

    private val _tags = MutableStateFlow("Penting")
    val tags: StateFlow<String> = _tags.asStateFlow()

    private val _isPinned = MutableStateFlow(false)
    val isPinned: StateFlow<Boolean> = _isPinned.asStateFlow()

    val currentStrokes = mutableStateListOf<DrawnStroke>()

    init {
        // Seed default notes if empty
        viewModelScope.launch {
            repository.notes.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultNotes()
                }
            }
        }
    }

    private suspend fun seedDefaultNotes() {
        val note1 = NoteEntity(
            title = "Catatan Ide Multi Tools",
            content = "1. File Manager dengan antarmuka bergaya Windows dan deteksi jaringan LAN.\n2. CamScanner dengan OCR teks dan ekspor PDF.\n3. Catatan Stylus dan Input Suara untuk produktivitas tinggi.",
            mode = "TYPE",
            tags = "Kerja",
            isPinned = true
        )
        val note2 = NoteEntity(
            title = "Sketsa & Tulisan Tangan Stylus",
            content = "[Catatan menggunakan Pena Stylus Digital]",
            mode = "STYLUS",
            tags = "Sketsa",
            isPinned = false
        )
        repository.insertNote(note1)
        repository.insertNote(note2)
    }

    fun startNewNote(mode: NoteMode = NoteMode.TYPE) {
        _activeNoteId.value = null
        _title.value = if (mode == NoteMode.TYPE) "Catatan Baru" else "Catatan Stylus Baru"
        _content.value = ""
        _currentMode.value = mode
        _tags.value = "Umum"
        _isPinned.value = false
        currentStrokes.clear()
        _isEditing.value = true
    }

    fun openNote(note: NoteEntity) {
        _activeNoteId.value = note.id
        _title.value = note.title
        _content.value = note.content
        _currentMode.value = if (note.mode == "STYLUS") NoteMode.STYLUS else NoteMode.TYPE
        _tags.value = note.tags
        _isPinned.value = note.isPinned
        currentStrokes.clear()
        if (note.mode == "STYLUS" && note.stylusDrawingData.isNotBlank()) {
            deserializeStrokes(note.stylusDrawingData)
        }
        _isEditing.value = true
    }

    fun closeEditor() {
        _isEditing.value = false
    }

    fun setTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun setContent(newContent: String) {
        _content.value = newContent
    }

    fun setMode(mode: NoteMode) {
        _currentMode.value = mode
    }

    fun togglePinned() {
        _isPinned.value = !_isPinned.value
    }

    fun appendVoiceInput(voiceText: String) {
        if (_currentMode.value == NoteMode.TYPE) {
            val current = _content.value
            _content.value = if (current.isBlank()) voiceText else "$current $voiceText"
        } else {
            val current = _title.value
            _title.value = if (current.startsWith("Catatan")) voiceText else "$current - $voiceText"
        }
    }

    fun undoStroke() {
        if (currentStrokes.isNotEmpty()) {
            currentStrokes.removeAt(currentStrokes.size - 1)
        }
    }

    fun clearCanvas() {
        currentStrokes.clear()
    }

    fun saveNote() {
        viewModelScope.launch(Dispatchers.IO) {
            val strokesJson = if (_currentMode.value == NoteMode.STYLUS) {
                serializeStrokes()
            } else ""

            val note = NoteEntity(
                id = _activeNoteId.value ?: 0,
                title = _title.value.ifBlank { "Tanpa Judul" },
                content = _content.value,
                mode = _currentMode.value.name,
                stylusDrawingData = strokesJson,
                tags = _tags.value,
                isPinned = _isPinned.value,
                updatedAt = System.currentTimeMillis()
            )

            if (_activeNoteId.value == null) {
                val newId = repository.insertNote(note)
                _activeNoteId.value = newId
            } else {
                repository.updateNote(note)
            }
            _isEditing.value = false
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(note)
        }
    }

    private fun serializeStrokes(): String {
        return try {
            val array = JSONArray()
            for (stroke in currentStrokes) {
                val obj = JSONObject()
                obj.put("color", stroke.color.value.toLong())
                obj.put("width", stroke.strokeWidth.toDouble())
                obj.put("isEraser", stroke.isEraser)
                val pointsArr = JSONArray()
                for (p in stroke.points) {
                    val pObj = JSONObject()
                    pObj.put("x", p.x.toDouble())
                    pObj.put("y", p.y.toDouble())
                    pointsArr.put(pObj)
                }
                obj.put("points", pointsArr)
                array.put(obj)
            }
            array.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun deserializeStrokes(json: String) {
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val colorVal = obj.getLong("color").toULong()
                val width = obj.getDouble("width").toFloat()
                val isEraser = obj.optBoolean("isEraser", false)
                val pointsArr = obj.getJSONArray("points")
                val points = mutableListOf<Offset>()
                for (j in 0 until pointsArr.length()) {
                    val pObj = pointsArr.getJSONObject(j)
                    points.add(Offset(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                }
                currentStrokes.add(
                    DrawnStroke(
                        points = points,
                        color = Color(colorVal),
                        strokeWidth = width,
                        isEraser = isEraser
                    )
                )
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
