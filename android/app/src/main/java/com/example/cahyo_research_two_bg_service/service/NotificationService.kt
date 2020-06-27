package com.example.cahyo_research_two_bg_service.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.cahyo_research_two_bg_service.Constant
import com.example.cahyo_research_two_bg_service.Util
import com.example.cahyo_research_two_bg_service.mqtt.IMqttCallback
import com.example.cahyo_research_two_bg_service.mqtt.MQTTConnection
import io.flutter.plugin.common.MethodChannel
import org.eclipse.paho.client.mqttv3.MqttMessage

class NotificationService : Service(), IMqttCallback {

    var mBackgroundChannel: MethodChannel? = null

    private var subsribeTopic: String? = null

    private val binder = LocalBinder()

    companion object {
        const val MQTT_CONNECT = "connect"
        const val MQTT_DISCONNECT = "disconnect"
        const val TAG = "NOTIFICATION_SERVICE";
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): NotificationService = this@NotificationService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Invoked")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(Constant.CHANNEL_ID, Constant.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT)

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val notification: Notification = Notification.Builder(applicationContext, Constant.CHANNEL_ID).build()
            startForeground(Constant.NOTIFICATION_ID_FOREGROUND, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand, " + "do i have intent ? ${if (intent != null) "yes" else "no"}")

        return super.onStartCommand(intent, flags, startId)
    }

    fun doStartMqttService(serverUri: String, clientId: String, clientUsername: String,
                           clientPassword: String, subsribeTopic: String) {
        this.subsribeTopic = subsribeTopic
        MQTTConnection.instance?.doConnect(this, serverUri, clientId, clientUsername, clientPassword, this)
    }

    fun doPublishMessage(message: String, topic: String) {
        MQTTConnection.instance?.doPublishMessages(message, topic)
    }

    fun doUnSubscribeTopic(topic: String) {
        MQTTConnection.instance?.doUnSubscribeTopic(topic, this)
    }

    fun doStopMqttService() {
        MQTTConnection.instance?.doDisConnect(true, this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onMQTTConnectionSuccess() {
        val subscribeTopic = this.subsribeTopic

        if (subscribeTopic != null && !(MQTTConnection.instance?.hasTopic(subscribeTopic)
                        ?: false)) {
            MQTTConnection.instance?.doSubscribeTopic(subscribeTopic, this)
        }
    }

    override fun onMQTTSubscribeTopicSuccess() {
        MQTTConnection.instance?.doMessageListen(this)
    }

    override fun onMQTTSubscribeTopicFailed(messages: String?) {
        mBackgroundChannel?.invokeMethod("mqttSubscribeFailed", messages)
    }

    override fun onMQTTConnectionFailed(messages: String?) {
        mBackgroundChannel?.invokeMethod("mqttConnectFailed", messages)
    }

    override fun onMQTTMessagesArrived(mqttMessage: MqttMessage?) {
        Util.showNotification(this, mqttMessage, mBackgroundChannel)
    }

    override fun onMQTTUnsubscribeSuccess() {
        mBackgroundChannel?.invokeMethod("mqttUnsubscribeSuccess", "Mqtt unsubscribe success")
    }

    override fun onMQTTUnsubscribeFailed() {
        mBackgroundChannel?.invokeMethod("mqttUnsubscribeFailed", "Mqtt unsubscribe failed")
    }

    override fun onMQTTDisconnectSuccess() {
        mBackgroundChannel?.invokeMethod("mqttDisconnectSuccess", "Mqtt disconnect success")
    }

    override fun onMQTTDisconnectFailed(messages: String?) {
        mBackgroundChannel?.invokeMethod("mqttDisconnectFailed", messages
                ?: "Mqtt disconnect failed")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d(TAG, "onRebind")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved")
    }
}