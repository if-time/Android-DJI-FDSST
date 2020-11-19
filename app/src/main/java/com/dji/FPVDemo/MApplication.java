package com.dji.FPVDemo;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.dianping.logan.Logan;
import com.dianping.logan.LoganConfig;
import com.dianping.logan.OnLoganProtocolStatus;
import com.dji.FPVDemo.handler.AppCrashHandler;
import com.secneo.sdk.Helper;
import com.tencent.bugly.crashreport.CrashReport;
import com.xsj.crasheye.Crasheye;
import com.zxy.recovery.callback.RecoveryCallback;
import com.zxy.recovery.core.Recovery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * 
 * @author dongsiyuan
 * @date 2020/11/10 11:22
 */
public class MApplication extends Application {

    private static Context sContext;

    private DJIApplication djiApplication;
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (djiApplication == null) {
            djiApplication = new DJIApplication();
            djiApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        Crasheye.init(this, "c31544d0");

        super.onCreate();
        djiApplication.onCreate();
        sContext = getApplicationContext();

        initLogan();
        initBugly();

        //崩溃界面
        initRecovery();
    }

    public static Context getContext() {
        return sContext;
    }

    private void initLogan() {
        LoganConfig config = new LoganConfig.Builder()
                .setCachePath(getApplicationContext().getFilesDir().getAbsolutePath())
                .setPath(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()
                        + File.separator + "logan_v2")
                .setEncryptKey16("0123456789012345".getBytes())
                .setEncryptIV16("0123456789012345".getBytes())
                .build();
        Logan.setDebug(true);
        Logan.setOnLoganProtocolStatus(new OnLoganProtocolStatus() {
            @Override
            public void loganProtocolStatus(String cmd, int code) {
                Log.d("dongLogan", "clogan > cmd : " + cmd + " | " + "code : " + code);
            }
        });
        Logan.init(config);
    }

    private void initBugly() {
        // 获取当前包名
        String packageName = sContext.getPackageName();
        // 获取当前进程名
        String processName = getProcessName(android.os.Process.myPid());
        // 设置是否为上报进程
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(sContext);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));
        // 初始化Bugly
        CrashReport.initCrashReport(sContext, "4e60bd54eb", true, strategy);
    }

    private void initRecovery() {
        Recovery.getInstance()
                .debug(BuildConfig.DEBUG)
                .recoverInBackground(true)
                .recoverStack(true)
                .mainPage(MainActivity.class)
                .recoverEnabled(true)
                .callback(new MyCrashCallback())
                .silent(false, Recovery.SilentMode.RECOVER_ACTIVITY_STACK)
//                .skip(TestActivity.class)
                .init(this);
        AppCrashHandler.register();
    }

    static final class MyCrashCallback implements RecoveryCallback {
        @Override
        public void stackTrace(String exceptionMessage) {
            Log.e("wzt", "exceptionMessage:" + exceptionMessage);
        }

        @Override
        public void cause(String cause) {
            Log.e("wzt", "cause:" + cause);
        }

        @Override
        public void exception(String exceptionType, String throwClassName, String throwMethodName, int throwLineNumber) {
            Log.e("wzt", "exceptionClassName:" + exceptionType);
            Log.e("wzt", "throwClassName:" + throwClassName);
            Log.e("wzt", "throwMethodName:" + throwMethodName);
            Log.e("wzt", "throwLineNumber:" + throwLineNumber);
        }

        @Override
        public void throwable(Throwable throwable) {

        }
    }

    /**
     * 获取进程号对应的进程名
     *
     * @param pid 进程号
     * @return 进程名
     */
    private static String getProcessName(int pid) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }
}
