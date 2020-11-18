package com.dji.FPVDemo.test.ncnnvulkan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dji.FPVDemo.R;

/**
 * ncnnvulkan yolov4 tiny Thermal
 *
 * @author dongsiyuan
 * @date 2020/11/17 17:11
 */

public class WelcomeActivity extends AppCompatActivity {

    private ToggleButton tbUseGpu;

    private Button btnYolov4Tiny;

    private boolean useGPU = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tbUseGpu = findViewById(R.id.tbUseGpu);
        tbUseGpu.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                useGPU = isChecked;
//                NcnnMainActivity.USE_GPU = useGPU;

                if (useGPU) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                    builder.setTitle("Warning");
                    builder.setMessage("If the GPU is too old, it may not work well in GPU mode.");
                    builder.setCancelable(true);
                    builder.setPositiveButton("OK", null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    Toast.makeText(WelcomeActivity.this, "CPU mode", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnYolov4Tiny = findViewById(R.id.btnYolov4Tiny);
        btnYolov4Tiny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, NcnnMainActivity.class);
                WelcomeActivity.this.startActivity(intent);
            }
        });
    }
}
