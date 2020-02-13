import 'dart:async';

import 'package:flutter/services.dart';

class FlutterPluginHpplay {
  static const MethodChannel _channel =
      const MethodChannel('flutter_plugin_hpplay');

  static final int TYPE_AUDIO = 101;
  static final int TYPE_VIDEO = 102;
  static final int TYPE_IMAGE = 103;

  StreamSubscription<dynamic> _streamSubscription;

  static Future<String> get HpplayCloseConnect async {
    await _channel.invokeMethod('HpplayCloseConnect');
  }

  static Future<String> get HpplayGetConnectInfos async {
    await _channel.invokeMethod('HpplayGetConnectInfos');
  }

  static Future<String> get getNetWorkName async {
    final String version = await _channel.invokeMethod('getNetWorkName');
    return version;
  }

  static Future<String> get initHpplay async {
    await _channel.invokeMethod('initHpplay');
  }

  static Future<String> get HpplayBrowse async {
    final String version = await _channel.invokeMethod('HpplayBrowse');
    return version;
  }

  static Future HpplayConnect(String ip) async {
    Map map = new Map();
    map['LelinkServiceInfoJson'] = ip;
    final String version = await _channel.invokeMethod('HpplayConnect', map);
  }

  void initEvent(eventListener) {
    print("init event");
    _streamSubscription = _eventChannelFor()
        .receiveBroadcastStream()
        .listen(eventListener, onError: errorListener);
  }

  void errorListener(dynamic event) {}

  EventChannel _eventChannelFor() {
    return EventChannel('flutter_plugin_event_discover_device');
  }

  static Future<void> HpplayPlayVideo(String url) async {
    Map map = new Map();
    map['MediaUrl'] = url;
    map['MediaType'] = TYPE_VIDEO;
    final String version = await _channel.invokeMethod('HpplayPlay', map);
  }

  static Future<void> HpplayPlayAudio(String url) async {
    Map map = new Map();
    map['MediaUrl'] = url;
    map['MediaType'] = TYPE_AUDIO;
    final String version = await _channel.invokeMethod('HpplayPlay', map);
  }

  static Future<void> HpplayPlayImage(String url) async {
    Map map = new Map();
    map['MediaUrl'] = url;
    map['MediaType'] = TYPE_IMAGE;
    final String version = await _channel.invokeMethod('HpplayPlay', map);
  }

  static Future<void> get HpplayPause async {
    final String version = await _channel.invokeMethod('HpplayPause');
  }

  static Future<void> get HppalyResume async {
    final String version = await _channel.invokeMethod('HppalyResume');
  }

  static Future<void> get HpplayStop async {
    final String version = await _channel.invokeMethod('HpplayStop');
  }

  static Future<void> get HpplayVoulumeUp async {
    final String version = await _channel.invokeMethod('HpplayVoulumeUp');
  }

  static Future<void> get HpplayVoulumeDown async {
    final String version = await _channel.invokeMethod('HpplayVoulumeDown');
  }
}
