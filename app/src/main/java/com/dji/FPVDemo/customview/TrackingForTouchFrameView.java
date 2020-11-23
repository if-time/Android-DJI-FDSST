package com.dji.FPVDemo.customview;

import android.app.Activity;
import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dji.FPVDemo.DJIMainActivity;
import com.dji.FPVDemo.R;
import com.dji.FPVDemo.interf.ConfirmLocationForTracking;
import com.dji.FPVDemo.utils.CommonUtils;
import com.dji.FPVDemo.view.TouchFrameView;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 通过点击按钮在界面添加一个view进行画框
 *
 * @author dongsiyuan
 * @since 2020/11/20 16:41
 */
public class TrackingForTouchFrameView extends CommonView {

    private View touchFrameView;

    TouchFrameView tpvTouchFrame;

    @BindView(R.id.btnAddView)
    Button btnAddView;

    LinearLayout llTouchFrameViewContainer;
    DJIMainActivity activity;

    public TrackingForTouchFrameView(@NonNull Context context) {
        super(context);
    }

    public TrackingForTouchFrameView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TrackingForTouchFrameView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int configLayoutId() {
        return R.layout.item_layout_tracking_touch_frame;
    }

    @OnClick(R.id.btnAddView)
    public void addTouchFrameView() {
        CommonUtils.showToast(activity, "OnClick回调");
        touchFrameView = LayoutInflater.from(activity).inflate(R.layout.inflater_touch_frame, null);

        tpvTouchFrame = touchFrameView.findViewById(R.id.tpvTouchFrame);

        llTouchFrameViewContainer.addView(touchFrameView,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 来自TouchPaintView
        // 确定点击到了画框区域
        tpvTouchFrame.setConfirmLocationForTracking(new ConfirmLocationForTracking() {
            @Override
            public void confirmForTracking(final RectF rectFForFrame) {
                CommonUtils.showToast(activity, "TrackingForTouchFrameView回调");
                activity.initTrackingAlgorithm(rectFForFrame);
            }
        });
    }

    /**
     * touchFrameView
     *
     * @param activity
     * @param llTouchFrameViewContainer
     */
    public void addTouchFrameView(DJIMainActivity activity, LinearLayout llTouchFrameViewContainer) {
        this.activity = activity;
        this.llTouchFrameViewContainer = llTouchFrameViewContainer;
    }

    public void clearView() {
        if (tpvTouchFrame != null) {
            tpvTouchFrame.clearView();
        }
    }
}
