package com.dji.FPVDemo.handler;

import androidx.annotation.NonNull;

public class AppCrashHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        uncaughtExceptionHandler.uncaughtException(t, e);
    }

    public static void register() {
        Thread.setDefaultUncaughtExceptionHandler(new AppCrashHandler());
    }

}
