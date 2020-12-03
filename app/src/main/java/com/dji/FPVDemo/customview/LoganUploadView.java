package com.dji.FPVDemo.customview;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dianping.logan.Logan;
import com.dianping.logan.SendLogCallback;
import com.dji.FPVDemo.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Logan上传
 *
 * @author dongsiyuan
 * @since 2020/12/1 16:07
 */
public class LoganUploadView extends CommonView {

    @BindView(R.id.tvTitle)
    TextView tvTitle;
    @BindView(R.id.btnSelect)
    Button btnSelect;
    @BindView(R.id.tvInfo)
    TextView tvInfo;

    Context context;

    public void initView(Context context) {
        this.context = context;
    }

    public LoganUploadView(@NonNull Context context) {
        super(context);
    }

    public LoganUploadView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LoganUploadView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int configLayoutId() {
        return R.layout.item_layout_logan_upload;
    }

    @OnClick(R.id.btnSelect)
    public void uploadLogan() {
        loganSend();
        loganFilesInfo();
    }

    private void loganSend() {
        String buildVersion = "";
        String appVersion = "";
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            appVersion = pInfo.versionName;
            buildVersion = String.valueOf(pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        final String url = "http://211.87.231.41:8011/logan-server/logan/upload.json";
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String date = dataFormat.format(new Date(System.currentTimeMillis()));
        Logan.s(url, date, "2", "com.dji", "deviceIdsua", buildVersion, appVersion, new SendLogCallback() {
            @Override
            public void onLogSendCompleted(int statusCode, byte[] data) {
                final String resultData = data != null ? new String(data) : "";
                Log.d("donglogan", "日志上传结果, http状态码: " + statusCode + ", 详细: " + resultData);
            }
        });
    }

    private void loganFilesInfo() {
        Map<String, Long> map = Logan.getAllFilesInfo();
        if (map != null) {
            StringBuilder info = new StringBuilder();
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                info.append("文件日期：").append(entry.getKey()).append("  文件大小（bytes）：").append(
                        entry.getValue()).append("\n");
            }
            tvInfo.setText(info.toString());
        }
    }
}
