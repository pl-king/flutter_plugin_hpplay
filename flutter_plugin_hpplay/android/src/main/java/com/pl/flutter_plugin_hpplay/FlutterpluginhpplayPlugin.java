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
import android.text.TextUtils;
import android.util.Log;

import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hpplay.common.utils.NetworkUtil;
import com.hpplay.sdk.source.api.IBindSdkListener;
import com.hpplay.sdk.source.api.IConnectListener;
import com.hpplay.sdk.source.api.ILelinkPlayerListener;
import com.hpplay.sdk.source.api.LelinkPlayerInfo;
import com.hpplay.sdk.source.api.LelinkSourceSDK;
import com.hpplay.sdk.source.browse.api.IBrowseListener;
import com.hpplay.sdk.source.browse.api.ILelinkServiceManager;
import com.hpplay.sdk.source.browse.api.LelinkServiceInfo;
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


/**
 * FlutterpluginhpplayPlugin
 */
public class FlutterpluginhpplayPlugin extends Activity implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Activity context;
    private UIHandler mUiHandler;


    private static final int MSG_SEARCH_EMPTY = 98;
    private static final int MSG_SEARCH_FAILED = 99;
    private static final int MSG_SEARCH_RESULT = 100;

    private static final int MSG_CONNECT_FAILURE = 101;
    private static final int MSG_CONNECT_SUCCESS = 102;
    private static final int MSG_UPDATE_PROGRESS = 103;
    private static final int MSG_PLAY_STATE_START = 120;
    private static final int MSG_PLAY_STATE_LOADING = 121;
    private static final int MSG_PLAY_STATE_PAUSE = 122;
    private static final int MSG_PLAY_STATE_COMPLETION = 123;
    private static final int MSG_PLAY_STATE_STOP = 124;
    private static final int MSG_PLAY_STATE_SEEKCOMPLETE = 125;
    private static final int MSG_PLAY_STATE_ERROR = 126;

//    private static final int

    private static final String APP_ID = "12688";
    private static final String APP_SECRET = "8914d933be4c002e3b49cd628eb9ead5";
    private boolean isPause;
//    public static final int CONNECT_INIT = 1;
//    public static final int CONNECT_CONNECT = 2;
//    public static final int CONNECT_DISCONNECT = 3;
//    public static final int CONNECT_PLAYV = 3;
//    public static final int CONNECT_PLAYP = 4;
//    public static final boolean searching = true;
//    public static final int CODE_SEARCHING = 1;
//    public static final int CODE_SEARCHED = 0;


    public static final int CHECKEDID_NET_VIDEO = 0;
    public static final int CHECKEDID_NET_MUSIC = 1;
    public static final int CHECKEDID_NET_PICTURE = 2;
    public static final int CHECKEDID_LOCAL_VIDEO = 3;
    public static final int CHECKEDID_LOCAL_MUSIC = 4;
    public static final int CHECKEDID_LOCAL_PICTURE = 5;
    public Object deviceInfo;
    //连接状态
    public int mConnectType = 0;

