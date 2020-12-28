import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
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
  static const platform = const MethodChannel('com.example.flutter_channel_app/location_service');
  Timer timer;
  // Get location information
  String _locationInformation = 'Not started yet';
  String _countDownValue = 'Count Unknown';

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    timer?.cancel();
    super.dispose();
  }

  Future<void> _startTimer(){
    timer = Timer.periodic(Duration(seconds: 3), (Timer t) => {
      _getCountDown(),
      _getLocationInformation()
    });
  }

  Future<void> _getCountDown() async {
    String countDownValue;
    try {
      final String result1 = await platform.invokeMethod('countDownValues');
      countDownValue = result1;
    } on PlatformException catch (e) {
      countDownValue = "Failed to get count down value: '${e.message}'.";
    }

    setState(() {
      _countDownValue = countDownValue;
    });
  }

  Future<void> _startReadingLocation() async {
    String response;
    try {
      final String result = await platform.invokeMethod('startLocation');
      response = result;
      _startTimer();
    } on PlatformException catch (e) {
      response = "Failed to get start location reading: '${e.message}'.";
    }

    setState(() {
      _locationInformation = response;
    });
  }

  Future<void> _getLocationInformation() async {
    String locationInformation;
    try {
      final String result = await platform.invokeMethod('updateLocation');
      locationInformation = result;
    } on PlatformException catch (e) {
      locationInformation = "Failed to get location information: '${e.message}'.";
    }

    setState(() {
      _locationInformation = locationInformation;
    });
  }

  Future<dynamic> myUtilsHandler(MethodCall methodCall) async {
    switch (methodCall.method) {
      case 'foo':
        return 'some string';
      case 'bar':
        return 123.0;
      default:
        throw MissingPluginException('notImplemented');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            Text(_countDownValue),
            FlatButton(
              child: Text('Start Reading Location'),
              onPressed: _startReadingLocation,
            ),
            Text(_locationInformation),
          ],
        ),
      ),
    );
  }
}
