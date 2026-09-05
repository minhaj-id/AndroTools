package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.PlannerItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PlannerWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PlannerRemoteViewsFactory(applicationContext)
    }
}

class PlannerRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<PlannerItemEntity> = emptyList()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        try {
            runBlocking(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(context)
                val all = db.plannerDao().getAllItemsDirect()
                items = all
                    .filter { !it.isCompleted }
                    .sortedBy { it.dueDateMillis }
                    .take(12)
            }
        } catch (e: Exception) {
            items = emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position !in items.indices) {
            return RemoteViews(context.packageName, R.layout.widget_task_item)
        }

        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        views.setTextViewText(R.id.widget_item_title, item.title)

        val formattedDate = formatDueDate(item.dueDateMillis)
        views.setTextViewText(R.id.widget_item_due, "$formattedDate • ${item.category}")

        val priorityText = when (item.priority) {
            "HIGH" -> "HIGH"
            "LOW" -> "LOW"
            else -> "MED"
        }
        views.setTextViewText(R.id.widget_item_priority_badge, priorityText)

        when (item.priority) {
            "HIGH" -> {
                views.setImageViewResource(R.id.widget_item_priority_dot, R.drawable.widget_dot_high)
                views.setTextColor(R.id.widget_item_priority_badge, Color.parseColor("#EF4444"))
            }
            "LOW" -> {
                views.setImageViewResource(R.id.widget_item_priority_dot, R.drawable.widget_dot_low)
                views.setTextColor(R.id.widget_item_priority_badge, Color.parseColor("#10B981"))
            }
            else -> {
                views.setImageViewResource(R.id.widget_item_priority_dot, R.drawable.widget_dot_medium)
                views.setTextColor(R.id.widget_item_priority_badge, Color.parseColor("#F59E0B"))
            }
        }

        // Fill-in Intent when tapped -> Opens MainActivity at Planner screen
        val fillInIntent = Intent().apply {
            putExtra(PlannerWidgetProvider.EXTRA_DESTINATION, "PLANNER")
            putExtra(PlannerWidgetProvider.EXTRA_TASK_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    private fun formatDueDate(millis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }

        val isSameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        val isTomorrow = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) + 1 == target.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> "Hari ini ${timeFormat.format(Date(millis))}"
            isTomorrow -> "Besok ${timeFormat.format(Date(millis))}"
            else -> dateFormat.format(Date(millis))
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in items.indices) items[position].id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
