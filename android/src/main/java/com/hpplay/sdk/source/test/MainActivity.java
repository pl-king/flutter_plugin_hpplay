package com.hpplay.sdk.source.test;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.zxing.activity.CaptureActivity;
import com.hpplay.common.utils.DeviceUtil;
import com.hpplay.common.utils.NetworkUtil;
import com.hpplay.sdk.source.bean.DanmakuPropertyBean;
import com.hpplay.sdk.source.browse.api.ILelinkServiceManager;
import com.hpplay.sdk.source.browse.api.IQRCodeListener;
import com.hpplay.sdk.source.browse.api.LelinkServiceInfo;
import com.hpplay.sdk.source.test.adapter.BrowseAdapter;
import com.hpplay.sdk.source.test.bean.MessageDeatail;
import com.hpplay.sdk.source.test.utils.AssetsUtil;
import com.hpplay.sdk.source.test.utils.CameraPermissionCompat;
import com.hpplay.sdk.source.test.utils.Logger;
import com.hpplay.sdk.source.test.utils.ToastUtil;
import com.mxiaotu.flutter_plugin_newhpplay.R;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zippo on 2018/6/8.
 * Date: 2018/6/8
 * Time: 14:49:57
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private static final String NET_VIDEO_URL = "https://v.mifile.cn/b2c-mimall-media/ed921294fb62caf889d40502f5b38147.mp4";
    private static final String NET_MUSIC_URL = "http://music.163.com/song/media/outer/url?id=287248.mp3";
    private static final String NET_PICTURE_URL = "http://news.cri.cn/gb/mmsource/images/2013/06/23/2/2211679758122940818.jpg";
    private static final String LOCAL_MEDIA_PATH = "/hpplay_demo/local_media/";
    private static final String SDCARD_LOCAL_MEDIA_PATH = Environment.getExternalStorageDirectory()
            + LOCAL_MEDIA_PATH;
    private static final String LOCAL_VIDEO_URL = SDCARD_LOCAL_MEDIA_PATH + "test_video.mp4";
    private static final String LOCAL_MUSIC_URL = SDCARD_LOCAL_MEDIA_PATH + "EDC - I Never Told You.mp3";
    private static final String LOCAL_PICTURE_URL = SDCARD_LOCAL_MEDIA_PATH + "I01027343.jpg";

    private static final int REQUEST_MUST_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 4;

    private Context mContext;

    // SDK
    private LelinkHelper mLelinkHelper;

    private TextView mTvVersion, mTvWifi, mTvIp;
    private SwitchCompat mSwitchLeLink, mSwitchDLNA;
    private RecyclerView mBrowseRecyclerView, mConnectRecyclerView;
    private EditText mEtPinCode;
    private RadioGroup mRadioGroup, mRgResolution, mRgBitRate, mRgMirrorAudio;
    private EditText mEtNetVideo, mEtNetMusic, mEtNetPicture;
    private EditText mEtLocalVideo, mEtLocalMusic, mEtLocalPicture, mAppidEdit;
    private SeekBar mProgressBar, seekbarVolume;
    private NetworkReceiver mNetworkReceiver;

    private RadioButton netVideoRbtn, netAudioRbtn, netPictureRbtn, localVideoRbtn, localAudioRbtn, localPictureRbtn;
    private CheckBox mCheckBox;
    // 数据
    private UIHandler mDelayHandler;
    private BrowseAdapter mBrowseAdapter, mConnectAdapter;
    private boolean isFirstBrowse = true;
    private boolean isPause = false;
    private SDKManager mSDKManager;
    private String mScreencode = null;
    private LelinkServiceInfo mSelectInfo;
    private boolean isPlayMirror;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        initViews();
        initEvents();
        initDatas();
    }

    private void initViews() {
        mTvVersion = (TextView) findViewById(R.id.tv_version);
        mTvWifi = (TextView) findViewById(R.id.tv_wifi);
        mTvIp = (TextView) findViewById(R.id.tv_ip);
        mSwitchLeLink = (SwitchCompat) findViewById(R.id.switch_lelink);
        mSwitchDLNA = (SwitchCompat) findViewById(R.id.switch_dlna);
        mBrowseRecyclerView = (RecyclerView) findViewById(R.id.recycler_browse);
        mEtPinCode = (EditText) findViewById(R.id.et_pincode);
        mRadioGroup = (RadioGroup) findViewById(R.id.radio_group);
        mEtNetVideo = (EditText) findViewById(R.id.et_net_video);
        mEtNetMusic = (EditText) findViewById(R.id.et_net_music);
        mEtNetPicture = (EditText) findViewById(R.id.et_net_picture);
        mEtLocalVideo = (EditText) findViewById(R.id.et_local_video);
        mEtLocalMusic = (EditText) findViewById(R.id.et_local_music);
        mEtLocalPicture = (EditText) findViewById(R.id.et_local_picture);
        mProgressBar = (SeekBar) findViewById(R.id.seekbar_progress);
        seekbarVolume = (SeekBar) findViewById(R.id.seekbar_volume);
        mConnectRecyclerView = (RecyclerView) findViewById(R.id.recycler_connect_device);
        mRgResolution = (RadioGroup) findViewById(R.id.rg_resolution);
        mRgBitRate = (RadioGroup) findViewById(R.id.rg_bitrate);
        mRgMirrorAudio = (RadioGroup) findViewById(R.id.rg_mirror_audio);

        netVideoRbtn = (RadioButton) findViewById(R.id.rb_net_video);
        netAudioRbtn = (RadioButton) findViewById(R.id.rb_net_music);
        netPictureRbtn = (RadioButton) findViewById(R.id.rb_net_picture);
        localVideoRbtn = (RadioButton) findViewById(R.id.rb_local_video);
        localAudioRbtn = (RadioButton) findViewById(R.id.rb_local_music);
        localPictureRbtn = (RadioButton) findViewById(R.id.rb_local_picture);
        mCheckBox = (CheckBox) findViewById(R.id.checkbox);
        mAppidEdit = (EditText) findViewById(R.id.edit_appid);
    }

    private void initEvents() {
        findViewById(R.id.btn_browse).setOnClickListener(this);
        findViewById(R.id.btn_stop_browse).setOnClickListener(this);
        findViewById(R.id.btn_disconnect).setOnClickListener(this);
        findViewById(R.id.btn_qrcode).setOnClickListener(this);
        findViewById(R.id.btn_delete).setOnClickListener(this);
        findViewById(R.id.btn_pincode_connect).setOnClickListener(this);
        findViewById(R.id.rb_net_video).setOnClickListener(this);
        findViewById(R.id.rb_net_music).setOnClickListener(this);
        findViewById(R.id.rb_net_picture).setOnClickListener(this);
        findViewById(R.id.rb_local_video).setOnClickListener(this);
        findViewById(R.id.rb_local_music).setOnClickListener(this);
        findViewById(R.id.rb_local_picture).setOnClickListener(this);
        findViewById(R.id.btn_play).setOnClickListener(this);
        findViewById(R.id.btn_pause).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
        findViewById(R.id.btn_volume_up).setOnClickListener(this);
        findViewById(R.id.btn_volume_down).setOnClickListener(this);
        mProgressBar.setOnSeekBarChangeListener(mProgressChangeListener);
        seekbarVolume.setOnSeekBarChangeListener(mProgressChangeListener);
        findViewById(R.id.btn_start_mirror).setOnClickListener(this);
        findViewById(R.id.btn_stop_mirror).setOnClickListener(this);
        findViewById(R.id.btn_set_ad_listener).setOnClickListener(this);
        findViewById(R.id.btn_report_ad_show).setOnClickListener(this);
        findViewById(R.id.btn_report_ad_end).setOnClickListener(this);
        findViewById(R.id.btn_send_mediaasset_info).setOnClickListener(this);
        findViewById(R.id.btn_send_error_info).setOnClickListener(this);
        findViewById(R.id.btn_send_passth_info).setOnClickListener(this);
        findViewById(R.id.btn_send_header_info).setOnClickListener(this);
        findViewById(R.id.btn_send_lebo_passth_info).setOnClickListener(this);
        findViewById(R.id.btn_loop_mode).setOnClickListener(this);
        findViewById(R.id.btn_3rd_monitor).setOnClickListener(this);
        findViewById(R.id.btn_pushbtn_click).setOnClickListener(this);
        findViewById(R.id.btn_list_gone).setOnClickListener(this);
        findViewById(R.id.send_danmaku).setOnClickListener(this);
        findViewById(R.id.danmaku_settings).setOnClickListener(this);
        findViewById(R.id.btn_screenshot).setOnClickListener(this);
    }

    private void initDatas() {
        mSDKManager = new SDKManager(MainActivity.this);
        mSDKManager.startMonitor();
        copyMediaToSDCard();

        mEtNetVideo.setText(NET_VIDEO_URL);
        mEtNetMusic.setText(NET_MUSIC_URL);
        mEtNetPicture.setText(NET_PICTURE_URL);

        mDelayHandler = new UIHandler(MainActivity.this);
        mNetworkReceiver = new NetworkReceiver(MainActivity.this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, intentFilter);

        mTvVersion.setText("SDK:" + com.hpplay.sdk.source.api.BuildConfig.BUILD_TYPE
                + "-" + com.hpplay.sdk.source.api.BuildConfig.VERSION_NAME);
        refreshWifiName();

        // 初始化browse RecyclerView
        // 设置Adapter
        mBrowseAdapter = new BrowseAdapter(mContext);
        mBrowseRecyclerView.setAdapter(mBrowseAdapter);
        mBrowseAdapter.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onClick(int position, LelinkServiceInfo info) {
//                disConnect(false);
                connect(info);
                mSelectInfo = info;
                mBrowseAdapter.setSelectInfo(info);
                mBrowseAdapter.notifyDataSetChanged();
            }

        });

        // connect adapter
        mConnectAdapter = new BrowseAdapter(mContext);
        mConnectRecyclerView.setAdapter(mConnectAdapter);
        mConnectAdapter.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onClick(int position, LelinkServiceInfo info) {
                mConnectAdapter.setSelectInfo(info);
                mConnectAdapter.notifyDataSetChanged();
            }

        });

        if (ContextCompat.checkSelfPermission(getApplication(),
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_DENIED
                && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_DENIED) {
            initLelinkHelper();
        } else {
            // 若没有授权，会弹出一个对话框（这个对话框是系统的，开发者不能自己定制），用户选择是否授权应用使用系统权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_MUST_PERMISSION);
        }
    }

    private void copyMediaToSDCard() {
        AssetsUtil.getInstance(mContext)
                .copyAssetsToSD("local_media", LOCAL_MEDIA_PATH)
                .setFileOperateCallback(new AssetsUtil.FileOperateCallback() {

                    @Override
                    public void onSuccess() {
                        Logger.d(TAG, "copyMediaToSDCard onSuccess");
                        mEtLocalVideo.setText(LOCAL_VIDEO_URL);
                        mEtLocalMusic.setText(LOCAL_MUSIC_URL);
                        mEtLocalPicture.setText(LOCAL_PICTURE_URL);
                    }

                    @Override
                    public void onFailed(String error) {
                        Logger.e(TAG, error);
                    }

                });
    }

    private void initLelinkHelper() {
        mLelinkHelper = MyApplication.getMyApplication().getLelinkHelper();
        mLelinkHelper.setUIUpdateListener(mUIUpdateListener);
    }

    public void refreshWifiName() {
        mTvWifi.setText("WiFi:" + NetworkUtil.getNetWorkName(mContext));
        mTvIp.setText(DeviceUtil.getIPAddress(mContext));
    }

    private void browse() {
        Logger.test(TAG, "btn_browse click");
        if (null != mLelinkHelper) {
            boolean isLelinkOpen = mSwitchLeLink.isChecked();
            boolean isDLNAOpen = mSwitchDLNA.isChecked();
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
            Logger.d(TAG, "browse type:" + text);
            if (!isFirstBrowse) {
                isFirstBrowse = true;
            }
            mLelinkHelper.browse(type);
        } else {
            ToastUtil.show(mContext, "权限不够");
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
            ToastUtil.show(mContext, "未初始化");
        }
    }

    private void scanQrCode() {
        CameraPermissionCompat.checkCameraPermission(this, new CameraPermissionCompat.OnCameraPermissionListener() {

            @Override
            public void onGrantResult(boolean granted) {
                Logger.d(TAG, "权限--------->" + granted);
                if (granted) {
                    // 允许，打开二维码
                    startCaptureActivity();
                } else {
                    // 若没有授权，会弹出一个对话框（这个对话框是系统的，开发者不能自己定制），用户选择是否授权应用使用系统权限
                }
            }

        });
    }

    private void startCaptureActivity() {
        // 允许，打开二维码
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, REQUEST_CAMERA_PERMISSION);
    }

    private void updateBrowseAdapter() {
        if (null != mLelinkHelper) {
            List<LelinkServiceInfo> infos = mLelinkHelper.getInfos();
            mBrowseAdapter.updateDatas(infos);
        }
    }

    private void connect(LelinkServiceInfo info) {
        Logger.test(TAG, "connect click:" + info.getName());
        if (null != mLelinkHelper) {
            ToastUtil.show(mContext, "选中了:" + info.getName()
                    + " type:" + info.getTypes());
            Logger.test(TAG, "start connect:" + info.getName());
            mLelinkHelper.connect(info);
        } else {
            ToastUtil.show(mContext, "未初始化或未选择设备");
        }
    }

    private void disConnect(boolean isBtnClick) {
        List<LelinkServiceInfo> selectInfos = mLelinkHelper.getConnectInfos();
        if (null != mLelinkHelper && null != selectInfos) {
            if (isBtnClick) {
                //断开所有连接设备
                for (LelinkServiceInfo info : selectInfos) {
                    mLelinkHelper.disConnect(info);
                }
            } else {
                //断开除了当前选择连接以外的所有设备
                for (LelinkServiceInfo info : selectInfos) {
                    if (!AssetsUtil.isContains(mSelectInfo, info)) {
                        mLelinkHelper.disConnect(info);
                    }
                }
            }
            updateConnectAdapter();
        } else {
            if (isBtnClick) {
                ToastUtil.show(mContext, "未初始化或未选择设备");
            }
        }
    }

    private void updateConnectAdapter() {
        if (null != mLelinkHelper) {
            List<LelinkServiceInfo> infos = mLelinkHelper.getConnectInfos();
            mConnectAdapter.updateDatas(infos);
        }
    }

    private void refreshMediaButton(LelinkServiceInfo serviceInfo) {
        boolean canPlayLocalVideo = mLelinkHelper.canPlayLocalVideo(serviceInfo);
        if (canPlayLocalVideo) {
            localVideoRbtn.setEnabled(true);
        } else {
            localVideoRbtn.setEnabled(false);
        }

        boolean canPlayLocalPhoto = mLelinkHelper.canPlayLocalPhoto(serviceInfo);

        if (canPlayLocalPhoto) {
            localPictureRbtn.setEnabled(true);
        } else {
            localPictureRbtn.setEnabled(false);
        }

        boolean canPlayLocalAudio = mLelinkHelper.canPlayLocalAudio(serviceInfo);

        if (canPlayLocalAudio) {
            localAudioRbtn.setEnabled(true);
        } else {
            localAudioRbtn.setEnabled(false);
        }


        boolean canPlayOnlieVideo = mLelinkHelper.canPlayOnlineVideo(serviceInfo);

        if (canPlayOnlieVideo) {
            netVideoRbtn.setEnabled(true);
        } else {
            netVideoRbtn.setEnabled(false);
        }

        boolean canPlayOnlieAudio = mLelinkHelper.canPlayOnlineAudio(serviceInfo);

        if (canPlayOnlieAudio) {
            netAudioRbtn.setEnabled(true);
        } else {
            netAudioRbtn.setEnabled(false);
        }

        boolean canPlayOnliePhoto = mLelinkHelper.canPlayOnlinePhoto(serviceInfo);

        if (canPlayOnliePhoto) {
            netPictureRbtn.setEnabled(true);
        } else {
            netPictureRbtn.setEnabled(false);
        }
    }

    private void play() {
        isPlayMirror = false;
        if (null == mLelinkHelper) {
            ToastUtil.show(mContext, "未初始化或未选择设备");
            return;
        }
        List<LelinkServiceInfo> connectInfos = mLelinkHelper.getConnectInfos();

        if (null == connectInfos || connectInfos.isEmpty()) {
            ToastUtil.show(mContext, "请先连接设备");
            return;
        }
        if (isPause) {
            Logger.test(TAG, "resume click");
            isPause = false;
            // 暂停中
            mLelinkHelper.resume();
            return;
        } else {
            Logger.test(TAG, "play click");
        }
        int checkedId = mRadioGroup.getCheckedRadioButtonId();
        int mediaType = 0;
        String mediaTypeStr = null;
        boolean isLocalFile = false;
        String url = null;
//        switch (checkedId) {
//            case R.id.rb_net_video:
//                mediaType = AllCast.MEDIA_TYPE_VIDEO;
//                url = mEtNetVideo.getText().toString();
//                mediaTypeStr = "NetVideo";
//                break;
//            case R.id.rb_net_music:
//                mediaType = AllCast.MEDIA_TYPE_AUDIO;
//                url = mEtNetMusic.getText().toString();
//                mediaTypeStr = "NetMusic";
//                break;
//            case R.id.rb_net_picture:
//                mediaType = AllCast.MEDIA_TYPE_IMAGE;
//                url = mEtNetPicture.getText().toString();
//                mediaTypeStr = "NetPicture";
//                break;
//            case R.id.rb_local_video:
//                mediaType = AllCast.MEDIA_TYPE_VIDEO;
//                isLocalFile = true;
//                url = mEtLocalVideo.getText().toString();
//                mediaTypeStr = "LocalVideo";
//                break;
//            case R.id.rb_local_music:
//                mediaType = AllCast.MEDIA_TYPE_AUDIO;
//                isLocalFile = true;
//                url = mEtLocalMusic.getText().toString();
//                mediaTypeStr = "LocalMusic";
//                break;
//            case R.id.rb_local_picture:
//                mediaType = AllCast.MEDIA_TYPE_IMAGE;
//                isLocalFile = true;
//                url = mEtLocalPicture.getText().toString();
//                mediaTypeStr = "LocalPicture";
//                break;
//        }
        Logger.test(TAG, "start play url:" + url + " type:" + mediaTypeStr);
        if (isLocalFile) {
            // 本地media
            mLelinkHelper.playLocalMedia(url, mediaType, mScreencode);
        } else {
            // 网络media
            mLelinkHelper.playNetMedia(url, mediaType, mScreencode);
        }
    }

    private void startMirror() {
        isPlayMirror = true;
        if (null == mLelinkHelper) {
            Logger.test(TAG, "start mirror click error not_init");
            ToastUtil.show(mContext, "未初始化");
            return;
        }
        LelinkServiceInfo info = mConnectAdapter.getSelectInfo();
        if (null == info) {
            Logger.test(TAG, "start mirror click error no_select");
            ToastUtil.show(mContext, "请在连接列表选中设备");
            return;
        }
        // 分辨率
        int resolutionLevel = 0;
        int resolutionCheckId = mRgResolution.getCheckedRadioButtonId();
//        switch (resolutionCheckId) {
//            case R.id.rb_resolution_height:
//                resolutionLevel = AllCast.RESOLUTION_HEIGHT;
//                break;
//            case R.id.rb_resolution_middle:
//                resolutionLevel = AllCast.RESOLUTION_MIDDLE;
//                break;
//            case R.id.rb_resolution_low:
//                resolutionLevel = AllCast.RESOLUTION_AUTO;
//                break;
//        }

        // 比特率
        int bitrateLevel = 0;
        int bitrateCheckId = mRgBitRate.getCheckedRadioButtonId();
//        switch (bitrateCheckId) {
//            case R.id.rb_bitrate_height:
//                bitrateLevel = AllCast.BITRATE_HEIGHT;
//                break;
//            case R.id.rb_bitrate_middle:
//                bitrateLevel = AllCast.BITRATE_MIDDLE;
//                break;
//            case R.id.rb_bitrate_low:
//                bitrateLevel = AllCast.BITRATE_LOW;
//                break;
//        }

        // 音频
        boolean audioEnable = true;
        int audioCheckId = mRgMirrorAudio.getCheckedRadioButtonId();
//        switch (audioCheckId) {
//            case R.id.rb_mirror_audio_on:
//                audioEnable = true;
//                break;
//            case R.id.rb_mirror_audio_off:
//                audioEnable = false;
//                break;
//        }

        // 开启镜像声音需要权限
        if (audioEnable) {
            if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_DENIED) {
                // 同意权限
                Logger.test(TAG, "star mirror name:" + info.getName()
                        + " resolutionLevel:" + resolutionLevel
                        + " bitrateLevel:" + bitrateLevel
                        + " audioEnable:" + audioEnable);
                mLelinkHelper.startMirror(MainActivity.this, info, resolutionLevel, bitrateLevel,
                        audioEnable, mScreencode);
            } else {
                // 不同意，则去申请权限
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            }
        } else {
            mLelinkHelper.startMirror(MainActivity.this, info, resolutionLevel, bitrateLevel,
                    audioEnable, mScreencode);
        }
    }

    private void stopMirror() {
        List<LelinkServiceInfo> connectInfos = mLelinkHelper.getConnectInfos();
        if (null != mLelinkHelper && null != connectInfos && !connectInfos.isEmpty()) {
            Logger.test(TAG, "stopMirro click");
            mLelinkHelper.stopMirror();
        }
    }

    @Override
    public void onClick(View v) {
        List<LelinkServiceInfo> connectInfos = null;
        if (null != mLelinkHelper) {
            connectInfos = mLelinkHelper.getConnectInfos();
        }
        int id = v.getId();
        if (id == R.id.btn_browse) {
            browse();
        } else if (id == R.id.btn_stop_browse) {
            stopBrowse();
        } else if (id == R.id.btn_disconnect) {
            disConnect(true);
            mBrowseAdapter.setSelectInfo(null);
            mBrowseAdapter.notifyDataSetChanged();
        } else if (id == R.id.btn_qrcode) {
            scanQrCode();
        } else if (id == R.id.btn_delete) {
            if (null != mLelinkHelper) {
                mLelinkHelper.deleteRemoteServiceInfo(mBrowseAdapter.getSelectInfo());
            }
        } else if (id == R.id.btn_pincode_connect) {
            String pinCodeStr = mEtPinCode.getText().toString();
            if (TextUtils.isEmpty(pinCodeStr) || pinCodeStr.length() != 9) {
                ToastUtil.show(mContext, "pin码不能为空或pin码不等于9位");
                return;
            }
            if (null != mLelinkHelper) {
                mLelinkHelper.addPinCodeServiceInfo(pinCodeStr);
            }
        } else if (id == R.id.btn_play) {
            play();
        } else if (id == R.id.btn_pause) {
            if (null != mLelinkHelper && null != connectInfos && !connectInfos.isEmpty()) {
                Logger.test(TAG, "pause click");
                isPause = true;
                mLelinkHelper.pause();
            }
        } else if (id == R.id.btn_stop) {
            if (null != mLelinkHelper && null != connectInfos && !connectInfos.isEmpty()) {
                Logger.test(TAG, "stop click");
                mLelinkHelper.stop();
            } else {
                ToastUtil.show(mContext, "请先连接设备");
            }
        } else if (id == R.id.btn_volume_up) {
            if (null != mLelinkHelper && null != connectInfos && !connectInfos.isEmpty()) {
                Logger.test(TAG, "volumeUp click");
                mLelinkHelper.voulumeUp();
            } else {
                ToastUtil.show(mContext, "请先连接设备");
            }
        } else if (id == R.id.btn_volume_down) {
            if (null != mLelinkHelper && null != connectInfos && !connectInfos.isEmpty()) {
                Logger.test(TAG, "volumeDown click");
                mLelinkHelper.voulumeDown();
            } else {
                ToastUtil.show(mContext, "请先连接设备");
            }
        } else if (id == R.id.btn_start_mirror) {
            startMirror();
        } else if (id == R.id.btn_stop_mirror) {
            stopMirror();
        } else if (id == R.id.btn_set_ad_listener) {
            if (null != mLelinkHelper) {
                mLelinkHelper.setInteractiveAdListener();
            }
        } else if (id == R.id.btn_report_ad_show) {
            if (null != mLelinkHelper) {
                mLelinkHelper.onInteractiveAdShow();
            }
        } else if (id == R.id.btn_report_ad_end) {
            if (null != mLelinkHelper) {
                mLelinkHelper.onInteractiveAdClosed();
            }

            if (null != mLelinkHelper) {
                mLelinkHelper.sendRelevantErrorInfo();
            }
        } else if (id == R.id.btn_send_error_info) {
            if (null != mLelinkHelper) {
                mLelinkHelper.sendRelevantErrorInfo();
            }
        } else if (id == R.id.btn_send_passth_info) {
            if (null != mLelinkHelper) {
                if (mAppidEdit.getText() != null && !TextUtils.isEmpty(mAppidEdit.getText().toString())) {
                    mLelinkHelper.sendRelevantInfo(mAppidEdit.getText().toString(), mCheckBox.isChecked());
                }
            }
        } else if (id == R.id.btn_send_mediaasset_info) {
            if (null != mLelinkHelper) {
                mLelinkHelper.playNetMediaAndPassthMediaAsset(mEtNetVideo.getText().toString(), AllCast.MEDIA_TYPE_VIDEO);
            }
        } else if (id == R.id.btn_send_header_info) {
            if (null != mLelinkHelper) {
                mLelinkHelper.playNetMediaAndPassthHeader(mEtNetVideo.getText().toString(), AllCast.MEDIA_TYPE_VIDEO);
            }
        } else if (id == R.id.btn_send_lebo_passth_info) {
            if (null != mLelinkHelper) {
                if (mAppidEdit.getText() != null && !TextUtils.isEmpty(mAppidEdit.getText().toString())) {
                    mLelinkHelper.sendLeboRelevantInfo(mAppidEdit.getText().toString(), mCheckBox.isChecked());
                }
            }
        } else if (id == R.id.btn_loop_mode) {
            if (null != mLelinkHelper) {
                int checkedId = mRadioGroup.getCheckedRadioButtonId();
                if (checkedId == R.id.rb_local_video) {
                    String url = mEtLocalVideo.getText().toString();
                    mLelinkHelper.startWithLoopMode(url, true);
                } else if (checkedId == R.id.rb_net_video) {
                    String url = mEtNetVideo.getText().toString();
                    mLelinkHelper.startWithLoopMode(url, false);
                } else {
                    ToastUtil.show(mContext, "目前只支持视频播放");
                }
            }
        } else if (id == R.id.btn_3rd_monitor) {
            if (null != mLelinkHelper) {
                mLelinkHelper.startNetVideoWith3rdMonitor(NET_VIDEO_URL);
            }
        } else if (id == R.id.btn_pushbtn_click) {
            if (mLelinkHelper != null) {
                mLelinkHelper.onPushButtonClick();
            }
        } else if (id == R.id.btn_list_gone) {
            if (mLelinkHelper != null) {
                mLelinkHelper.onBrowseListGone();
            }
        } else if (id == R.id.send_danmaku) {
            if (mLelinkHelper != null) {
                mLelinkHelper.sendDanmaku();
            }
        } else if (id == R.id.danmaku_settings) {
            if (dialog == null) {
                createDanmakuPropertyDialog();
            }
            dialog.show();
        } else if (id == R.id.btn_screenshot) {
            mLelinkHelper.startScreenShot();
        }
    }

    public void showScreenCodeDialog() {
        final EditText editText = new EditText(this);
        new AlertDialog.Builder(this).setTitle("请输入投屏码")
                .setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (TextUtils.isEmpty(editText.getText().toString())) {
                            ToastUtil.show(mContext, "屏幕码为空");
                            return;
                        }
                        mScreencode = editText.getText().toString();
                        if (isPlayMirror) {
                            startMirror();
                        } else {
                            play();
                        }
                    }
                }).setNegativeButton("取消", null).show();
    }

    private Dialog dialog;
    private Spinner switchSpinner, linesSpinner, speedSpinner;

    private void createDanmakuPropertyDialog() {
        dialog = new Dialog(this, R.style.Theme_AppCompat_Dialog);
        View inflate = LayoutInflater.from(this).inflate(R.layout.danmaku_settings_dialog, null);
        dialog.setContentView(inflate);
        switchSpinner = (Spinner) inflate.findViewById(R.id.spinner_1);
        linesSpinner = (Spinner) inflate.findViewById(R.id.spinner_2);
        speedSpinner = (Spinner) inflate.findViewById(R.id.spinner_3);

        List<String> swithcList = new ArrayList<>();
        swithcList.add("开");
        swithcList.add("关");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                MainActivity.this, android.R.layout.simple_spinner_item, swithcList);
        switchSpinner.setAdapter(adapter);
        List<String> linesList = new ArrayList<>();
        for (int i = 0; i < DanmakuPropertyBean.Lines.values().length; i++) {
            linesList.add((i < 9 ? "0" + (i + 1) : (i + 1)) + "  行");
        }
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(
                MainActivity.this, android.R.layout.simple_spinner_item, linesList);
        linesSpinner.setAdapter(adapter2);

        List<String> speedList = new ArrayList<>();
        for (int i = 0; i < DanmakuPropertyBean.Speed.values().length; i++) {
            speedList.add((i < 9 ? "0" + (i + 1) : (i + 1)) + "  速度 ");
        }
        ArrayAdapter<String> adapter3 = new ArrayAdapter<>(
                MainActivity.this, android.R.layout.simple_spinner_item, speedList);
        speedSpinner.setAdapter(adapter3);

        Button btnCancel = (Button) inflate.findViewById(R.id.cancle);
        Button btnConfirm = (Button) inflate.findViewById(R.id.confirm);
        btnCancel.setOnClickListener(onClickListener);
        btnConfirm.setOnClickListener(onClickListener);
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        dialogWindow.setAttributes(lp);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.confirm) {
                DanmakuPropertyBean mDanmakuProperty = new DanmakuPropertyBean();
                boolean swtc = switchSpinner.getSelectedItemPosition() == 0 ? true : false;
                mDanmakuProperty.setSwitch(swtc);
                mDanmakuProperty.setLines(DanmakuPropertyBean.Lines.values()[linesSpinner.getSelectedItemPosition()]);
                mDanmakuProperty.setSpeed(DanmakuPropertyBean.Speed.values()[speedSpinner.getSelectedItemPosition()]);
                Logger.d(TAG, mDanmakuProperty.toJson(1));
                mLelinkHelper.sendDanmakuProperty(mDanmakuProperty);
            }
            dialog.dismiss();
        }

    };

    private SeekBar.OnSeekBarChangeListener mProgressChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // ignore
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // ignore
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            if (seekBar.getId() == R.id.seekbar_progress) {
                Logger.test(TAG, "seek click:" + progress);
                ToastUtil.show(mContext, "seek到" + progress);
                mLelinkHelper.seekTo(progress);
            } else if (seekBar.getId() == R.id.seekbar_volume) {
                Logger.test(TAG, "set volume:" + progress);
                ToastUtil.show(mContext, "设置音量到" + progress);
                mLelinkHelper.setVolume(progress);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_MUST_PERMISSION) {
            boolean denied = false;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    denied = true;
                }
            }
            if (denied) {
                // 拒绝
                ToastUtil.show(mContext, "您拒绝了权限");
            } else {
                // 允许
                initLelinkHelper();
            }
        } else if (requestCode == CameraPermissionCompat.REQUEST_CODE_CAMERA) {
            boolean denied = false;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    denied = true;
                }
            }
            if (denied) {
                // 拒绝
                ToastUtil.show(mContext, "请打开此应用的摄像头权限！");
            } else {
                startCaptureActivity();
            }
        } else if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            boolean denied = false;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    denied = true;
                }
            }
            if (denied) {
                // 拒绝
                ToastUtil.show(mContext, "您录制音频的权限");
            } else {
                // 允许
                startMirror();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA_PERMISSION) {
                String scanResult = data.getStringExtra(CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN);
                Logger.i(TAG, "scanResult-->" + scanResult);
                mLelinkHelper.addQRServiceInfo(scanResult, new IQRCodeListener() {

                    @Override
                    public void onParceResult(int resultCode, LelinkServiceInfo info) {
                        if (resultCode == IQRCodeListener.PARCE_SUCCESS) {
                            mBrowseAdapter.setSelectInfo(info);
                            connect(info);
                            mBrowseAdapter.notifyDataSetChanged();
                        }
                    }

                });

            }
        }
    }

    @Override
    protected void onDestroy() {
        Logger.i(TAG, "onDestroy");
        if (null != mNetworkReceiver) {
            unregisterReceiver(mNetworkReceiver);
            mNetworkReceiver = null;
        }
        if (mLelinkHelper != null) {
            mLelinkHelper.stop();
        }
        if (null != mSDKManager) {
            mSDKManager.stopMonitor();
        }
        super.onDestroy();
    }

    private static class NetworkReceiver extends BroadcastReceiver {

        private WeakReference<MainActivity> mReference;

        public NetworkReceiver(MainActivity pReference) {
            mReference = new WeakReference<>(pReference);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == mReference || null == mReference.get()) {
                return;
            }
            MainActivity mainActivity = mReference.get();
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equalsIgnoreCase(action) ||
                    MainActivity.WIFI_AP_STATE_CHANGED_ACTION.equalsIgnoreCase(action)) {
                mainActivity.refreshWifiName();
            }
        }
    }

    private IUIUpdateListener mUIUpdateListener = new IUIUpdateListener() {

        @Override
        public void onUpdate(int what, MessageDeatail deatail) {
            Logger.d(TAG, "onUpdateText : " + deatail.text + "\n\n");
            Logger.d(TAG, "IUIUpdateListener state:" + what + " text:" + deatail.text);
            switch (what) {
                case IUIUpdateListener.STATE_SEARCH_SUCCESS:
                    if (isFirstBrowse) {
                        isFirstBrowse = false;
                        ToastUtil.show(mContext, "搜索成功");
                        Logger.test(TAG, "搜索成功");
                    }
                    if (null != mDelayHandler) {
                        mDelayHandler.removeCallbacksAndMessages(null);
                        mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_SUCCESS,
                                TimeUnit.SECONDS.toMillis(1));
                    }
                    break;
                case IUIUpdateListener.STATE_SEARCH_ERROR:
                    ToastUtil.show(mContext, "Auth错误");
                    break;
                case IUIUpdateListener.STATE_SEARCH_NO_RESULT:
                    if (null != mDelayHandler) {
                        mDelayHandler.removeCallbacksAndMessages(null);
                        mDelayHandler.sendEmptyMessageDelayed(IUIUpdateListener.STATE_SEARCH_SUCCESS,
                                TimeUnit.SECONDS.toMillis(1));
                    }
                    break;
                case IUIUpdateListener.STATE_CONNECT_SUCCESS:
                    Logger.test(TAG, "connect success:" + deatail.text);
                    // 刷新button
                    refreshMediaButton((LelinkServiceInfo) deatail.obj);
                    // 更新列表
                    updateConnectAdapter();
                    Logger.d(TAG, "ToastUtil " + deatail.text);
                    ToastUtil.show(mContext, deatail.text);
                    break;
                case IUIUpdateListener.STATE_DISCONNECT:
                    Logger.test(TAG, "disConnect success:" + deatail.text);
                    Logger.d(TAG, "ToastUtil " + deatail.text);
                    ToastUtil.show(mContext, deatail.text);
                    mBrowseAdapter.setSelectInfo(null);
                    mBrowseAdapter.notifyDataSetChanged();
                    // 更新列表
                    updateConnectAdapter();
                    break;
                case IUIUpdateListener.STATE_CONNECT_FAILURE:
                    Logger.test(TAG, "connect failure:" + deatail.text);
                    Logger.d(TAG, "ToastUtil " + deatail.text);
                    ToastUtil.show(mContext, deatail.text);
                    mBrowseAdapter.setSelectInfo(null);
                    mBrowseAdapter.notifyDataSetChanged();
                    // 更新列表
                    updateConnectAdapter();
                    break;
                case IUIUpdateListener.STATE_PLAY:
                    Logger.test(TAG, "callback play");
                    isPause = false;
                    Logger.d(TAG, "ToastUtil 开始播放");
                    ToastUtil.show(mContext, "开始播放");
                    break;
                case IUIUpdateListener.STATE_LOADING:
                    Logger.test(TAG, "callback loading");
                    isPause = false;
                    Logger.d(TAG, "ToastUtil 开始加载");
                    ToastUtil.show(mContext, "开始加载");
                    break;
                case IUIUpdateListener.STATE_PAUSE:
                    Logger.test(TAG, "callback pause");
                    Logger.d(TAG, "ToastUtil 暂停播放");
                    ToastUtil.show(mContext, "暂停播放");
                    isPause = true;
                    break;
                case IUIUpdateListener.STATE_STOP:
                    Logger.test(TAG, "callback stop");
                    isPause = false;
                    Logger.d(TAG, "ToastUtil 播放结束");
                    ToastUtil.show(mContext, "播放结束");
                    break;
                case IUIUpdateListener.STATE_SEEK:
                    Logger.test(TAG, "callback seek:" + deatail.text);
                    Logger.d(TAG, "ToastUtil seek完成:" + deatail.text);
                    ToastUtil.show(mContext, "seek完成" + deatail.text);
                    break;
                case IUIUpdateListener.STATE_PLAY_ERROR:
                    Logger.test(TAG, "callback error:" + deatail.text);
                    ToastUtil.show(mContext, "播放错误：" + deatail.text);
                    break;
                case IUIUpdateListener.STATE_POSITION_UPDATE:
                    Logger.test(TAG, "callback position update:" + deatail.text);
                    long[] arr = (long[]) deatail.obj;
                    long duration = arr[0];
                    long position = arr[1];
                    Logger.d(TAG, "ToastUtil 总长度：" + duration + " 当前进度:" + position);
                    mProgressBar.setMax((int) duration);
                    mProgressBar.setProgress((int) position);
                    break;
                case IUIUpdateListener.STATE_COMPLETION:
                    Logger.test(TAG, "callback completion");
                    Logger.d(TAG, "ToastUtil 播放完成");
                    ToastUtil.show(mContext, "播放完成");
                    break;
                case IUIUpdateListener.STATE_INPUT_SCREENCODE:
                    Logger.test(TAG, "input screencode");
                    ToastUtil.show(mContext, deatail.text);
                    showScreenCodeDialog();
                    break;
                case IUIUpdateListener.RELEVANCE_DATA_UNSUPPORT:
                    Logger.test(TAG, "unsupport relevance data");
                    ToastUtil.show(mContext, deatail.text);
                    break;
                case IUIUpdateListener.STATE_SCREENSHOT:
                    Logger.test(TAG, "unsupport relevance data");
                    ToastUtil.show(mContext, deatail.text);
                    break;
            }
        }

    };

    private static class UIHandler extends Handler {

        private WeakReference<MainActivity> mReference;

        UIHandler(MainActivity reference) {
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mReference.get();
            if (mainActivity == null) {
                return;
            }
            switch (msg.what) {
                case IUIUpdateListener.STATE_SEARCH_SUCCESS:
                    mainActivity.updateBrowseAdapter();
                    break;
            }
            super.handleMessage(msg);
        }
    }

}