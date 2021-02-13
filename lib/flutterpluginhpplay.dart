import 'dart:async';

import 'package:flutter/services.dart';

class Flutterpluginhpplay {
  static const MethodChannel _channel =
      const MethodChannel('flutterpluginhpplay');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static final int CHECKEDID_NET_VIDEO = 0;
  static final int CHECKEDID_NET_MUSIC = 1;
  static final int CHECKEDID_NET_PICTURE = 2;
  static final int CHECKEDID_LOCAL_VIDEO = 3;
  static final int CHECKEDID_LOCAL_MUSIC = 4;
  static final int CHECKEDID_LOCAL_PICTURE = 5;

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

  static Future<bool> get initHpplay async {
    bool inint = await _channel.invokeMethod('initHpplay');
    return inint;
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
    map['MediaType'] = CHECKEDID_NET_VIDEO;
    final String version = await _channel.invokeMethod('HpplayPlay', map);
  }

  static Future<void> HpplaySeekTo(int position) async {
    Map map = new Map();
    map['position'] = position;
    final String version = await _channel.invokeMethod('HpplaySeekTo', map);
  }

  static Future<void> HpplayPlayAudio(String url) async {
    Map map = new Map();
    map['MediaUrl'] = url;
    map['MediaType'] = CHECKEDID_NET_MUSIC;
    final String version = await _channel.invokeMethod('HpplayPlay', map);
  }

  static Future<void> HpplayPlayImage(String url) async {
    Map map = new Map();
    map['MediaUrl'] = url;
    map['MediaType'] = CHECKEDID_NET_PICTURE;
    final String version = await _channel.invokeMethod('HpplayPlay', map);
  }

  static Future<void> HpplayPlay(int type, String url) async {
    Map map = new Map();
    map['MediaUrl'] = url;
    map['MediaType'] = type;
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
