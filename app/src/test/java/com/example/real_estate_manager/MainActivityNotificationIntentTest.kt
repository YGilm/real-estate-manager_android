package com.example.real_estate_manager

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityNotificationIntentTest {

    @Test
    fun pushTapIntent_resolvesNotificationDetailsTarget() {
        val intent = Intent(MainActivity.ACTION_OPEN_NOTIFICATION_DETAILS)
            .putExtra(MainActivity.EXTRA_NOTIFICATION_ID, "notification-id")
            .putExtra(MainActivity.EXTRA_REMINDER_ID, "reminder-id")

        val target = MainActivity.notificationTargetFrom(intent)

        assertEquals("notification-id", target?.notificationId)
        assertEquals("reminder-id", target?.reminderId)
    }

    @Test
    fun unrelatedIntent_hasNoNotificationTarget() {
        assertNull(MainActivity.notificationTargetFrom(Intent(Intent.ACTION_VIEW)))
    }
}