//    //搜索状态 搜索中 搜索完成
//    public int mSearchedType = CODE_SEARCHED;

    public boolean mInit = false;


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

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        Log.e(FlutterpluginhpplayPlugin.TAG, call.method);
        if (call.method.equals("initHpplay")) {
            result.success(initDates());
        } else if (call.method.equals("getNetWorkName")) {
            result.success("WiFi:" + NetworkUtil.getNetWorkName(context));
        } else if (call.method.equals("HpplayBrowse")) {
            LelinkSourceSDK.getInstance().startBrowse();
        } else if (call.method.equals("HpplayConnect")) {
            String deviceIp = call.argument("LelinkServiceInfoJson");
            Log.e(TAG, deviceIp);
//            Gson gson = new Gson();
//            LelinkServiceInfo info = gson.fromJson(infoJson, LelinkServiceInfo.class);
            mSelectInfo = mDevicesMap.get(deviceIp);
            connect(mSelectInfo);
        } else if (call.method.equals("HpplayCloseConnect")) {
            LelinkSourceSDK.getInstance().disConnect(mSelectInfo);
            mSelectInfo = null;
        } else if (call.method.equals("HpplayPlay")) {
            int mediaType = call.argument("MediaType");
            String mediaUrl = call.argument("MediaUrl");
            startPlayMedia(mediaType, mediaUrl);
        } else if (call.method.equals("HpplaySeekTo")) {
            int position = call.argument("position");
            LelinkSourceSDK.getInstance().seekTo(position);
        } else if (call.method.equals("HpplayPause")) {
            LelinkSourceSDK.getInstance().pause();
        } else if (call.method.equals("HppalyResume")) {
            LelinkSourceSDK.getInstance().resume();
        } else if (call.method.equals("HpplayStop")) {
            LelinkSourceSDK.getInstance().stopPlay();
        } else if (call.method.equals("HpplayVoulumeUp")) {
            LelinkSourceSDK.getInstance().addVolume();
        } else if (call.method.equals("HpplayVoulumeDown")) {
            LelinkSourceSDK.getInstance().subVolume();
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
    ///获取正在连接的设备
    private void showConnetDevice() {
        if (deviceInfo != null) {
            ackSuccess(MSG_CONNECT_SUCCESS, deviceInfo);
        } else {
            ackSuccess(MSG_CONNECT_FAILURE);
        }
    }

    private boolean initDates() {
        if (!mInit) {
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
            mInit = true;
        } else {
            return false;
        }
        return mInit;
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
//        mLelinkHelper = LelinkHelper.getInstance(context.getApplicationContext());
//        mLelinkHelper.setUIUpdateListener(mUIUpdateListener);
        //sdk初始化
        LelinkSourceSDK.getInstance()
                .setBrowseResultListener(mBrowseListener)
                .setPlayListener(mLelinkPlayerListener)
                .setConnectListener(mConnectListener)
                .setBindSdkListener(mBindSdkListener)
                .setDebugMode(true)
                .setSdkInitInfo(context.getApplicationContext(), APP_ID, APP_SECRET).bindSdk();

        mUiHandler = new UIHandler(this);
    }

    ///搜索事件监听
    IBrowseListener mBrowseListener = new IBrowseListener() {

        @Override
        public void onBrowse(int i, List<LelinkServiceInfo> list) {
            Log.i(TAG, "-------------->list size : " + list.size());
            if (i == IBrowseListener.BROWSE_ERROR_AUTH) {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context.getApplicationContext(), "授权失败", Toast.LENGTH_SHORT).show();
                    }
                });

                mUiHandler.sendMessage(Message.obtain(null, MSG_SEARCH_FAILED));
                return;
            }
            if (mUiHandler != null) {
                mUiHandler.sendMessage(Message.obtain(null, MSG_SEARCH_RESULT, list));
            }
        }

    };
    ///播放事件监听
    ILelinkPlayerListener mLelinkPlayerListener = new ILelinkPlayerListener() {

        @Override
        public void onLoading() {
            mUiHandler.sendMessage(Message.obtain(null, MSG_PLAY_STATE_LOADING));

            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(), "开始加载", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onStart() {
            mUiHandler.sendMessage(Message.obtain(null, MSG_PLAY_STATE_START));


            isPause = false;
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(), "开始播放", Toast.LENGTH_SHORT).show();
//                    mirrorSwitchChange();
                }
            });

        }

        @Override
        public void onPause() {
            mUiHandler.sendMessage(Message.obtain(null, MSG_PLAY_STATE_PAUSE));

            isPause = true;
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(), "暂停播放", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onCompletion() {
            mUiHandler.sendMessage(Message.obtain(null, MSG_PLAY_STATE_COMPLETION));

            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(), "播放完成", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onStop() {
            mUiHandler.sendMessage(Message.obtain(null, MSG_PLAY_STATE_STOP));

            isPlayMirror = false;
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(), "播放停止", Toast.LENGTH_SHORT).show();
//                    mirrorSwitchChange();
                }
            });
        }

        @Override
        public void onSeekComplete(int i) {
            mUiHandler.sendMessage(Message.obtain(null, MSG_PLAY_STATE_SEEKCOMPLETE));

        }

        @Override
        public void onInfo(final int i, final int i1) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (i == ILelinkPlayerListener.INFO_MIRROR_STATE) {
                        if (i1 == ILelinkPlayerListener.INFO_MIRROR_RESUME) {
                            Toast.makeText(context.getApplicationContext(), "镜像恢复", Toast.LENGTH_SHORT).show();
                        } else if (i1 == ILelinkPlayerListener.INFO_MIRROR_PAUSE) {
                            Toast.makeText(context.getApplicationContext(), "镜像暂停", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        @Override
        public void onInfo(int what, final String data) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context.getApplicationContext(), "当前倍率是：" + data, Toast.LENGTH_SHORT).show();
//                    mirrorSwitchChange();
                }
            });
        }

        String text = null;

        @Override
        public void onError(int what, int extra) {
            mUiHandler.sendMessage(Message.obtain(null, MSG_PLAY_STATE_ERROR));


            Log.d(TAG, "onError what:" + what + " extra:" + extra);
            if (what == PUSH_ERROR_INIT) {
                if (extra == PUSH_ERRROR_FILE_NOT_EXISTED) {
                    text = "文件不存在";
                } else if (extra == PUSH_ERROR_IM_OFFLINE) {
                    text = "IM TV不在线";
                } else if (extra == PUSH_ERROR_IMAGE) {

                } else if (extra == PUSH_ERROR_IM_UNSUPPORTED_MIMETYPE) {
                    text = "IM不支持的媒体类型";
                } else {
                    text = "未知";
                }
            } else if (what == MIRROR_ERROR_INIT) {
                if (extra == MIRROR_ERROR_UNSUPPORTED) {
                    text = "不支持镜像";
                } else if (extra == MIRROR_ERROR_REJECT_PERMISSION) {
                    text = "镜像权限拒绝";
                } else if (extra == MIRROR_ERROR_DEVICE_UNSUPPORTED) {
                    text = "设备不支持镜像";
                } else if (extra == NEED_SCREENCODE) {
                    text = "请输入投屏码";
                }
            } else if (what == MIRROR_ERROR_PREPARE) {
                if (extra == MIRROR_ERROR_GET_INFO) {
                    text = "获取镜像信息出错";
                } else if (extra == MIRROR_ERROR_GET_PORT) {
                    text = "获取镜像端口出错";
                } else if (extra == NEED_SCREENCODE) {
                    text = "请输入投屏码";
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
//                            showScreenCodeDialog();
                        }
                    });
                    if (extra == PREEMPT_UNSUPPORTED) {
                        text = "投屏码模式不支持抢占";
                    }
                } else if (what == PUSH_ERROR_PLAY) {
                    if (extra == PUSH_ERROR_NOT_RESPONSED) {
                        text = "播放无响应";
                    } else if (extra == NEED_SCREENCODE) {
                        text = "请输入投屏码";
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
//                                showScreenCodeDialog();
                            }
                        });
                    } else if (extra == RELEVANCE_DATA_UNSUPPORTED) {
                        text = "老乐联不支持数据透传,请升级接收端的版本！";
                    } else if (extra == ILelinkPlayerListener.PREEMPT_UNSUPPORTED) {
                        text = "投屏码模式不支持抢占";
                    }
                } else if (what == PUSH_ERROR_STOP) {
                    if (extra == ILelinkPlayerListener.PUSH_ERROR_NOT_RESPONSED) {
                        text = "退出 播放无响应";
                    }
                } else if (what == PUSH_ERROR_PAUSE) {
                    if (extra == ILelinkPlayerListener.PUSH_ERROR_NOT_RESPONSED) {
                        text = "暂停无响应";
                    }
                } else if (what == PUSH_ERROR_RESUME) {
                    if (extra == ILelinkPlayerListener.PUSH_ERROR_NOT_RESPONSED) {
                        text = "恢复无响应";
                    }
                }

            } else if (what == MIRROR_PLAY_ERROR) {
                if (extra == MIRROR_ERROR_FORCE_STOP) {
                    text = "接收端断开";
                } else if (extra == MIRROR_ERROR_PREEMPT_STOP) {
                    text = "镜像被抢占";
                }
            } else if (what == MIRROR_ERROR_CODEC) {
                if (extra == MIRROR_ERROR_NETWORK_BROKEN) {
                    text = "镜像网络断开";
                }
            }
            if (null != mUiHandler) {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
//                        mirrorSwitchChange();
                        Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }

        @Override
        public void onVolumeChanged(float v) {

        }

        @Override
        public void onPositionUpdate(long l, long l1) {
            if (mUiHandler != null) {
                Message msg = new Message();
                msg.what = MSG_UPDATE_PROGRESS;
                msg.arg1 = (int) l;
                msg.arg2 = (int) l1;
                mUiHandler.sendMessage(msg);
            }
        }
    };
    ///连接状态监听
    IConnectListener mConnectListener = new IConnectListener() {
        @Override
        public void onConnect(LelinkServiceInfo lelinkServiceInfo, int extra) {

            Log.d(TAG, "onConnect:" + lelinkServiceInfo.getName());
            if (mUiHandler != null) {
                mUiHandler.sendMessage(Message.obtain(null, MSG_CONNECT_SUCCESS, extra, 0, lelinkServiceInfo));
            }
        }

        @Override
        public void onDisconnect(LelinkServiceInfo lelinkServiceInfo, int what, int extra) {
            Log.d(TAG, "onDisconnect:" + lelinkServiceInfo.getName() + " disConnectType:" + what + " extra:" + extra);
            String text = null;
            if (what == IConnectListener.CONNECT_INFO_DISCONNECT) {
                if (null != mUiHandler) {
                    if (TextUtils.isEmpty(lelinkServiceInfo.getName())) {
                        text = "pin码连接断开";
                    } else {
                        text = lelinkServiceInfo.getName() + "连接断开";
                    }
                }
            } else if (what == IConnectListener.CONNECT_ERROR_FAILED) {
                if (extra == IConnectListener.CONNECT_ERROR_IO) {
                    text = lelinkServiceInfo.getName() + "连接失败";
                } else if (extra == IConnectListener.CONNECT_ERROR_IM_WAITTING) {
                    text = lelinkServiceInfo.getName() + "等待确认";
                } else if (extra == IConnectListener.CONNECT_ERROR_IM_REJECT) {
                    text = lelinkServiceInfo.getName() + "连接拒绝";
                } else if (extra == IConnectListener.CONNECT_ERROR_IM_TIMEOUT) {
                    text = lelinkServiceInfo.getName() + "连接超时";
                } else if (extra == IConnectListener.CONNECT_ERROR_IM_BLACKLIST) {
                    text = lelinkServiceInfo.getName() + "连接黑名单";
                }
            }
            if (null != mUiHandler) {
                mUiHandler.sendMessage(Message.obtain(null, MSG_CONNECT_FAILURE, text));

            }
        }
    };
    //日志监听
    IBindSdkListener mBindSdkListener = new IBindSdkListener() {
        @Override
        public void onBindCallback(boolean b) {
//            Log.i("onBindCallback", "--------->" + b);
//            // 打开本地日志开关
//            LelinkSourceSDK.getInstance().setOption(IAPI.OPTION_49,true);
//            setPassthroughListener();
        }
    };
