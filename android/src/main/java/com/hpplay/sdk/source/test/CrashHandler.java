package com.hpplay.sdk.source.test;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.hpplay.sdk.source.api.BuildConfig;
import com.hpplay.sdk.source.test.utils.Logger;
import com.hpplay.sdk.source.test.utils.ToastUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CrashHandler implements UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";

    // 程序的 Context 对象
    private Context mContext;

    // 系统默认的 UncaughtException 处理类
    private UncaughtExceptionHandler mDefaultHandler;

    // 用来存储设备信息和异常信息
    private Map<String, String> mInfos = new LinkedHashMap<>();

    // 用于格式化日期,作为日志文件名的一部分
    private SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());

    /**
     * 保证只有一个 CrashHandler 实例
     */
    private CrashHandler() {
    }

    /**
     * 静态内部类
     */
    private static class SingletonHolder {
        private static final CrashHandler sInstance = new CrashHandler();
    }

    static synchronized CrashHandler getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * 初始化
     *
     * @param context 上下文环境
     */
    void init(Context context) {
        mContext = context;

        // 获取系统默认的 UncaughtException 处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        // 设置该 CrashHandler 为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当 UncaughtException 发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Logger.w(TAG, e);
            }

            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理，收集错误信息，发送错误报告等操作均在此完成
     *
     * @param ex 异常封装类
     * @return true：如果处理了该异常信息；否则返回 false
     */
    private boolean handleException(final Throwable ex) {
        if (ex == null) {
            return false;
        }

        // 使用 Toast 来显示异常信息
        ToastUtil.show(mContext, "很抱歉，程序出现异常，即将退出。");
        // 收集设备参数信息
        collectDeviceInfo();

        saveCrashInfo2File(ex);
        return true;
    }

    /**
     * 收集设备参数信息
     */
    private void collectDeviceInfo() {
        mInfos.put("versionName", BuildConfig.VERSION_NAME);
        mInfos.put("versionCode", String.valueOf(BuildConfig.VERSION_CODE));
        Field[] fields = Build.class.getDeclaredFields();
        String key;
        Object value;
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                key = field.getName();
                value = field.get(null);
                if (value instanceof String[]) {
                    value = Arrays.toString((Object[]) value);
                }
                mInfos.put(key, value.toString());
            } catch (Exception e) {
                Logger.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @param ex 异常封装类
     */
    private void saveCrashInfo2File(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : mInfos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append("=").append(value).append("\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();

        final String result = writer.toString();
        sb.append(result);

        Logger.e(TAG, "Crash:" + result);
        FileOutputStream fos = null;
        try {
            Date date = new Date();
            long timestamp = date.getTime();
            String time = mFormatter.format(date);
            String fileName = "crash-" + time + "-" + timestamp + ".log";

            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                String path = Environment.getExternalStorageDirectory().getPath() + File.separator
                        + mContext.getPackageName() + "/source/sdk/crash";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                fos = new FileOutputStream(new File(path, fileName));
                fos.write(sb.toString().getBytes());
            }
        } catch (Exception e) {
            Logger.e(TAG, "an error occured while writing file...", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Logger.w(TAG, e);
                }
            }
        }
    }

}