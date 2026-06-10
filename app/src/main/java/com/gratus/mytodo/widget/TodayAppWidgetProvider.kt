package com.gratus.mytodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gratus.mytodo.MainActivity
import com.gratus.mytodo.R
import com.gratus.mytodo.components.NotificationReceiver
import com.gratus.mytodo.data.TaskDatabase
import com.gratus.mytodo.ui.utils.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Provider responsible for managing the Today widget container, setup, and click broadcast handlers.
 */
class TodayAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            
            // Format and display today's date in header
            val cal = Calendar.getInstance()
            views.setTextViewText(R.id.widget_date_label, DateTimeUtils.formatHomeDateLabel(cal))

            // Set up list adapter backing TodayWidgetService
            val adapterIntent = Intent(context, TodayWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, adapterIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

            // Setup Template PendingIntent on ListView for interactive clicks
            val toggleIntent = Intent(context, TodayAppWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_COMPLETE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId, // Unique request code per widget ID
                toggleIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, togglePendingIntent)

            // Setup app launcher click intent for header app icon
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_app_icon, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        
        if (action == ACTION_WIDGET_UPDATE || action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, TodayAppWidgetProvider::class.java))
            
            // Re-render widget headers and backgrounds
            onUpdate(context, appWidgetManager, ids)
            
            // Refresh tasks list view items
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
            
        } else if (action == ACTION_TOGGLE_COMPLETE) {
            val taskId = intent.getIntExtra("task_id", -1)
            if (taskId != -1) {
                val db = TaskDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null) {
                        val updated = task.copy(isCompleted = !task.isCompleted)
                        val updatedWithReset = if (!updated.isCompleted) updated.copy(repeatedTimes = 0) else updated
                        db.taskDao().updateTask(updatedWithReset)
                        
                        // Cancel or schedule exact reminder alarm
                        if (updatedWithReset.isCompleted) {
                            NotificationReceiver.cancelReminder(context, updatedWithReset)
                        } else if (updatedWithReset.reminderTime != null && updatedWithReset.reminderTime > System.currentTimeMillis()) {
                            NotificationReceiver.scheduleExactReminder(context, updatedWithReset)
                        }
                        
                        // Broadcast update to notify widgets to redraw
                        val updateIntent = Intent(ACTION_WIDGET_UPDATE).apply {
                            setPackage(context.packageName)
                        }
                        context.sendBroadcast(updateIntent)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_COMPLETE = "com.gratus.mytodo.action.TOGGLE_COMPLETE"
        const val ACTION_WIDGET_UPDATE = "com.gratus.mytodo.action.WIDGET_UPDATE"
    }
}