//    private IUIUpdateListener mUIUpdateListener = new IUIUpdateListener() {
//
//        @Override
//        public void onUpdate(int what, MessageDeatail deatail) {
//            switch (what) {
//                case IUIUpdateListener.STATE_SEARCH_SUCCESS:
//                    if (isFirstBrowse) {
//                        isFirstBrowse = false;
//                        ToastUtil.show(context, "搜索成功");
//                        Log.e(TAG, "搜索成功");
//                    }
//                    if (null != mDelayHandler) {
//                        mDelayHandler.removeMessages(what);
//                        mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_SUCCESS,
//                                TimeUnit.SECONDS.toMillis(1));
//                    }
//                    break;
//                case IUIUpdateListener.STATE_SEARCH_ERROR:
//                    if (null != mDelayHandler) {
//                        mDelayHandler.removeMessages(what);
//                        mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_SUCCESS,
//                                TimeUnit.SECONDS.toMillis(1));
//                    }
//                    break;
//                case IUIUpdateListener.STATE_SEARCH_NO_RESULT:
//                    if (null != mDelayHandler) {
//                        mDelayHandler.removeMessages(what);
//                        mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_NO_RESULT,
//                                TimeUnit.SECONDS.toMillis(1));
//                    }
//                    break;
//                //-------------------------以上是搜索状态-----------------------------------------------------------
//                case IUIUpdateListener.STATE_CONNECT_SUCCESS:
//                    if (null != mDelayHandler) {
//                        mDelayHandler.removeMessages(what);
//                        Message message = new Message();
//                        message.obj = deatail.obj;
//                        message.what = IUIUpdateListener.STATE_CONNECT_SUCCESS;
//                        mDelayHandler.sendMessageDelayed(message,
//                                TimeUnit.SECONDS.toMillis(1));
//                    }
//                    break;
//                case IUIUpdateListener.STATE_DISCONNECT:
//                    if (null != mDelayHandler) {
//                        mDelayHandler.removeMessages(what);
//                        Message message = new Message();
//                        message.obj = deatail.text;
//                        message.what = IUIUpdateListener.STATE_DISCONNECT;
//                        mDelayHandler.sendMessageDelayed(message,
//                                TimeUnit.SECONDS.toMillis(1));
//                    }
//                    break;
//                case IUIUpdateListener.STATE_CONNECT_FAILURE:
//                    Logger.test(TAG, "connect failure:" + deatail.text);
//                    Log.e(TAG, "ToastUtil " + deatail.text);
//                    ToastUtil.show(context, deatail.text);
//                    break;
//                //-------------------------以上是连接状态-----------------------------------------------------------
//                case IUIUpdateListener.STATE_PLAY:
//                    ackSuccess(IUIUpdateListener.STATE_PLAY);
//                    ToastUtil.show(context, "开始播放");
//                    break;
//                case IUIUpdateListener.STATE_LOADING:
//                    ackSuccess(IUIUpdateListener.STATE_LOADING);
//                    ToastUtil.show(context, "开始加载");
//                    break;
//                case IUIUpdateListener.STATE_PAUSE:
//                    ackSuccess(IUIUpdateListener.STATE_PAUSE);
//                    ToastUtil.show(context, "暂停播放");
////                    isPause = true;
//                    break;
//                case IUIUpdateListener.STATE_STOP:
//                    ackSuccess(IUIUpdateListener.STATE_STOP);
//                    ToastUtil.show(context, "播放结束");
//                    break;
//                case IUIUpdateListener.STATE_SEEK:
//                    Logger.test(TAG, "callback seek:" + deatail.text);
//                    Logger.d(TAG, "ToastUtil seek完成:" + deatail.text);
//                    ToastUtil.show(context, "seek完成" + deatail.text);
//                    break;
//                case IUIUpdateListener.STATE_PLAY_ERROR:
//                    ackSuccess(IUIUpdateListener.STATE_COMPLETION);
//                    ToastUtil.show(context, "播放错误：" + deatail.text);
//                    break;
//                case IUIUpdateListener.STATE_POSITION_UPDATE:
//                    long[] arr = (long[]) deatail.obj;
//                    long duration = arr[0];
//                    long position = arr[1];
//                    Logger.d(TAG, "ToastUtil 总长度：" + duration + " 当前进度:" + position);
//                    ackSuccess(IUIUpdateListener.STATE_POSITION_UPDATE, arr);
//                    break;
//                case IUIUpdateListener.STATE_COMPLETION:
//                    ackSuccess(IUIUpdateListener.STATE_COMPLETION);
//                    ToastUtil.show(context, "播放完成");
//                    break;
//                case IUIUpdateListener.STATE_INPUT_SCREENCODE:
//                    Logger.test(TAG, "input screencode");
//                    ToastUtil.show(context, deatail.text);
////                    showScreenCodeDialog();
//                    break;
//                case IUIUpdateListener.RELEVANCE_DATA_UNSUPPORT:
//                    Logger.test(TAG, "unsupport relevance data");
//                    ToastUtil.show(context, deatail.text);
//                    break;
//                case IUIUpdateListener.STATE_SCREENSHOT:
//                    Logger.test(TAG, "unsupport relevance data");
//                    ToastUtil.show(context, deatail.text);
//                    break;
//            }
//        }
//
//    };

