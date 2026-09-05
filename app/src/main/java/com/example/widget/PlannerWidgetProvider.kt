package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class PlannerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_QUICK_NOTE = "com.example.widget.ACTION_QUICK_NOTE"
        const val ACTION_OPEN_PLANNER = "com.example.widget.ACTION_OPEN_PLANNER"
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"

        const val EXTRA_DESTINATION = "EXTRA_DESTINATION"
        const val EXTRA_QUICK_ACTION = "EXTRA_QUICK_ACTION"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"

        fun notifyDataChanged(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisAppWidget = ComponentName(context.packageName, PlannerWidgetProvider::class.java.name)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                if (appWidgetIds.isNotEmpty()) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_task_list)
                }
            } catch (e: Exception) {
                // Ignore if widget is not active
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_planner)

            // Connect RemoteViewsService for dynamic list
            val serviceIntent = Intent(context, PlannerWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_task_list, serviceIntent)
            views.setEmptyView(R.id.widget_task_list, R.id.widget_empty_view)

            // Shortcut 1: Quick Note Shortcut
            val noteIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_QUICK_NOTE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_DESTINATION, "NOTES")
                putExtra(EXTRA_QUICK_ACTION, "NEW_NOTE")
            }
            val notePendingIntent = PendingIntent.getActivity(
                context,
                101,
                noteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_quick_note, notePendingIntent)

            // Shortcut 2: Open Planner Shortcut
            val plannerIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_PLANNER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_DESTINATION, "PLANNER")
            }
            val plannerPendingIntent = PendingIntent.getActivity(
                context,
                102,
                plannerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_open_planner, plannerPendingIntent)

            // Shortcut 3: Refresh Button
            val refreshIntent = Intent(context, PlannerWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                103,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // Click template for individual task items
            val itemClickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val itemClickPendingIntent = PendingIntent.getActivity(
                context,
                104,
                itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_task_list, itemClickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            notifyDataChanged(context)
        }
    }
}
