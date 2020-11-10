package com.dji.FPVDemo;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.secneo.sdk.Helper;
import com.tencent.bugly.crashreport.CrashReport;
import com.xsj.crasheye.Crasheye;

import java.io.BufferedReader;
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

    public static Context getContext() {
        return sContext;
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
