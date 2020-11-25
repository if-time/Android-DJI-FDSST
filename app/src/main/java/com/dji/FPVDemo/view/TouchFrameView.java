package com.dji.FPVDemo.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.dji.FPVDemo.interf.ConfirmLocationForTracking;

/**
 * @author dongsiyuan
 * @date 2020年10月29日
 */
public class TouchFrameView extends View {

    private ConfirmLocationForTracking confirmLocationForTracking;

    private Paint mPaint;

    private RectF rectFForFrame;

    private float mXStart, mYStart;
    private float mXLast, mYLast;

    private boolean isNeedFrame = true;

    public TouchFrameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public TouchFrameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TouchFrameView(Context context) {
        super(context);
        init();
    }

    public void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0x77FF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getRawX();
            float y = event.getRawY();
            if (isNeedFrame) {
                if (isTouchPointInView(x, y)) {
                    Toast.makeText(getContext(), x + " " + y, Toast.LENGTH_LONG).show();
                    isNeedFrame = false;
                    confirmForTracking(rectFForFrame);
                    return true;
                }
            }
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isNeedFrame) {
                    touch_start(x, y);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isNeedFrame) {
                    touch_move(x, y);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawRect(mXStart, mYStart, mXLast, mYLast, mPaint);
        rectFForFrame = new RectF(mXStart, mYStart, mXLast, mYLast);
        super.onDraw(canvas);
    }

    private void touch_start(float x, float y) {
        mXStart = x;
        mYStart = y;
    }

    private void touch_move(float x, float y) {
        mXLast = x;
        mYLast = y;
    }

    /**
     * 清除框
     */
    public void clearView() {
        //重置paint
        mPaint.reset();
        mPaint.setColor(Color.TRANSPARENT);
        isNeedFrame = false;
        invalidate();
    }

    /**
     * 重新初始化画笔
     */
    public void initPaint() {
        init();
        isNeedFrame = true;
        invalidate();
    }

    /**
     * 是否需要画框
     *
     * @param isNeedFrame
     */
    public void setNeedFrame(boolean isNeedFrame) {
        this.isNeedFrame = isNeedFrame;
    }

    /**
     * 返回框的坐标
     *
     * @return
     */
    public RectF getFrameLocation() {
        return rectFForFrame;
    }


    /**
     * (x,y)是否在view的区域内
     *
     * @param x
     * @param y
     * @return
     */
    private Boolean isTouchPointInView(float x, float y) {
        return rectFForFrame.contains(x, y);
    }


    public void setConfirmLocationForTracking(ConfirmLocationForTracking confirmLocationForTracking) {
        this.confirmLocationForTracking = confirmLocationForTracking;
    }

    /**
     * 返回MainActivity
     */
    public void confirmForTracking(RectF rectFForFrame) {
        confirmLocationForTracking.confirmForTracking(rectFForFrame);
    }
}
