package com.dji.FPVDemo.interf;

import android.graphics.RectF;

/**
 * 点击画框区域后，确认是否跟踪
 */
public interface ConfirmLocationForTensorFlow {

    public void confirmForTracking(RectF rectFForFrame);
}
