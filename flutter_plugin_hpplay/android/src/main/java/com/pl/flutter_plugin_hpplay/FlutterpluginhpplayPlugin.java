package com.pl.flutter_plugin_hpplay;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.util.Log;
import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.hpplay.common.utils.NetworkUtil;
import com.hpplay.sdk.source.browse.api.ILelinkServiceManager;
import com.hpplay.sdk.source.browse.api.LelinkServiceInfo;
import com.hpplay.sdk.source.test.AllCast;
import com.hpplay.sdk.source.test.IUIUpdateListener;
import com.hpplay.sdk.source.test.LelinkHelper;
import com.hpplay.sdk.source.test.SDKManager;
import com.hpplay.sdk.source.test.bean.MessageDeatail;
import com.hpplay.sdk.source.test.utils.Logger;
import com.hpplay.sdk.source.test.utils.ToastUtil;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import com.hpplay.sdk.source.test.AllCast;
import com.hpplay.sdk.source.test.IUIUpdateListener;
import com.hpplay.sdk.source.test.LelinkHelper;
import com.hpplay.sdk.source.test.SDKManager;
import com.hpplay.sdk.source.test.bean.MessageDeatail;
import com.hpplay.sdk.source.test.utils.Logger;
import com.hpplay.sdk.source.test.utils.ToastUtil;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static com.hpplay.sdk.source.test.IUIUpdateListener.STATE_SEARCH_RESULT;

/**
 * FlutterpluginhpplayPlugin
 */
public class FlutterpluginhpplayPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Activity context;

    public static final int CONNECT_INIT = 1;
    public static final int CONNECT_CONNECT = 2;
    public static final int CONNECT_DISCONNECT = 3;
    public static final int CONNECT_PLAYV = 3;
    public static final int CONNECT_PLAYP = 4;
    public static final boolean searching = true;
    public static final int CODE_SEARCHING = 1;
    public static final int CODE_SEARCHED = 0;

    //连接状态
    public int mConnectType = 0;

    //搜索状态 搜索中 搜索完成
    public int mSearchedType = CODE_SEARCHED;


    public void startSearch() {

        if (null != mDelayHandler) {
            mDelayHandler.removeMessages(IUIUpdateListener.STATE_SEARCH_ING);
            mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_ING,
                    TimeUnit.SECONDS.toMillis(1));
        }

    }
//
//    public void stopSearch() {
//        Message handler = mSearchHandler.obtainMessage();
//        handler.what = CODE_SEARCHED;
//        mSearchHandler.handleMessage(handler);
//    }

    public boolean changeConnectType(int type) {
        if (mConnectType == type) {
            return false;
        }
        mSearchedType = type;//更新全局变量表示状态改变
        Map<String, Integer> map = new HashMap();
        map.put("search", type);
        sendAck(mSearchedType, map);
        return true;
    }

    public boolean changeSearchType(int type) {
        if (mSearchedType == type) {
            return false;
        }
        mSearchedType = type;//更新全局变量表示状态改变
        Map<String, Integer> map = new HashMap();
        map.put("search", type);
        sendAck(mSearchedType, map);
        return true;
    }

    public void sendAck(int act, Map data) {
        Map map = new HashMap();
        map.put("data", data);
        map.put("ack", act);
        Gson gson = new Gson();
        String toJson = gson.toJson(map);
        eventSink.success(toJson);
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flutterpluginhpplay");
        channel.setMethodCallHandler(this);
        new EventChannel(flutterPluginBinding.getBinaryMessenger(), "sample.flutter.io/test_event_channel").setStreamHandler(streamHandler);
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutterpluginhpplay");
        channel.setMethodCallHandler(new FlutterpluginhpplayPlugin());
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("initHpplay")) {
            initDates();
        } else if (call.method.equals("getNetWorkName")) {
            result.success("WiFi:" + NetworkUtil.getNetWorkName(context));
        } else if (call.method.equals("HpplayBrowse")) {
            startSearch();
        } else if (call.method.equals("HpplayConnect")) {
            String deviceIp = call.argument("LelinkServiceInfoJson");
            Log.e(TAG, deviceIp);
//            Gson gson = new Gson();
//            LelinkServiceInfo info = gson.fromJson(infoJson, LelinkServiceInfo.class);
            info = mDevicesMap.get(deviceIp);
            connect(info);
        } else if (call.method.equals("HpplayCloseConnect")) {
            disConnect();
        } else if (call.method.equals("HpplayPlay")) {
            int mediaType = call.argument("MediaType");
            String mediaUrl = call.argument("MediaUrl");
            play(mediaType, mediaUrl);
        } else if (call.method.equals("HpplayPause")) {
            pause();
        } else if (call.method.equals("HppalyResume")) {
            resume();
        } else if (call.method.equals("HpplayStop")) {
            stop();
        } else if (call.method.equals("HpplayVoulumeUp")) {
            mLelinkHelper.voulumeUp();
        } else if (call.method.equals("HpplayVoulumeDown")) {
            mLelinkHelper.voulumeDown();
        } else if (call.method.equals("HpplayGetConnectInfos")) {
            showConnetDevice();
        } else {
            result.notImplemented();
        }
    }

    //  @Override