//    private void browse() {
//
//        if (null != mLelinkHelper) {
//
//            //            boolean isLelinkOpen = mSwitchLeLink.isChecked();
//            //            boolean isDLNAOpen = mSwitchDLNA.isChecked();
//            boolean isLelinkOpen = true;
//            boolean isDLNAOpen = true;
//
//            int type;
//            String text;
//            if (isLelinkOpen && isDLNAOpen) {
//                text = "All";
//                type = ILelinkServiceManager.TYPE_ALL;
//            } else if (isLelinkOpen) {
//                text = "Lelink";
//                type = ILelinkServiceManager.TYPE_LELINK;
//            } else if (isDLNAOpen) {
//                text = "DLNA";
//                type = ILelinkServiceManager.TYPE_DLNA;
//            } else {
//                text = "All";
//                type = ILelinkServiceManager.TYPE_ALL;
//            }
//            Logger.test(TAG, "browse type:" + text);
//            Log.e(TAG, "browse type:" + text);
//            if (!isFirstBrowse) {
//                isFirstBrowse = true;
//            }
//            mLelinkHelper.browse(type);
//        } else {
//            ToastUtil.show(context, "权限不够");
//            if (null != mDelayHandler) {
//                mDelayHandler.removeCallbacksAndMessages(null);
//                mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_SUCCESS,
//                        TimeUnit.SECONDS.toMillis(1));
//            }
//        }
//    }

