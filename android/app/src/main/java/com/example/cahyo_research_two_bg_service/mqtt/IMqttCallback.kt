package com.example.cahyo_research_two_bg_service.mqtt

import org.eclipse.paho.client.mqttv3.MqttMessage

interface IMqttCallback {
    fun onMQTTConnectionSuccess()

    fun onMQTTSubscribeTopicSuccess()

    fun onMQTTSubscribeTopicFailed(messages: String?)

    fun onMQTTConnectionFailed(messages: String?)

    fun onMQTTMessagesArrived(mqttMessage: MqttMessage?)

    fun onMQTTUnsubscribeSuccess()

    fun onMQTTUnsubscribeFailed()

    fun onMQTTDisconnectSuccess()

    fun onMQTTDisconnectFailed(messages: String?)
}