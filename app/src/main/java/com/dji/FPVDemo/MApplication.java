package com.dji.FPVDemo;

import android.content.Context;

import com.secneo.sdk.Helper;


public class MApplication extends android.app.Application {

    private DJIApplication DJIApplication;
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (DJIApplication == null) {
            DJIApplication = new DJIApplication();
            DJIApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DJIApplication.onCreate();
    }

}
