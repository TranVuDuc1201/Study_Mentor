package com.example.study_mentor

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.study_mentor.notifications.StudyNotificationHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyNotificationHelperTest {

    @Test
    fun testShowNotification() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Tren Android 13+, can cap quyen truoc khi test.
        assumeTrue(
            StudyNotificationHelper.hasNotificationPermission(context)
        )

        // Goi Backend gui thong bao.
        val result =
            StudyNotificationHelper.showTestNotification(context)

        assertTrue("Khong gui duoc thong bao", result)

        // Kiem tra kenh thong bao.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertNotNull(
                manager.getNotificationChannel(
                    "study_mentor_notifications"
                )
            )
        }

        // Kiem tra thong bao da duoc dang.
        var notificationFound = false

        repeat(10) {
            notificationFound = manager.activeNotifications.any {
                it.id == 1001
            }

            if (notificationFound) return@repeat

            Thread.sleep(200)
        }

        assertTrue(
            "Khong tim thay thong bao Study Mentor",
            notificationFound
        )
    }
}
