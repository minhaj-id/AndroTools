package com.bimantara.feature.planner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

object PlannerNotificationHelper {

    fun scheduleAlarm(context: Context, id: Long, title: String, description: String, triggerMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, PlannerAlarmReceiver::class.java).apply {
            putExtra("EXTRA_ID", id)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_DESC", description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: Exception) {
            // Fallback for security restrictions
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, id: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PlannerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun triggerTestNotification(context: Context, title: String, message: String) {
        PlannerAlarmReceiver.showPlannerNotification(
            context = context,
            title = "🔔 [Pengingat] $title",
            description = message,
            notificationId = (System.currentTimeMillis() % 10000).toInt()
        )
        Toast.makeText(context, "Notifikasi pengingat ditampilkan!", Toast.LENGTH_SHORT).show()
    }
}
