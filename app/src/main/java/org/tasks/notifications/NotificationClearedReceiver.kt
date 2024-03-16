package org.tasks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.injection.ApplicationScope
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationClearedReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationManager: NotificationManager
    @Inject @ApplicationScope lateinit var scope: CoroutineScope
    @Inject lateinit var preferences: Preferences

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)
        Timber.d("cleared $notificationId")
        scope.launch {
            if (preferences.usePersistentReminders()) {
                notificationManager.restoreNotifications(false)
            } else {
                notificationManager.cancel(notificationId)
            }
        }
    }
}
