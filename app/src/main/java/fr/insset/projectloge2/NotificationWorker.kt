package fr.insset.projectloge2

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters


class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context,
    workerParams
) {
    val notificationManager : NotificationManagerCompat = NotificationManagerCompat.from(context)

    override fun doWork(): Result {

        showNotification(
            "Hey!",
            "Prends une photo de ta localisation actuelle!"
        )
        return Result.success()
    }

    private fun showNotification(title: String, desc: String) {

        val notification =
            NotificationCompat.Builder(applicationContext, App.photoNotificationChannel)
                .setContentTitle(title)
                .setContentText(desc)
                .setSmallIcon(R.mipmap.ic_launcher)

        notificationManager.notify(1, notification.build())
    }

}