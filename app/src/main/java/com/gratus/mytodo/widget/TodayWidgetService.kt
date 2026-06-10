package com.gratus.mytodo.widget

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.gratus.mytodo.R
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.TaskDatabase
import com.gratus.mytodo.ui.utils.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import androidx.core.content.ContextCompat

/**
 * Service that provides the adapter/factory for the list view inside the widget.
 */
class TodayWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayWidgetFactory(applicationContext)
    }
}

class TodayWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var tasks = listOf<Task>()

    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        val dateStr = DateTimeUtils.formatDbDate(System.currentTimeMillis())
        try {
            val db = TaskDatabase.getDatabase(context)
            tasks = runBlocking(Dispatchers.IO) {
                db.taskDao().getTasksForDateDirect(dateStr)
            }
        } catch (e: Exception) {
            android.util.Log.e("TodayWidgetFactory", "Error loading tasks for widget: ${e.message}", e)
            tasks = emptyList()
        }
    }

    override fun onDestroy() {
        // No-op
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= tasks.size) {
            return RemoteViews(context.packageName, R.layout.widget_task_item)
        }
        val task = tasks[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_task_item)

        // Set check icon based on completion state
        val checkIcon = if (task.isCompleted) R.drawable.ic_widget_check else R.drawable.ic_widget_uncheck
        rv.setImageViewResource(R.id.widget_item_check, checkIcon)

        // Set task title (apply strikethrough if completed)
        val titleSpan = SpannableString(task.title)
        if (task.isCompleted) {
            titleSpan.setSpan(StrikethroughSpan(), 0, task.title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.widget_item_title, titleSpan)

        // Programmatically set the text color based on task completion status
        val titleColor = if (task.isCompleted) {
            ContextCompat.getColor(context, R.color.widget_empty_text)
        } else {
            ContextCompat.getColor(context, R.color.widget_text_primary)
        }
        rv.setTextColor(R.id.widget_item_title, titleColor)

        // Set priority level and background shape
        rv.setTextViewText(R.id.widget_item_priority, task.priority.toString())
        val priorityBg = if (task.isCompleted) {
            R.drawable.widget_priority_completed
        } else {
            when (task.priority) {
                1 -> R.drawable.widget_priority_1
                2 -> R.drawable.widget_priority_2
                3 -> R.drawable.widget_priority_3
                4 -> R.drawable.widget_priority_4
                else -> R.drawable.widget_priority_completed
            }
        }
        rv.setInt(R.id.widget_item_priority, "setBackgroundResource", priorityBg)

        // Set click fill-in intent for task completion toggle
        val fillInIntent = Intent().apply {
            putExtra("task_id", task.id)
        }
        rv.setOnClickFillInIntent(R.id.widget_item_check, fillInIntent)

        return rv
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position < tasks.size) tasks[position].id.toLong() else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
