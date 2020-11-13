package com.dji.FPVDemo.utils;

import android.content.Context;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 公共工具
 * @author dongsiyuan
 * @date 2020/11/12 15:08
 */
public class CommonUtils {

    public static String currentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        return sdf.format(curDate);
    }

    public static void showToast(Context context, String s) {
        Toast.makeText(context.getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }
}