//    private void stopBrowse() {
//        Logger.test(TAG, "btn_stop_browse click");
//        if (null != mLelinkHelper) {
//            Logger.test(TAG, "stop browse");
//            Logger.d(TAG, "stop browse");
//            isFirstBrowse = false;
//            mLelinkHelper.stopBrowse();
//        } else {
//            ToastUtil.show(context, "未初始化");
//        }
//    }

    private void connect(LelinkServiceInfo serviceInfo) {
        LelinkSourceSDK.getInstance().connect(serviceInfo);
    }

    void startPlayMedia(int checkedId, String url) {
//        if (null == mSelectInfo) {
//            Toast.makeText(getApplicationContext(), "请选择接设备", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (isPause) {
//            isPause = false;
//            // 暂停中
//            LelinkSourceSDK.getInstance().resume();
//            return;
//        }
//        int checkedId = mRadioGroup.getCheckedRadioButtonId();
        int mediaType = 0;
        String mediaTypeStr = null;
        boolean isLocalFile = false;
//        String url = url;
        switch (checkedId) {
            case CHECKEDID_NET_VIDEO:
                mediaType = LelinkSourceSDK.MEDIA_TYPE_VIDEO;

                mediaTypeStr = "NetVideo";
                break;
            case CHECKEDID_NET_MUSIC:
                mediaType = LelinkSourceSDK.MEDIA_TYPE_AUDIO;

                mediaTypeStr = "NetMusic";
                break;
            case CHECKEDID_NET_PICTURE:
                mediaType = LelinkSourceSDK.MEDIA_TYPE_IMAGE;

                mediaTypeStr = "NetPicture";
                break;
            case CHECKEDID_LOCAL_VIDEO:
                mediaType = LelinkSourceSDK.MEDIA_TYPE_VIDEO;
                isLocalFile = true;

                mediaTypeStr = "LocalVideo";
                break;
            case CHECKEDID_LOCAL_MUSIC:
                mediaType = LelinkSourceSDK.MEDIA_TYPE_AUDIO;
                isLocalFile = true;

                mediaTypeStr = "LocalMusic";
                break;
            case CHECKEDID_LOCAL_PICTURE:
                mediaType = LelinkSourceSDK.MEDIA_TYPE_IMAGE;
                isLocalFile = true;

                mediaTypeStr = "LocalPicture";
                break;
        }
        Log.i(TAG, "start play url:" + url + " type:" + mediaTypeStr);

        LelinkPlayerInfo lelinkPlayerInfo = new LelinkPlayerInfo();
        if (isLocalFile) {
            lelinkPlayerInfo.setLocalPath(url);
        } else {
            lelinkPlayerInfo.setUrl(url);
        }
        lelinkPlayerInfo.setType(mediaType);
//        if (!TextUtils.isEmpty(mScreencode)) {
//            lelinkPlayerInfo.setOption(IAPI.OPTION_6, mScreencode);
//        }
        lelinkPlayerInfo.setLelinkServiceInfo(mSelectInfo);
        LelinkSourceSDK.getInstance().startPlayMedia(lelinkPlayerInfo);
    }

