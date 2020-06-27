package com.example.cahyo_research_two_bg_service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cahyo_research_two_bg_service.service.NotificationService
import io.flutter.plugin.common.MethodChannel
import org.eclipse.paho.client.mqttv3.MqttMessage
import kotlin.random.Random

class Util {
    companion object {
        fun showNotification(context: Context, mqttMessage: MqttMessage?, mBackgroundChannel: MethodChannel? = null) {
            mqttMessage?.let {
                val strMessage = String(it.payload)

                if (mBackgroundChannel != null) mBackgroundChannel.run { invokeMethod("mqttMessageArrive", strMessage) }

                val listMessage = (Storage.getValue(Constant.NOTIFICATION_STORAGE_KEY) as MutableList<*>).map {
                    it.toString()
                }.toMutableList()
                listMessage.add(strMessage)
                Storage.saveValue(Constant.NOTIFICATION_STORAGE_KEY, listMessage)

                Log.d(NotificationService.TAG, "message has arrived")

                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

                if (!pm.isScreenOn) {
                    val wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "com.example.cahyo_research_two_bg_service:POWER_WAKE_LOCK")
                    wl.acquire(10000)
                    val wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.example.cahyo_research_two_bg_service:CPU_WAKE_LOCK")
                    wl_cpu.acquire(10000)
                }

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

                val notificationBuilder = NotificationCompat.Builder(context, Constant.CHANNEL_ID)
                        .setDefaults(NotificationCompat.DEFAULT_SOUND)
                notificationBuilder.setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Message")
                        .setContentText(strMessage)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setVibrate(arrayListOf(1000L, 1000L, 1000L, 1000L).toLongArray())
                val notification = notificationBuilder.build()

                with(NotificationManagerCompat.from(context)) {
                    this.notify(Random.nextInt(0, 100), notification)
                }
            }
        }
    }
}