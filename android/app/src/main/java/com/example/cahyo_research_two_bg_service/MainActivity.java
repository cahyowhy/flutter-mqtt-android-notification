package com.example.cahyo_research_two_bg_service;

import androidx.annotation.NonNull;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.cahyo_research_two_bg_service.mqtt.MQTTConnection;
import com.example.cahyo_research_two_bg_service.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity implements ServiceConnection {

    private static final String TAG = "MainActivity";

    private NotificationService notificationService;

    private BinaryMessenger binaryMessenger;

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(this, NotificationService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (notificationService != null) {
            notificationService.setMBackgroundChannel(null);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        Log.d(TAG, "onActivityReenter");
    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        Log.d(TAG, "configureFlutterEngine");
        Storage.initValue(this);
        binaryMessenger = flutterEngine.getDartExecutor().getBinaryMessenger();

        Intent intent = new Intent(this, NotificationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        GeneratedPluginRegistrant.registerWith(flutterEngine);
        new MethodChannel(binaryMessenger, Constant.INVOKE_METHOD_ANDROID).setMethodCallHandler(new MethodChannel.MethodCallHandler() {
            @Override
            public void onMethodCall(MethodCall call, MethodChannel.Result result) {

                if (notificationService != null && notificationService.getMBackgroundChannel() == null) {
                    notificationService.setMBackgroundChannel(new MethodChannel(binaryMessenger, Constant.INVOKE_METHOD_DART));
                }

                switch (call.method) {
                    case "startMqttService":
                        doStartMqttService(call, result);
                        break;
                    case "publishMessage":
                        doPublishMessage(call, result);
                        break;
                    case "stopMqttService":
                        doStopMqttService(result);
                        break;
                    case "unsubscribeTopic":
                        doUnSubscribeTopic(call, result);
                        break;
                    case "checkMqttStatus":
                        result.success(Objects.requireNonNull(MQTTConnection.Companion.getInstance()).isConnected());
                        break;
                    case "getNotification":
                        ArrayList<String> listMessage = new ArrayList<>();
                        List notifications = ((List) Storage.getValue(Constant.NOTIFICATION_STORAGE_KEY));

                        for (Object o : notifications) {
                            listMessage.add(o.toString());
                        }

                        result.success(listMessage);
                        break;
                    case "setNotification":

                        try {
                            List params = ((List) call.arguments());
                            ArrayList<String> listMessages = new ArrayList<>();

                            for (Object o : params) {
                                listMessages.add(o.toString());
                            }

                            Storage.saveValue(Constant.NOTIFICATION_STORAGE_KEY, params);
                        } catch (Exception e) {
                            result.error("Set storage error", "Set Notification Error", null);
                        }

                        break;
                    default:
                        result.notImplemented();
                }
            }
        });
    }

    private void doStartMqttService(MethodCall call, MethodChannel.Result result) {
        Map<String, String> param = call.arguments();
        String serverUri = param.get("serverUri");
        String clientId = param.get("clientId");
        String clientUsername = param.get("clientUsername");
        String clientPassword = param.get("clientPassword");
        String subscribeTopic = param.get("subscribeTopic");

        boolean valid = serverUri != null && clientId != null && clientUsername != null &&
                clientPassword != null && subscribeTopic != null;

        if (notificationService != null && valid) {
            notificationService.doStartMqttService(serverUri, clientId, clientUsername, clientPassword, subscribeTopic);
        } else if (!valid) {
            result.error("ARGS_NULL", "Args is null", null);
        } else {
            result.error("NOTIFICATION_NULL", "Notification Service is null", null);
        }
    }

    private void doPublishMessage(MethodCall call, MethodChannel.Result result) {
        Map<String, String> params = call.arguments();

        String messages = params.get("serverUri");
        String topic = params.get("clientId");
        boolean valid = messages != null && topic != null;

        if (valid && notificationService != null) {
            notificationService.doPublishMessage(messages, topic);
            result.success(true);
        } else if (!valid) {
            result.error("INCOMPLETE ARGS", "ARGUMENT ARE INCOMPLETE", null);
        } else {
            result.error("NOTIFICATION_NULL", "Notification Service is null", null);
        }
    }

    private void doStopMqttService(MethodChannel.Result result) {
        if (notificationService != null) {
            notificationService.doStopMqttService();
            result.success(true);
        } else {
            result.error("NOTIFICATION_NULL", "Notification Service is null", null);
        }
    }

    private void doUnSubscribeTopic(MethodCall call, MethodChannel.Result result) {
        Map<String, String> param = call.arguments();
        String topic = param.get("topic");

        if (topic != null && notificationService != null) {
            notificationService.doUnSubscribeTopic(topic);
        } else if (topic == null) {
            result.error("INCOMPLETE ARGS", "ARGUMENT ARE INCOMPLETE", null);
        } else {
            result.error("NOTIFICATION_NULL", "Notification Service is null", null);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        NotificationService.LocalBinder binder = (NotificationService.LocalBinder) service;
        notificationService = binder.getService();

        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onServiceConnected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        notificationService = null;

        Log.d(TAG, "onServiceDisconnected");
    }
}
