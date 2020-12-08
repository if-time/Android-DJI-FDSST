package com.dji.FPVDemo.test.album;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class AutoGridView extends GridView {

    public AutoGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AutoGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoGridView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 3,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
