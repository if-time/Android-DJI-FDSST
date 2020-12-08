package com.dji.FPVDemo.test.album;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * 为了解决photoview嵌套在部分父控件时闪退的bug，github上提供的解决方案
 */
public class ShowImagesViewPager extends ViewPager {
    public ShowImagesViewPager(Context context) {
        this(context, null);
    }

    public ShowImagesViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            //uncomment if you really want to see these errors
            //e.printStackTrace();
            return false;
        }
    }

    /**
     * 此类只是为了解决双指缩放过小可能会出现异常的问题
     *
     * @param arg0
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        try {
            return super.onTouchEvent(arg0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
