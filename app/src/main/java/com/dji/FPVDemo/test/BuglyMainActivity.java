package com.dji.FPVDemo.test;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.dji.FPVDemo.R;
import com.tencent.bugly.crashreport.CrashReport;

/**
 * bugly测试
 * @author dongsiyuan
 * @date 2020/11/10 11:25
 */
public class BuglyMainActivity extends AppCompatActivity {

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bugly_main);

        button = findViewById(R.id.btn);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CrashReport.testJavaCrash();
            }
        });

    }
}