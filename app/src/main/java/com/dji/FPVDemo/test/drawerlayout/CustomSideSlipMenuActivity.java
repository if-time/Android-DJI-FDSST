package com.dji.FPVDemo.test.drawerlayout;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.dji.FPVDemo.R;
import com.dji.FPVDemo.utils.DensityUtil;
import com.dji.FPVDemo.view.xcslideview.XCSlideView;

/**
 * 自定义侧滑菜单/侧滑View控件
 *
 * @author dongsiyuan
 * @date 2020/11/13 16:22
 */
public class CustomSideSlipMenuActivity extends AppCompatActivity {

    private Context mContext;
    //屏幕宽度
    private int mScreenWidth = 0;
    private XCSlideView mSlideViewLeft;
    private XCSlideView mSlideViewRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_custom_sideslip_menu);
        init();
    }

    private void init() {
        mContext = this;
        initSlideView();
    }

    private void initSlideView() {

        mScreenWidth = DensityUtil.getScreenWidthAndHeight(mContext)[0];
        View menuViewLeft = LayoutInflater.from(mContext).inflate(R.layout.layout_slideview, null);
        mSlideViewLeft = XCSlideView.create(this, XCSlideView.Positon.LEFT);
        mSlideViewLeft.setMenuView(CustomSideSlipMenuActivity.this, menuViewLeft);
        mSlideViewLeft.setMenuWidth(mScreenWidth * 7 / 9);
        Button left = (Button) findViewById(R.id.btnLeft);
        left.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!mSlideViewLeft.isShow()) {
                    mSlideViewLeft.show();
                }
            }
        });

        mSlideViewRight = XCSlideView.create(this, XCSlideView.Positon.RIGHT);
        View menuViewRight = LayoutInflater.from(mContext).inflate(R.layout.layout_slideview, null);
        mSlideViewRight.setMenuView(CustomSideSlipMenuActivity.this, menuViewRight);
        mSlideViewRight.setMenuWidth(mScreenWidth * 7 / 9);
        Button right = (Button) findViewById(R.id.btnRight);
        right.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!mSlideViewRight.isShow())
                    mSlideViewRight.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mSlideViewLeft.isShow()) {
            mSlideViewLeft.dismiss();
            return;
        }
        super.onBackPressed();
    }
}