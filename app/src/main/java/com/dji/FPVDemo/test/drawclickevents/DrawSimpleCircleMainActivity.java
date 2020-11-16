package com.dji.FPVDemo.test.drawclickevents;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

import com.dji.FPVDemo.R;

/**
 * 画图的点击事件处理
 * @author dongsiyuan
 * @date 2020/11/14 16:38
 */
public class DrawSimpleCircleMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // 屏幕的分辨率
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        setContentView(new MyCircleView(this, width, height));
        Log.i("donguri", "onCreate: " +    Environment.getExternalStorageDirectory().getAbsolutePath() + "/result");
//        setContentView(R.layout.activity_draw_simple_circle_main);
    }
}