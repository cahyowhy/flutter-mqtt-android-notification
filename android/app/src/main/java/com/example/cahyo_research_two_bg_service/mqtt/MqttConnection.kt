package com.example.cahyo_research_two_bg_service.mqtt

import android.content.Context
import android.util.Log
import com.example.cahyo_research_two_bg_service.Constant
import com.example.cahyo_research_two_bg_service.Storage
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.nio.charset.StandardCharsets


class MQTTConnection {

    private var mqttAndroidClient: MqttAndroidClient? = null

    private val TAG: String = "MQTTConnection"

    private val topics = mutableListOf<String>()

    fun hasTopic(topic: String): Boolean {
        return topics.contains(topic)
    }

    fun doAssignMqttAndroidClient(context: Context, serverUri: String, clientId: String) {
        mqttAndroidClient = MqttAndroidClient(context, serverUri, clientId)
    }

    fun doConnect(context: Context, serverUri: String, clientId: String, clientUsername: String, clientPassword: String, callback: IMqttCallback) {
        doAssignMqttAndroidClient(context, serverUri, clientId)
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false
        mqttConnectOptions.userName = clientUsername
        mqttConnectOptions.password = clientPassword.toCharArray()
        mqttConnectOptions.maxInflight = 1000

        try {
            mqttAndroidClient?.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient?.setBufferOpts(disconnectedBufferOptions)
                    callback.onMQTTConnectionSuccess()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e(TAG, "Connection lost: ${exception.message}", exception)
                    callback.onMQTTConnectionFailed(exception.message)
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun doDisConnect(deleteStorage: Boolean = false, iMqttCallback: IMqttCallback? = null) {
        if (deleteStorage) {
            Storage.deleteValue(Constant.MQTTOPTIONS_STORAGE_KEY)
        }

        try {
            if (mqttAndroidClient != null) {

                mqttAndroidClient?.unregisterResources()
                mqttAndroidClient?.close()

                if (iMqttCallback != null) {
                    /**
                     * call this bitch on onDestroy will throw err
                     */
                    mqttAndroidClient?.disconnect(null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d("MQTT", "Disconnect successfully!")
                            iMqttCallback.onMQTTDisconnectSuccess()
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.d("MQTT", "Disconnect failure!")
                            iMqttCallback.onMQTTDisconnectFailed(exception?.message)
                        }
                    })
                }

                mqttAndroidClient = null

                Log.d(TAG, "Disconnect")

                topics.clear()
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    val isConnected: Boolean
        get() = mqttAndroidClient?.isConnected ?: false

    fun doMessageListen(callback: IMqttCallback) {
        mqttAndroidClient?.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {}
            override fun connectionLost(cause: Throwable) {}

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                callback.onMQTTMessagesArrived(message)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })
    }

    fun doSubscribeTopic(topic: String, callback: IMqttCallback) {
        try {
            topics.add(topic)

            mqttAndroidClient?.subscribe(topic, 2, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.i(TAG, "subscribed")
                    callback.onMQTTSubscribeTopicSuccess()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e(TAG, "unsubscribed failed: ${exception.message}")
                    callback.onMQTTSubscribeTopicFailed(exception.message)
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    @Throws(MqttException::class)
    fun doUnSubscribeTopic(topic: String, callback: IMqttCallback) {
        val token = mqttAndroidClient?.unsubscribe(topic)
        token?.actionCallback = object : IMqttActionListener {
            override fun onSuccess(iMqttToken: IMqttToken) {
                topics.remove(topic)
                callback.onMQTTUnsubscribeSuccess()
            }

            override fun onFailure(iMqttToken: IMqttToken, throwable: Throwable) {
                callback.onMQTTUnsubscribeFailed()
            }
        }
    }

    companion object {
        var instance: MQTTConnection? = null
            get() {
                if (field == null) {
                    field = MQTTConnection()
                }

                return field
            }
            private set
    }

    fun doPublishMessages(messages: String, topic: String) {
        try {
            val encodedPayload = messages.toByteArray(StandardCharsets.UTF_8)
            val message = MqttMessage(encodedPayload)
            message.isRetained = true
            message.qos = 1

            if (mqttAndroidClient != null && isConnected) {
                mqttAndroidClient?.publish(topic, message)
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}