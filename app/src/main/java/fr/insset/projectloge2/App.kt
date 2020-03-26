package fr.insset.projectloge2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import net.gotev.uploadservice.BuildConfig
import net.gotev.uploadservice.UploadServiceConfig

class App : Application() {

    companion object {
        // ID of the notification channel used by upload service. This is needed by Android API 26+
        // but you have to always specify it even if targeting lower versions, because it's handled
        // by AndroidX AppCompat library automatically
        const val notificationChannelID = "TestChannel"
        const val photoNotificationChannel = "channel1"
    }

    // Customize the notification channel as you wish. This is only for a bare minimum example
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            //channel used by upload service
            val uploadChannel = NotificationChannel(
                notificationChannelID,
                "TestApp Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            //channel used by photo notification
            val photoNotificationChannel = NotificationChannel(
                photoNotificationChannel,
                "Photo Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            photoNotificationChannel.description =
                "Some simple description, no really, just a simple test."

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(uploadChannel, photoNotificationChannel)
            manager.createNotificationChannels(channels)
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()
        UploadServiceConfig.initialize(
            context = this,
            defaultNotificationChannel = notificationChannelID,
            debug = BuildConfig.DEBUG
        )
    }
}