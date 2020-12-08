package com.dji.FPVDemo.test.album.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.dji.FPVDemo.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PreviewPhotoActivity extends AppCompatActivity {
    String TAG = "shengmingzq";
    
    private ImageView iv_fangda_img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_photo);
        iv_fangda_img = findViewById(R.id.iv_fangda_img);
        iv_fangda_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 注意这里不使用finish
                ActivityCompat.finishAfterTransition(PreviewPhotoActivity.this);
            }
        });
        Log.i(TAG, "onCreate: PreviewPhotoActivity");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("shengmingzq", "onStart: PreviewPhotoActivity");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("shengmingzq", "onPause: PreviewPhotoActivity");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: PreviewPhotoActivity");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: PreviewPhotoActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: PreviewPhotoActivity");
    }

}