//    private void play(int mediaType, String url) {
//        isPlayMirror = false;
//        if (null == mLelinkHelper) {
//            ToastUtil.show(context, "未初始化或未选择设备");
//            return;
//        }
//        List<LelinkServiceInfo> connectInfos = mLelinkHelper.getConnectInfos();
//        Logger.e(TAG, "connect click:" + connectInfos.toString());
//        if (null == connectInfos || connectInfos.isEmpty()) {
//            ToastUtil.show(context, "请先连接设备");
//            return;
//        }
////        if (isPause) {
////            Logger.test(TAG, "resume click");
////            isPause = false;
////            // 暂停中
////            mLelinkHelper.resume();
////            return;
////        } else {
////            Logger.test(TAG, "play click");
////        }
////        int checkedId = mRadioGroup.getCheckedRadioButtonId();
////        int mediaType = 0;
//        String mediaTypeStr = null;
//        boolean isLocalFile = false;
////        String url = null;
//        switch (mediaType) {
//            case AllCast.MEDIA_TYPE_VIDEO:
////                url = mEtNetVideo.getText().toString();
//                mediaTypeStr = "NetVideo";
//                break;
//            case AllCast.MEDIA_TYPE_AUDIO:
////                url = mEtNetMusic.getText().toString();
//                mediaTypeStr = "NetMusic";
//                break;
//            case AllCast.MEDIA_TYPE_IMAGE:
//                mediaType = AllCast.MEDIA_TYPE_IMAGE;
////                url = mEtNetPicture.getText().toString();
//                mediaTypeStr = "NetPicture";
//                break;
////            case AllCast.MEDIA_TYPE_VIDEO:
////                mediaType = AllCast.MEDIA_TYPE_VIDEO;
////                isLocalFile = true;
//////                url = mEtLocalVideo.getText().toString();
////                mediaTypeStr = "LocalVideo";
////                break;
////            case AllCast.MEDIA_TYPE_AUDIO:
////                mediaType = AllCast.MEDIA_TYPE_AUDIO;
////                isLocalFile = true;
//////                url = mEtLocalMusic.getText().toString();
////                mediaTypeStr = "LocalMusic";
////                break;
////            case AllCast.MEDIA_TYPE_IMAGE:
////                mediaType = AllCast.MEDIA_TYPE_IMAGE;
////                isLocalFile = true;
//////                url = mEtLocalPicture.getText().toString();
////                mediaTypeStr = "LocalPicture";
////                break;
//        }
//        Logger.test(TAG, "start play url:" + url + " type:" + mediaTypeStr);
//        if (isLocalFile) {
//            // 本地media
//            mLelinkHelper.playLocalMedia(url, mediaType, "");
//        } else {
//            // 网络media
//            mLelinkHelper.playNetMedia(url, mediaType, "");
//        }
//
//    }

    //设备搜索结果
    private void updateBrowseAdapter(List<LelinkServiceInfo> infos) {
        if (!infos.isEmpty()) {
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
                map1.put("ack", MSG_SEARCH_RESULT);
                String toJson = gson.toJson(map1);
                eventSink.success(toJson);
            } else {
                Log.e(TAG, "error :eventSink not init");
            }
        } else {
            //搜索数据为空
            ackSuccess(MSG_SEARCH_EMPTY);
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    private static String TAG = "FlutterpluginhpplayPlugin";
    HashMap<String, LelinkServiceInfo> mDevicesMap = new HashMap<String, LelinkServiceInfo>();
    private LelinkServiceInfo mSelectInfo;
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

    private class UIHandler extends Handler {

        private WeakReference<Activity> mReference;

        UIHandler(Activity reference) {
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Activity mainActivity = mReference.get();
            if (mainActivity == null) {
                return;
            }
            switch (msg.what) {
                case MSG_SEARCH_FAILED://搜索失败
                    ackSuccess(MSG_SEARCH_FAILED);
                    break;
                case MSG_SEARCH_RESULT://搜索成功
                    updateBrowseAdapter((List<LelinkServiceInfo>) msg.obj);//更新列表
                    break;
                case MSG_CONNECT_SUCCESS://连接成功
                    deviceInfo = msg.obj;
                    showConnetDevice();
                    break;
                case MSG_CONNECT_FAILURE:
                    deviceInfo = null;
                    ackSuccess(MSG_CONNECT_FAILURE, msg.obj);
                    String text = (String) msg.obj;
                    if (!TextUtils.isEmpty(text))
                        Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_UPDATE_PROGRESS:
                    int duration = msg.arg1;
                    int position = msg.arg2;
                    int[] arr = {duration, position};
                    Logger.d(TAG, "ToastUtil 总长度：" + msg.arg1 + " 当前进度:" + msg.arg2);
                    ackSuccess(MSG_UPDATE_PROGRESS, arr);
                    break;
                case MSG_PLAY_STATE_START:
                case MSG_PLAY_STATE_PAUSE:
                case MSG_PLAY_STATE_COMPLETION:
                case MSG_PLAY_STATE_STOP:
                case MSG_PLAY_STATE_ERROR:
                case MSG_PLAY_STATE_SEEKCOMPLETE:
                case MSG_PLAY_STATE_LOADING:

                    ackSuccess(msg.what);

                    break;
            }
            super.handleMessage(msg);
        }

    }
//    ///这个handler 负责回馈状态到flutter
//    // 只有搜索状态只有改变时 才通知flutter 状态改变
//    // 搜索只要有结果    就通知flutter 更新列表
//    // 只有搜索完成到开始搜索 才进行搜索
//    private class UIHandler extends Handler {
//
//        private WeakReference<Activity> mReference;
//
//        UIHandler(Activity reference) {
//            mReference = new WeakReference<>(reference);
//        }
//
//
//        @Override
//        public void handleMessage(Message msg) {
//            boolean b = false;
//            if (msg.what == IUIUpdateListener.STATE_SEARCH_SUCCESS || msg.what == IUIUpdateListener.STATE_SEARCH_ING) {
//                b = changeSearchType(msg.what);
//            } else {
//                changeConnectType(msg.what);
//            }
//
//            Activity mainActivity = mReference.get();
//            if (mainActivity == null) {
//                return;
//            }
//            switch (msg.what) {
//                case IUIUpdateListener.STATE_SEARCH_SUCCESS:
//                    updateBrowseAdapter();//更新列表
//                    break;
////                case IUIUpdateListener.STATE_SEARCH_ING:
////                    if (b) browse();
////                    break;
//                case IUIUpdateListener.STATE_SEARCH_NO_RESULT: {
//                    ackSuccess(IUIUpdateListener.STATE_SEARCH_NO_RESULT);
//                    break;
//                }
//
//                case IUIUpdateListener.STATE_CONNECT_SUCCESS: {
//                    mConnectType = IUIUpdateListener.STATE_CONNECT_SUCCESS;
//                    deviceInfo = msg.obj;
//                    ackSuccess(IUIUpdateListener.STATE_CONNECT_SUCCESS, deviceInfo);
//                    break;
//                }
//
//                case IUIUpdateListener.STATE_DISCONNECT:
//                    deviceInfo = null;
//                    mConnectType = IUIUpdateListener.STATE_DISCONNECT;
//                    ackSuccess(IUIUpdateListener.STATE_DISCONNECT);
//                    break;
//            }
//
//            super.handleMessage(msg);
//        }
//    }

    public int ackSuccess(int i) {
        Map map = new HashMap();
        map.put("ack", i);
        Gson gson = new Gson();
        String toJson = gson.toJson(map);
        eventSink.success(toJson);
        return mConnectType;
    }

    public int ackSuccess(int i, Object data) {
        Gson gson = new Gson();
        Map map = new HashMap();
        map.put("ack", i);
        map.put("data", gson.toJson(data));
        String toJson = gson.toJson(map);
        eventSink.success(toJson);
        return mConnectType;
    }

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
