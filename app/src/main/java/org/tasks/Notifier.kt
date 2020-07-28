package org.tasks

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.reminders.ReminderService
import com.todoroo.astrid.voice.VoiceOutputAssistant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import org.tasks.intents.TaskIntents
import org.tasks.notifications.AudioManager
import org.tasks.notifications.Notification
import org.tasks.notifications.NotificationManager
import org.tasks.notifications.TelephonyManager
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.time.DateTimeUtils
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.min

class Notifier @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val taskDao: TaskDao,
        private val notificationManager: NotificationManager,
        private val telephonyManager: TelephonyManager,
        private val audioManager: AudioManager,
        private val voiceOutputAssistant: VoiceOutputAssistant,
        private val preferences: Preferences) {

    private val colorProvider: ColorProvider = ColorProvider(context, preferences)

    suspend fun triggerFilterNotification(filter: Filter) {
        val tasks = taskDao.fetchFiltered(filter)
        val count = tasks.size
        if (count == 0) {
            return
        }
        val intent = TaskIntents.getTaskListIntent(context, filter)
        val pendingIntent = PendingIntent.getActivity(
                context, filter.listingTitle.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val summaryTitle = context.resources.getQuantityString(R.plurals.task_count, count, count)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(summaryTitle)
        var maxPriority = 3
        for (task in tasks) {
            style.addLine(task.title)
            maxPriority = min(maxPriority, task.priority)
        }
        val builder = NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_TASKER)
                .setSmallIcon(R.drawable.ic_done_all_white_24dp)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setTicker(summaryTitle)
                .setContentTitle(summaryTitle)
                .setContentText(filter.listingTitle)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setWhen(DateTimeUtils.currentTimeMillis())
                .setShowWhen(true)
                .setColor(colorProvider.getPriorityColor(maxPriority, true))
                .setGroupSummary(true)
                .setGroup(filter.listingTitle)
                .setStyle(style)
        notificationManager.notify(filter.listingTitle.hashCode().toLong(), builder, true, false, false)
    }

    suspend fun triggerNotifications(entries: List<Notification>) {
        val notifications: MutableList<Notification> = ArrayList()
        var ringFiveTimes = false
        var ringNonstop = false
        for (entry in entries.takeLast(NotificationManager.MAX_NOTIFICATIONS)) {
            val task = taskDao.fetch(entry.taskId) ?: continue
            if (entry.type != ReminderService.TYPE_RANDOM) {
                ringFiveTimes = ringFiveTimes or task.isNotifyModeFive
                ringNonstop = ringNonstop or task.isNotifyModeNonstop
            }
            val notification = notificationManager.getTaskNotification(entry)
            if (notification != null) {
                notifications.add(entry)
            }
        }
        if (notifications.isEmpty()) {
            return
        }

        Timber.d("Triggering $notifications")

        notificationManager.notifyTasks(notifications, true, ringNonstop, ringFiveTimes)

        if (preferences.getBoolean(R.string.p_voiceRemindersEnabled, false)
                && !ringNonstop
                && !audioManager.notificationsMuted()
                && telephonyManager.callStateIdle()) {
            notifications
                    .mapNotNull {
                        notificationManager.getTaskNotification(it)?.build()?.tickerText?.toString()
                    }
                    .forEach {
                        delay(2000)
                        voiceOutputAssistant.speak(it)
                    }
        }
    }
}