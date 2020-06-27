import 'dart:async';
import 'dart:convert';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const methodChannel =
      const MethodChannel('com.example.research_cahyo/method');
  static const methodBackgroundChannel =
      const MethodChannel('com.example.research_cahyo/method_dart');

  bool isConected = false;

  String payloadPublishMesage =
      '{"uuid":"1592301341321","messageType":"ORDER","payload":{"paymentType":{"id":5,"type":"CASH","accountName":"Pembayaran Tunai","outletId":5},"outletId":5,"user":{"resource":"MOBILE2_CASHIER","id":142,"username":"cahyo@dapur.com","profileName":"Cahyo Dapur","pin":"0000","role":"ROLE_OUTLET","outlets":[{"id":5,"name":"HanStore"}],"outletIdSelected":5,"loginAs":"CASHIER","cashlezUsername":"Saless002yy","cashlezPin":"240196","outletNameSelected":"HanStore","ppn":10.0},"payment":false,"senderId":"59c03df4aa25a624","serveBy":"Cahyo Dapur","cash":0,"subTotal":4000,"change":0,"orderDetails":[{"packets":[],"modifiers":[],"images":[],"note":"","productImage":"","productId":2667,"clientId":"0de739a9-8a14-58ca-a085-1a1f24c0b400","productName":"Teh Botol Sosro","quantity":1.0,"price":4000.0,"unitOfPurchase":"Krat","unitOfSale":"Botol","conversion":24.0,"inInventory":true,"hasRecipe":false,"outletId":5,"nominalPromo":0.0,"isRevise":false,"isPacket":false,"isModifier":false,"isVariant":false}],"createdId":142,"grandTotal":4000,"paymentTypeId":5,"promoDiscount":0,"reservationId":0,"salesDiscount":0,"salesVoucher":0,"serviceCharge":0,"serviceTypeId":0,"shiftId":678,"transactionTime":"Jun 16, 2020 04:55:40 PM","dueDate":"","customerName":"tttt","transactionId":"160620#dRG1","ppn":400},"expanded":false,"read":false,"hide":false}';

  var mqttConnectOptions;

  List<Map<String, dynamic>> _messageResults = [];

  void _connectMqtt() {
    try {
      methodChannel.invokeMethod("startMqttService", mqttConnectOptions);
    } on PlatformException catch (e) {
      print(e);
    }
  }

  @override
  void initState() {
    super.initState();
    methodBackgroundChannel.setMethodCallHandler(_onMethodInvoked);

    mqttConnectOptions = {
      "serverUri": "ws://maqiatto.com:8883/mqtt",
      "clientId": randomString(),
      "clientUsername": "cahyowhy01@gmail.com",
      "clientPassword": "Cahyowhy123",
      "subscribeTopic": "cahyowhy01@gmail.com/mytopic2",
    };

    Future.delayed(Duration.zero).then((_) => _onGetMessages());
  }

  void _onGetMessages() async {
    try {
      var messages = await methodChannel.invokeMethod("getNotification");

      if (messages != null && messages is List) {
        setState(() {
          messages.forEach((message) {
            _messageResults.add(
                Map<String, dynamic>.from(json.decode(message.toString())));
          });
        });
      }
    } on PlatformException catch (e) {
      print(e);
    }
  }

  Future<dynamic> _onMethodInvoked(MethodCall call) async {
    switch (call.method) {
      case "mqttSubscribeFailed":
      case "mqttConnectFailed":
      case "mqttUnsubscribeFailed":
      case "mqttUnsubscribeSuccess":
        print(call.arguments);
        break;
      case "mqttMessageArrive":
        _onMessageReceived(call.arguments as String);
        break;
    }
  }

  void _publishMessage() {
    try {
      var mqttOptionsSubscribe = {
        "messages": payloadPublishMesage,
        "topic": 'cahyowhy01@gmail.com/mytopic',
      };

      methodChannel.invokeMethod("publishMessage", mqttOptionsSubscribe);
    } on PlatformException catch (e) {
      print(e);
    }
  }

  void _disconnectMqtt() async {
    try {
      methodChannel.invokeMethod("stopMqttService");
    } on PlatformException catch (e) {
      print(e);
    }
  }

  void _onMessageReceived(message) {
    setState(() =>
        _messageResults.add(Map<String, dynamic>.from(json.decode(message))));
  }

  String randomString({int strlen = 12}) {
    const chars =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    Random rnd = new Random(new DateTime.now().microsecondsSinceEpoch);
    String result = "";

    for (var i = 0; i < strlen; i++) {
      result += chars[rnd.nextInt(chars.length)];
    }

    return result;
  }

  dynamic mapGet(Map map, List path, {var defaultValue = "-"}) {
    assert(path.length > 0);
    var m = map ?? const {};
    for (int i = 0; i < path.length - 1; i++) {
      m = m[path[i]] ?? const {};
    }

    return m[path.last] ?? defaultValue;
  }

  _onCheckConnected() async {
    try {
      var isConnected = await methodChannel.invokeMethod("checkMqttStatus");

      print(isConnected);
    } on PlatformException catch (e) {
      print(e);
    }
  }

  _onDeleteNotif(int index) {
    setState(() => _messageResults.removeAt(index));

    try {
      methodChannel.invokeMethod("setNotification",
          _messageResults.map((item) => json.encode(item)).toList());
    } on PlatformException catch (e) {
      print(e);
    }
  }

  @override
  Widget build(BuildContext context) {
    var timerCard = Card(
        child: Column(mainAxisSize: MainAxisSize.min, children: <Widget>[
      const ListTile(
        leading: const Icon(Icons.timer),
        title: const Text('Event and Method Channel Sample'),
        subtitle: const Text(
            'An example application showing off the communications between Flutter and native Android.'),
      ),
      ListView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemBuilder: (BuildContext context, int index) {
          var message = _messageResults[index];
          var customerName = mapGet(message, ["payload", "customerName"],
              defaultValue: "No name");

          var grandTotal =
              mapGet(message, ["payload", "grandTotal"], defaultValue: "0");

          var paymentType = mapGet(
              message, ["payload", "paymentType", "accountName"],
              defaultValue: "Cash");

          var orderDetails = (mapGet(message, ["payload", "orderDetails"],
                  defaultValue: []) as List)
              .fold(
                  "",
                  (accu, item) =>
                      accu += mapGet(item, ["productName"], defaultValue: "-"));

          return ListTile(
              trailing: IconButton(
                  icon: Icon(Icons.delete),
                  onPressed: () => _onDeleteNotif(index)),
              leading: const Icon(Icons.notifications),
              subtitle: Text(
                  "$customerName, $paymentType, $grandTotal, $orderDetails"),
              title: Text(
                  mapGet(message, ["messageType"], defaultValue: "Title")));
        },
        itemCount: _messageResults.length,
      ),
      ButtonBar(children: <Widget>[
        RaisedButton(
          child: const Text('Connect MQTT'),
          onPressed: _connectMqtt,
        ),
        RaisedButton(
          child: const Text('DisConnect MQTT'),
          onPressed: _disconnectMqtt,
        ),
        RaisedButton(
          child: const Text('Publish Message'),
          onPressed: _publishMessage,
        ),
        RaisedButton(
          child: const Text('Check is connected'),
          onPressed: _onCheckConnected,
        ),
      ]),
    ]));

    return Scaffold(
        appBar: AppBar(
          title: Text(widget.title),
        ),
        body: SingleChildScrollView(
          child: Container(
            padding: EdgeInsets.all(8.0),
            child: timerCard,
          ),
        ));
  }
}
