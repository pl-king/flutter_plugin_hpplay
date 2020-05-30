package com.hpplay.sdk.source.test;

import android.app.Application;


/**
 * Created by Zippo on 2018/3/27.
 * Date: 2018/3/27
 * Time: 21:16:01
 */

public class MyApplication extends Application {

    private static final String BUGLY_APPID = "7f89580564";

    private static MyApplication sMyApplication;
    private LelinkHelper mLelinkHelper;

    public static MyApplication getMyApplication() {
        return sMyApplication;
    }

    public LelinkHelper getLelinkHelper() {
        return mLelinkHelper;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sMyApplication = this;
//        CrashReport.initCrashReport(getApplicationContext(), BUGLY_APPID, false);
        mLelinkHelper = LelinkHelper.getInstance(getApplicationContext());
//        CrashHandler.getInstance().init(getApplicationContext());
    }

}
