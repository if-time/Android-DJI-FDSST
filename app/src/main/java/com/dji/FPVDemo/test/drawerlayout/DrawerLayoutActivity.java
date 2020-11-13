package com.dji.FPVDemo.test.drawerlayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.dji.FPVDemo.R;
import com.google.android.material.navigation.NavigationView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 
 * @author dongsiyuan
 * @date 2020/11/13 17:34
 */
public class DrawerLayoutActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.navigation_view)
    NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer_layout);
        ButterKnife.bind(this);

        initView();
    }

    protected void initView() {

        //监听
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View view, float v) {
                Log.i("---", "滑动中");
            }

            @Override
            public void onDrawerOpened(@NonNull View view) {
                Log.i("---", "打开");
            }

            @Override
            public void onDrawerClosed(@NonNull View view) {
                Log.i("---", "关闭");
            }

            @Override
            public void onDrawerStateChanged(int i) {
                Log.i("---", "状态改变");
            }
        });


        //NavigationView 内容点击事件
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @SuppressLint("WrongConstant")
    @OnClick({R.id.btnOpenLeft, R.id.btnOpenRight, R.id.btnCloseRight})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btnOpenLeft:
                mDrawerLayout.openDrawer(Gravity.START);
                break;
            case R.id.btnOpenRight:
                mDrawerLayout.openDrawer(Gravity.END);
                break;
            case R.id.btnCloseRight:
                mDrawerLayout.closeDrawer(Gravity.END);//关闭执行DrawerLayout
                //mDrawerLayout.closeDrawers();//关闭所有
                break;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        String title = (String) menuItem.getTitle();
        Toast.makeText(this, "点击了----- " + title, Toast.LENGTH_SHORT).show();
        return false;
    }
}