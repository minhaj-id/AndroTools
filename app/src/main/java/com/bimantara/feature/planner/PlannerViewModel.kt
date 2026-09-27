package com.bimantara.feature.planner

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bimantara.data.db.AppDatabase
import com.bimantara.data.model.PlannerItemEntity
import com.bimantara.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannerViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AppRepository = AppRepository(AppDatabase.getDatabase(application))
) : AndroidViewModel(application) {

    val allItems: StateFlow<List<PlannerItemEntity>> = repository.plannerItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterTab = MutableStateFlow("ALL")
    val filterTab: StateFlow<String> = _filterTab.asStateFlow()

    fun setFilterTab(tab: String) {
        _filterTab.value = tab
    }

    fun toggleCompleted(item: PlannerItemEntity) {
        viewModelScope.launch {
            repository.setPlannerCompleted(item.id, !item.isCompleted)
        }
    }

    fun deleteItem(item: PlannerItemEntity) {
        viewModelScope.launch {
            PlannerNotificationHelper.cancelAlarm(getApplication(), item.id)
            repository.deletePlannerItem(item)
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
        viewModelScope.launch {
            val entity = PlannerItemEntity(
                id = id,
                title = title,
                description = description,
                dueDateMillis = dueDateMillis,
                priority = priority,
                category = category,
                isCompleted = false,
                alarmEnabled = alarmEnabled
            )
            val newId = if (id == 0L) {
                repository.insertPlannerItem(entity)
            } else {
                repository.updatePlannerItem(entity)
                id
            }

            if (alarmEnabled && dueDateMillis > System.currentTimeMillis()) {
                PlannerNotificationHelper.scheduleAlarm(
                    context = getApplication(),
                    id = newId,
                    title = title,
                    description = description,
                    triggerMillis = dueDateMillis
                )
            }
        }
    }

    fun testNotificationNow(item: PlannerItemEntity) {
        PlannerNotificationHelper.triggerTestNotification(
            context = getApplication(),
            title = item.title,
            message = item.description.ifBlank { "Tenggat waktu: ${item.priority} Prioritas" }
        )
    }
}
