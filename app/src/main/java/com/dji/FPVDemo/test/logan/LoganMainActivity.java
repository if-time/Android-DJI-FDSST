package com.dji.FPVDemo.test.logan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dianping.logan.Logan;
import com.dianping.logan.SendLogCallback;
import com.dji.FPVDemo.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class LoganMainActivity extends AppCompatActivity {

    private static final String TAG = LoganMainActivity.class.getName();

    private TextView mTvInfo;
    private EditText mEditIp;
    private RealSendLogRunnable mSendLogRunnable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logan_main);

        initView();
        mSendLogRunnable = new RealSendLogRunnable();
    }

    private void initView() {
        Button button = (Button) findViewById(R.id.write_btn);
        Button batchBtn = (Button) findViewById(R.id.write_batch_btn);
        Button sendBtn = (Button) findViewById(R.id.send_btn);
        Button logFileBtn = (Button) findViewById(R.id.show_log_file_btn);
        mTvInfo = (TextView) findViewById(R.id.info);
        mEditIp = (EditText) findViewById(R.id.send_ip);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logan.w("啊哈哈哈哈66666", 2);
            }
        });
        batchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loganTest();
            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loganSend();
            }
        });
        logFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loganFilesInfo();
            }
        });
        findViewById(R.id.send_btn_default).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loganSendByDefault();
            }
        });
    }

    private void loganTest() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    for (int i = 0; i < 9; i++) {
                        Log.d(TAG, "times : " + i);
                        Logan.w(String.valueOf(i), 1);
                        Thread.sleep(5);
                    }
                    Log.d(TAG, "write log end");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void loganSend() {
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");
        String d = dataFormat.format(new Date(System.currentTimeMillis()));
        String[] temp = new String[1];
        temp[0] = d;
        String ip = mEditIp.getText().toString().trim();
        if (!TextUtils.isEmpty(ip)) {
            mSendLogRunnable.setIp(ip);
        }
        Logan.s(temp, mSendLogRunnable);
    }

    private void loganFilesInfo() {
        Map<String, Long> map = Logan.getAllFilesInfo();
        if (map != null) {
            StringBuilder info = new StringBuilder();
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                info.append("文件日期：").append(entry.getKey()).append("  文件大小（bytes）：").append(
                        entry.getValue()).append("\n");
            }
            mTvInfo.setText(info.toString());
        }
    }

    private void loganSendByDefault() {
        String buildVersion = "";
        String appVersion = "";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            appVersion = pInfo.versionName;
            buildVersion = String.valueOf(pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        final String url = "http://211.87.231.41:8011/logan-server/logan/upload.json";
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String date = dataFormat.format(new Date(System.currentTimeMillis()));
        Log.i("donglogan", "url: " + url + " date: " + date + " buildVersion: " + buildVersion + " appVersion: " + appVersion);
        Logan.s(url, date, "2", "com.dji", "deviceIdsua", buildVersion, appVersion, new SendLogCallback() {
            @Override
            public void onLogSendCompleted(int statusCode, byte[] data) {
                final String resultData = data != null ? new String(data) : "";
                Log.d("donglogan", "日志上传结果, http状态码: " + statusCode + ", 详细: " + resultData);
            }
        });
    }
}