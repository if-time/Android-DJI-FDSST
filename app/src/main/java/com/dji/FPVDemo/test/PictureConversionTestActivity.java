package com.dji.FPVDemo.test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.dji.FPVDemo.R;


/**
 * 使用jni调用opencv库实现图片转换
 *
 * @author dongsiyuan
 * @time 2020/10/31 16:15
 */
public class PictureConversionTestActivity extends AppCompatActivity {

    static{
        System.loadLibrary("apriltag");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_conversion_test);
        ImageView ivOld = (ImageView) findViewById(R.id.ivOld);
        final ImageView ivNew = (ImageView) findViewById(R.id.ivNew);
        Button button = findViewById(R.id.btChange);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.zheng);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixArr = new int[width * height];
        bitmap.getPixels(pixArr, 0, width, 0, 0, width, height);
        gray(pixArr, width, height);
        final Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        newBitmap.setPixels(pixArr, 0, width, 0, 0, width, height);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivNew.setImageBitmap(newBitmap);
            }
        });
//        Log.e("dongsiyuans", "PictureConversionTestActivityresultPath: " + resultPath + "");
    }

    public native int[] gray(int[] pix, int w, int h);

}