//  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
//    channel.setMethodCallHandler(null);
//  }
    private boolean showConnetDevice() {
        List<LelinkServiceInfo> connectInfos = mLelinkHelper.getConnectInfos();
        Logger.e(TAG, connectInfos.toString());
        if (null != mLelinkHelper && null != connectInfos && !connectInfos.isEmpty()) {
            for (int i = 0; i < connectInfos.size(); i++) {
                if (info != connectInfos.get(i))
                    mLelinkHelper.disConnect(connectInfos.get(i));
                else {
                    eventSink.success(connectInfos.get(i).getName());
                    return true;
                }
            }

        } else {
//                ToastUtil.show(context, "请先连接设备");
        }
        return false;
    }

    private void disConnect() {
        List<LelinkServiceInfo> connectInfos = null;
        if (null != mLelinkHelper) {
            connectInfos = mLelinkHelper.getConnectInfos();
        }
        Logger.test(TAG, "stop click");
        if (null != mLelinkHelper && null != connectInfos && !connectInfos.isEmpty()) {
            Logger.test(TAG, "stop click");
            for (int i = 0; i < connectInfos.size(); i++) {
                boolean isDis = mLelinkHelper.disConnect(connectInfos.get(i));
            }

        } else {
            ToastUtil.show(context, "请先连接设备");
        }
    }

    private void initDates() {
        mDelayHandler = new FlutterpluginhpplayPlugin.UIHandler(context);

        mSDKManager = new SDKManager(context);
        mSDKManager.startMonitor();
        mNetworkReceiver = new FlutterpluginhpplayPlugin.NetworkReceiver(context);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(mNetworkReceiver, intentFilter);

        if (ContextCompat.checkSelfPermission(context.getApplication(),
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_DENIED
                && ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_DENIED) {
            Log.e(TAG, "有权限");
            initLelinkHelper();
        } else {
            // 若没有授权，会弹出一个对话框（这个对话框是系统的，开发者不能自己定制），用户选择是否授权应用使用系统权限
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_MUST_PERMISSION);
            Log.e(TAG, "没有权限");
            initLelinkHelper();
        }

    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        context = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }


    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }


    private static class NetworkReceiver extends BroadcastReceiver {

        private WeakReference<Activity> mReference;

        public NetworkReceiver(Activity pReference) {
            mReference = new WeakReference<>(pReference);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == mReference || null == mReference.get()) {
                return;
            }
            Activity mainActivity = mReference.get();
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equalsIgnoreCase(action) ||
                    WIFI_AP_STATE_CHANGED_ACTION.equalsIgnoreCase(action)) {
//                mainActivity.refreshWifiName();
            }
        }
    }

    private void initLelinkHelper() {
        mLelinkHelper = LelinkHelper.getInstance(context.getApplicationContext());
        mLelinkHelper.setUIUpdateListener(mUIUpdateListener);
    }

    private IUIUpdateListener mUIUpdateListener = new IUIUpdateListener() {

        @Override
        public void onUpdate(int what, MessageDeatail deatail) {
            switch (what) {
                case IUIUpdateListener.STATE_SEARCH_SUCCESS:
                    if (isFirstBrowse) {
                        isFirstBrowse = false;
                        ToastUtil.show(context, "搜索成功");
                        Log.e(TAG, "搜索成功");
                    }
                    if (null != mDelayHandler) {
                        mDelayHandler.removeMessages(what);
                        mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_SUCCESS,
                                TimeUnit.SECONDS.toMillis(1));
                    }
                    break;
                case IUIUpdateListener.STATE_SEARCH_ERROR:
                    if (null != mDelayHandler) {
                        mDelayHandler.removeMessages(what);
                        mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_SUCCESS,
                                TimeUnit.SECONDS.toMillis(1));
                    }
                    break;
                case IUIUpdateListener.STATE_SEARCH_NO_RESULT:
                    if (null != mDelayHandler) {
                        mDelayHandler.removeMessages(what);
                        mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_NO_RESULT,
                                TimeUnit.SECONDS.toMillis(1));
                    }
                    break;
                //------------------------------------------------------------------------------------
                case IUIUpdateListener.STATE_CONNECT_SUCCESS:
                    Log.e("pl123", "onConnect3");
                    if (null != mDelayHandler) {
                        Log.e("pl123", "onConnect3。1");
                        mDelayHandler.removeMessages(what);
                        Message message = new Message();
                        message.obj = deatail.text;
                        message.what = IUIUpdateListener.STATE_CONNECT_SUCCESS;
                        mDelayHandler.sendMessageDelayed(message,
                                TimeUnit.SECONDS.toMillis(1));
                        Log.e("pl123", "onConnect3。2");
                    }
                    break;
                case IUIUpdateListener.STATE_DISCONNECT:
                    if (null != mDelayHandler) {
                        mDelayHandler.removeMessages(what);
                        Message message = new Message();
                        message.obj = deatail.text;
                        message.what = IUIUpdateListener.STATE_DISCONNECT;
                        mDelayHandler.sendMessageDelayed(message,
                                TimeUnit.SECONDS.toMillis(1));
                    }
                    break;
                case IUIUpdateListener.STATE_CONNECT_FAILURE:
                    Logger.test(TAG, "connect failure:" + deatail.text);
                    Log.e(TAG, "ToastUtil " + deatail.text);
                    ToastUtil.show(context, deatail.text);
