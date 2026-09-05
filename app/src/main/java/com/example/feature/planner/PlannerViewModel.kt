package com.example.feature.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.PlannerItemEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getDatabase(application))

    val allItems = repository.plannerItems.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _filterTab = MutableStateFlow("ALL") // ALL, TODAY, UPCOMING, COMPLETED
    val filterTab: StateFlow<String> = _filterTab.asStateFlow()

    init {
        // Seed default planner items if empty
        viewModelScope.launch {
            repository.plannerItems.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultPlanner()
                }
            }
        }
    }

    private suspend fun seedDefaultPlanner() {
        val now = System.currentTimeMillis()
        val item1 = PlannerItemEntity(
            title = "Pindai Dokumen Pajak & Faktur",
            description = "Gunakan CamScanner untuk scan berkas fisik dan ekspor ke format PDF.",
            dueDateMillis = now + 7200000L, // +2 hours
            priority = "HIGH",
            category = "WORK",
            isCompleted = false,
            alarmEnabled = true
        )
        val item2 = PlannerItemEntity(
            title = "Bersihkan Memori & Sampah HP",
            description = "Jalankan Deep Cleaner untuk menghapus cache aplikasi dan backup APK penting.",
            dueDateMillis = now + 86400000L, // +1 day
            priority = "MEDIUM",
            category = "PERSONAL",
            isCompleted = false,
            alarmEnabled = true
        )
        val item3 = PlannerItemEntity(
            title = "Review Catatan Stylus Rapat",
            description = "Periksa sketsa diagram alur yang dibuat di menu Notes.",
            dueDateMillis = now + 172800000L, // +2 days
            priority = "LOW",
            category = "STUDY",
            isCompleted = true,
            alarmEnabled = false
        )
        repository.insertPlannerItem(item1)
        repository.insertPlannerItem(item2)
        repository.insertPlannerItem(item3)
        com.example.widget.PlannerWidgetProvider.notifyDataChanged(getApplication())
    }

    fun setFilterTab(tab: String) {
        _filterTab.value = tab
    }

    fun toggleCompleted(item: PlannerItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPlannerCompleted(item.id, !item.isCompleted)
            com.example.widget.PlannerWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    fun saveItem(
        id: Long,
        title: String,
        description: String,
        dueDateMillis: Long,
        priority: String,
        category: String,
        alarmEnabled: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = PlannerItemEntity(
                id = id,
                title = title.ifBlank { "Rencana Tanpa Judul" },
                description = description,
                dueDateMillis = dueDateMillis,
                priority = priority,
                category = category,
                isCompleted = false,
                alarmEnabled = alarmEnabled
            )

            val savedId = if (id == 0L) {
                repository.insertPlannerItem(item)
            } else {
                repository.updatePlannerItem(item)
                id
            }

            com.example.widget.PlannerWidgetProvider.notifyDataChanged(getApplication())

            if (alarmEnabled && dueDateMillis > System.currentTimeMillis()) {
                PlannerNotificationHelper.scheduleAlarm(
                    context = getApplication<Application>(),
                    id = savedId,
                    title = item.title,
                    description = item.description.ifBlank { "Waktunya menjalankan rencana ini!" },
                    triggerMillis = dueDateMillis
                )
            }
        }
    }

    fun deleteItem(item: PlannerItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            PlannerNotificationHelper.cancelAlarm(getApplication<Application>(), item.id)
            repository.deletePlannerItem(item)
            com.example.widget.PlannerWidgetProvider.notifyDataChanged(getApplication())
        }
    }

    fun testNotificationNow(item: PlannerItemEntity) {
        PlannerNotificationHelper.triggerTestNotification(
            context = getApplication<Application>(),
            title = item.title,
            message = item.description.ifBlank { "Pengingat jadwal rencana telah tiba!" }
        )
    }
}
