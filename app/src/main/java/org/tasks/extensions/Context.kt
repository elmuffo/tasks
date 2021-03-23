package org.tasks.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.tasks.R

object Context {
    fun Context.safeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_app_found)
        }
    }

    fun Context.toast(resId: Int, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(this, resId, duration).show()
    }
}