//                    mBrowseAdapter.setSelectInfo(null);
//                    mBrowseAdapter.notifyDataSetChanged();
//                    // 更新列表
//                    updateConnectAdapter();
                    break;
                case IUIUpdateListener.STATE_PLAY:
                    Logger.test(TAG, "callback play");
//                    isPause = false;
                    Log.e(TAG, "ToastUtil 开始播放");
                    ToastUtil.show(context, "开始播放");
                    break;
                case IUIUpdateListener.STATE_LOADING:
                    Logger.test(TAG, "callback loading");
//                    isPause = false;
                    Log.e(TAG, "ToastUtil 开始加载");
                    ToastUtil.show(context, "开始加载");
                    break;
                case IUIUpdateListener.STATE_PAUSE:
                    Logger.test(TAG, "callback pause");
                    Log.e(TAG, "ToastUtil 暂停播放");
                    ToastUtil.show(context, "暂停播放");
//                    isPause = true;
                    break;
                case IUIUpdateListener.STATE_STOP:
                    Logger.test(TAG, "callback stop");
//                    isPause = false;
                    Log.e(TAG, "ToastUtil 播放结束");
                    ToastUtil.show(context, "播放结束");
                    break;
                case IUIUpdateListener.STATE_SEEK:
                    Logger.test(TAG, "callback seek:" + deatail.text);
                    Logger.d(TAG, "ToastUtil seek完成:" + deatail.text);
                    ToastUtil.show(context, "seek完成" + deatail.text);
                    break;
                case IUIUpdateListener.STATE_PLAY_ERROR:
                    Logger.test(TAG, "callback error:" + deatail.text);
                    ToastUtil.show(context, "播放错误：" + deatail.text);
                    break;
                case IUIUpdateListener.STATE_POSITION_UPDATE:
                    Logger.test(TAG, "callback position update:" + deatail.text);
                    long[] arr = (long[]) deatail.obj;
                    long duration = arr[0];
                    long position = arr[1];
                    Logger.d(TAG, "ToastUtil 总长度：" + duration + " 当前进度:" + position);
