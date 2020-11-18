package com.dji.FPVDemo.test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.dji.FPVDemo.R;
import com.dji.FPVDemo.test.bugly.BuglyMainActivity;
import com.dji.FPVDemo.test.drawerlayout.CustomSideSlipMenuActivity;
import com.dji.FPVDemo.test.drawerlayout.DrawerLayoutActivity;
import com.dji.FPVDemo.test.drawerlayout.DrawerlayoutNaviMainActivity;
import com.dji.FPVDemo.test.imageopencv.PictureConversionTestActivity;
import com.dji.FPVDemo.test.logan.LoganMainActivity;
import com.dji.FPVDemo.test.ncnnvulkan.camera2.CameraMainActivity;
import com.dji.FPVDemo.test.ncnnvulkan.WelcomeActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dongsiyuan
 * @date 2020/11/13 16:33
 */
public class RecyclerViewMainActivity extends AppCompatActivity {

    private final List<String> buttonList = new ArrayList<>();

    ButtonAdapter.OnItemClickListener onItemClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_main);
        initButtons();
        initListener();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rvRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        ButtonAdapter adapter = new ButtonAdapter(buttonList, onItemClickListener);
        recyclerView.setAdapter(adapter);
    }

    private void initListener() {
        onItemClickListener = new ButtonAdapter.OnItemClickListener() {
            @Override
            public void onClick(int pos) {
                switch (pos) {
                    case 0:
                        Intent buglyIntent = new Intent(RecyclerViewMainActivity.this, BuglyMainActivity.class);
                        startActivity(buglyIntent);
                        break;
                    case 1:
                        Intent pictureConversionIntent = new Intent(RecyclerViewMainActivity.this, PictureConversionTestActivity.class);
                        startActivity(pictureConversionIntent);
                        break;
                    case 2:
                        Intent loganIntent = new Intent(RecyclerViewMainActivity.this, LoganMainActivity.class);
                        startActivity(loganIntent);
                        break;
                    case 3:
                        Intent customSideSlipMenuIntent = new Intent(RecyclerViewMainActivity.this, CustomSideSlipMenuActivity.class);
                        startActivity(customSideSlipMenuIntent);
                        break;
                    case 4:
                        Intent drawerlayoutNaviIntent = new Intent(RecyclerViewMainActivity.this, DrawerlayoutNaviMainActivity.class);
                        startActivity(drawerlayoutNaviIntent);
                        break;

                    case 5:
                        Intent drawerlayoutIntent = new Intent(RecyclerViewMainActivity.this, DrawerLayoutActivity.class);
                        startActivity(drawerlayoutIntent);
                        break;
                    case 6:
                        Intent ncnnVulkanIntent = new Intent(RecyclerViewMainActivity.this, WelcomeActivity.class);
                        startActivity(ncnnVulkanIntent);
                        break;
                    case 7:
                        Intent cameraMainIntent = new Intent(RecyclerViewMainActivity.this, CameraMainActivity.class);
                        startActivity(cameraMainIntent);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void initButtons() {

        String buglyString = "Bugly";
        buttonList.add(buglyString);

        String pictureConversionString = "PictureConversion";
        buttonList.add(pictureConversionString);

        String loganString = "Logan";
        buttonList.add(loganString);

        String customSideSlipMenuActivityString = "CustomSideSlipMenuActivity";
        buttonList.add(customSideSlipMenuActivityString);

        String drawerlayoutNaviString = "DrawerlayoutNaviMainActivity";
        buttonList.add(drawerlayoutNaviString);

        String drawerlayoutString = "DrawerlayoutActivity";
        buttonList.add(drawerlayoutString);

        String ncnnVulkanString = "NcnnVulkanMainActivity";
        buttonList.add(ncnnVulkanString);

        String cameraMainString = "CameraMainActivity";
        buttonList.add(cameraMainString);

    }
}