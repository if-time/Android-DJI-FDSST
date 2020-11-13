package com.dji.FPVDemo.test.bugly;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dianping.logan.Logan;
import com.dianping.logan.SendLogCallback;
import com.dji.FPVDemo.R;
import com.tencent.bugly.crashreport.CrashReport;

import java.text.SimpleDateFormat;
import java.util.Date;

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

        Logan.w("test logan", 1);

        final String url = "https://openlogan.inf.test.sankuai.com/logan/upload.json";
        Logan.s(url, currentTime(), "testAppId", "testUnionid", "testdDviceId", "testBuildVersion", "testAppVersion", new SendLogCallback() {
            @Override
            public void onLogSendCompleted(int statusCode, byte[] data) {
                final String resultData = data != null ? new String(data) : "";
                Log.d("dongLogan", "upload result, httpCode: " + statusCode + ", details: " + resultData);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CrashReport.testJavaCrash();
            }
        });

    }

    private String currentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        return sdf.format(curDate);
    }
}