//                    mProgressBar.setMax((int) duration);
//                    mProgressBar.setProgress((int) position);
                    break;
                case IUIUpdateListener.STATE_COMPLETION:
                    Logger.test(TAG, "callback completion");
                    Logger.d(TAG, "ToastUtil 播放完成");
                    ToastUtil.show(context, "播放完成");
                    break;
                case IUIUpdateListener.STATE_INPUT_SCREENCODE:
                    Logger.test(TAG, "input screencode");
                    ToastUtil.show(context, deatail.text);
//                    showScreenCodeDialog();
                    break;
                case IUIUpdateListener.RELEVANCE_DATA_UNSUPPORT:
                    Logger.test(TAG, "unsupport relevance data");
                    ToastUtil.show(context, deatail.text);
                    break;
                case IUIUpdateListener.STATE_SCREENSHOT:
                    Logger.test(TAG, "unsupport relevance data");
                    ToastUtil.show(context, deatail.text);
                    break;
            }
        }

    };

    private void browse() {

        if (null != mLelinkHelper) {

            //            boolean isLelinkOpen = mSwitchLeLink.isChecked();
            //            boolean isDLNAOpen = mSwitchDLNA.isChecked();
            boolean isLelinkOpen = true;
            boolean isDLNAOpen = true;

            int type;
            String text;
            if (isLelinkOpen && isDLNAOpen) {
                text = "All";
                type = ILelinkServiceManager.TYPE_ALL;
            } else if (isLelinkOpen) {
                text = "Lelink";
                type = ILelinkServiceManager.TYPE_LELINK;
            } else if (isDLNAOpen) {
                text = "DLNA";
                type = ILelinkServiceManager.TYPE_DLNA;
            } else {
                text = "All";
                type = ILelinkServiceManager.TYPE_ALL;
            }
            Logger.test(TAG, "browse type:" + text);
            Log.e(TAG, "browse type:" + text);
            if (!isFirstBrowse) {
                isFirstBrowse = true;
            }
            mLelinkHelper.browse(type);
        } else {
            ToastUtil.show(context, "权限不够");
            if (null != mDelayHandler) {
                mDelayHandler.removeCallbacksAndMessages(null);
                mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_SUCCESS,
                        TimeUnit.SECONDS.toMillis(1));
            }
        }
    }

    private void stopBrowse() {
        Logger.test(TAG, "btn_stop_browse click");
        if (null != mLelinkHelper) {
            Logger.test(TAG, "stop browse");
            Logger.d(TAG, "stop browse");
            isFirstBrowse = false;
            mLelinkHelper.stopBrowse();
        } else {
            ToastUtil.show(context, "未初始化");
        }
    }

    private void connect(LelinkServiceInfo info) {
        if (null == mLelinkHelper) {
            ToastUtil.show(context, "未初始化");
            return;
        }
        List<LelinkServiceInfo> connectInfos = mLelinkHelper.getConnectInfos();
        if (null != connectInfos && !connectInfos.isEmpty()) {
//            Logger.test(TAG, "stop click");
//            for (int i = 0; i < connectInfos.size(); i++) {
//                if (info != connectInfos.get(i)) {
//                    mLelinkHelper.disConnect(connectInfos.get(i));
//                    Logger.test(TAG, "disConnect 000000:" + connectInfos.get(i).getName());
//                }
//
//            }
//            ToastUtil.show(context, "选中了:" + info.getName()
//                    + " type:" + info.getTypes());
            Logger.test(TAG, "还有没有断开的连接 connect:" + connectInfos.toString());
//            mLelinkHelper.connect(info);
        } else {
            mLelinkHelper.connect(info);
            ToastUtil.show(context, "connect");
        }
        Logger.test(TAG, "connect click:" + info.getName());
    }

    private void play(int mediaType, String url) {
        isPlayMirror = false;
        if (null == mLelinkHelper) {
            ToastUtil.show(context, "未初始化或未选择设备");
            return;
        }
        List<LelinkServiceInfo> connectInfos = mLelinkHelper.getConnectInfos();
        Logger.e(TAG, "connect click:" + connectInfos.toString());
        if (null == connectInfos || connectInfos.isEmpty()) {
            ToastUtil.show(context, "请先连接设备");
            return;
        }
//        if (isPause) {
//            Logger.test(TAG, "resume click");
//            isPause = false;
//            // 暂停中
//            mLelinkHelper.resume();
//            return;
//        } else {
//            Logger.test(TAG, "play click");
//        }
//        int checkedId = mRadioGroup.getCheckedRadioButtonId();
//        int mediaType = 0;
        String mediaTypeStr = null;
        boolean isLocalFile = false;
//        String url = null;
        switch (mediaType) {
            case AllCast.MEDIA_TYPE_VIDEO:
//                url = mEtNetVideo.getText().toString();
                mediaTypeStr = "NetVideo";
                break;
            case AllCast.MEDIA_TYPE_AUDIO:
//                url = mEtNetMusic.getText().toString();
                mediaTypeStr = "NetMusic";
                break;
            case AllCast.MEDIA_TYPE_IMAGE:
                mediaType = AllCast.MEDIA_TYPE_IMAGE;
//                url = mEtNetPicture.getText().toString();
                mediaTypeStr = "NetPicture";
                break;
//            case AllCast.MEDIA_TYPE_VIDEO:
//                mediaType = AllCast.MEDIA_TYPE_VIDEO;
//                isLocalFile = true;
////                url = mEtLocalVideo.getText().toString();
//                mediaTypeStr = "LocalVideo";
//                break;
//            case AllCast.MEDIA_TYPE_AUDIO:
//                mediaType = AllCast.MEDIA_TYPE_AUDIO;
//                isLocalFile = true;
////                url = mEtLocalMusic.getText().toString();
//                mediaTypeStr = "LocalMusic";
//                break;
//            case AllCast.MEDIA_TYPE_IMAGE:
//                mediaType = AllCast.MEDIA_TYPE_IMAGE;
//                isLocalFile = true;
////                url = mEtLocalPicture.getText().toString();
//                mediaTypeStr = "LocalPicture";
//                break;
        }
        Logger.test(TAG, "start play url:" + url + " type:" + mediaTypeStr);
        if (isLocalFile) {
            // 本地media
            mLelinkHelper.playLocalMedia(url, mediaType, "");
        } else {
            // 网络media
            mLelinkHelper.playNetMedia(url, mediaType, "");
        }

    }

    private void pause() {
        List<LelinkServiceInfo> connectInfos = null;
        if (null != mLelinkHelper) {
            connectInfos = mLelinkHelper.getConnectInfos();
        }
        if (null != mLelinkHelper && null != connectInfos && !connectInfos.isEmpty()) {
            Logger.test(TAG, "pause click");
            mLelinkHelper.pause();
        }
    }

    private void resume() {
        mLelinkHelper.resume();
    }

    private void stop() {
        List<LelinkServiceInfo> connectInfos = null;
        if (null != mLelinkHelper) {
            connectInfos = mLelinkHelper.getConnectInfos();
        }
        if (null != mLelinkHelper && null != connectInfos && !connectInfos.isEmpty()) {
//            Logger.test(TAG, "stop click");
            mLelinkHelper.stop();
        } else {
//            ToastUtil.show(mContext, "请先连接设备");
        }
    }

    private void updateBrowseAdapter() {
        if (null != mLelinkHelper) {
            List<LelinkServiceInfo> infos = mLelinkHelper.getInfos();
            HashMap map = new HashMap<String, LelinkServiceInfo>();
            for (int i = 0; i < infos.size(); i++) {
                map.put(infos.get(i).getIp(), infos.get(i));
            }
            mDevicesMap.clear();
            mDevicesMap.putAll(map);
            if (eventSink != null) {
                HashMap map1 = new HashMap<String, String>();
                Gson gson = new Gson();
                map1.put("data", gson.toJson(infos));
                map1.put("ack", STATE_SEARCH_RESULT);
                String toJson = gson.toJson(map1);
                eventSink.success(toJson);
            } else {
                Log.e(TAG, "error :eventSink not init");
            }
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    private static String TAG = "FlutterpluginhpplayPlugin";
    HashMap<String, LelinkServiceInfo> mDevicesMap = new HashMap<String, LelinkServiceInfo>();
    LelinkServiceInfo info;
    //事件派发
    private EventChannel.EventSink eventSink = null;
    private EventChannel.StreamHandler streamHandler = new EventChannel.StreamHandler() {
        @Override
        public void onListen(Object o, EventChannel.EventSink sink) {
            eventSink = sink;
        }

        @Override
        public void onCancel(Object o) {
            eventSink = null;
        }
    };

    private SDKManager mSDKManager;
    private boolean isFirstBrowse = true;
    //    private boolean isPause = false;
    private boolean isPlayMirror;
    private FlutterpluginhpplayPlugin.UIHandler mDelayHandler;


    ///这个handler 负责回馈状态到flutter
    // 只有搜索状态只有改变时 才通知flutter 状态改变
    // 搜索只要有结果    就通知flutter 更新列表
    // 只有搜索完成到开始搜索 才进行搜索
    private class UIHandler extends Handler {

        private WeakReference<Activity> mReference;

        UIHandler(Activity reference) {
            mReference = new WeakReference<>(reference);
        }


        @Override
        public void handleMessage(Message msg) {
            boolean b = false;
            if (msg.what == IUIUpdateListener.STATE_SEARCH_SUCCESS || msg.what == IUIUpdateListener.STATE_SEARCH_ING) {
                b = changeSearchType(msg.what);
            } else {
                changeConnectType(msg.what);
            }

            Activity mainActivity = mReference.get();
            if (mainActivity == null) {
                return;
            }
            switch (msg.what) {
                case IUIUpdateListener.STATE_SEARCH_SUCCESS:
                    updateBrowseAdapter();//更新列表
                    break;
                case IUIUpdateListener.STATE_SEARCH_ING:
                    if (b) browse();
                    break;
                case IUIUpdateListener.STATE_SEARCH_NO_RESULT: {
                    Map map = new HashMap();
                    map.put("ack", IUIUpdateListener.STATE_SEARCH_NO_RESULT);
                    Gson gson = new Gson();
                    String toJson = gson.toJson(map);
                    eventSink.success(toJson);
                    break;
                }

                case IUIUpdateListener.STATE_CONNECT_SUCCESS: {
                    Map map = new HashMap();
                    map.put("ack", IUIUpdateListener.STATE_CONNECT_SUCCESS);
                    map.put("data", msg.obj);
                    Gson gson = new Gson();
                    String toJson = gson.toJson(map);
                    eventSink.success(toJson);
                    break;
                }

                case IUIUpdateListener.STATE_DISCONNECT:
                    Map map = new HashMap();
                    map.put("ack", IUIUpdateListener.STATE_CONNECT_SUCCESS);
//                    map.put("data", msg.obj);
                    Gson gson = new Gson();
                    String toJson = gson.toJson(map);
                    eventSink.success(toJson);
                    break;
            }

            super.handleMessage(msg);
        }
    }


    // SDK
    private LelinkHelper mLelinkHelper;
    private static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private static final String NET_VIDEO_URL = "https://v.mifile.cn/b2c-mimall-media/ed921294fb62caf889d40502f5b38147.mp4";
    private static final String NET_MUSIC_URL = "http://music.163.com/song/media/outer/url?id=287248.mp3";
    private static final String NET_PICTURE_URL = "http://news.cri.cn/gb/mmsource/images/2013/06/23/2/2211679758122940818.jpg";
    private static final String LOCAL_MEDIA_PATH = "/hpplay_demo/local_media/";
    private static final int REQUEST_MUST_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 4;

    private FlutterpluginhpplayPlugin.NetworkReceiver mNetworkReceiver;

}
