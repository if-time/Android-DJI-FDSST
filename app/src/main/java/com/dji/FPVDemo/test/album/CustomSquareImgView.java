package com.dji.FPVDemo.test.album;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CustomSquareImgView extends ImageView {
    public CustomSquareImgView(Context context) {
        super(context);
    }

    public CustomSquareImgView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSquareImgView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomSquareImgView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), getDefaultSize(0, heightMeasureSpec));
        int childwid = getMeasuredWidth();
        heightMeasureSpec = widthMeasureSpec = MeasureSpec.makeMeasureSpec(childwid, MeasureSpec.EXACTLY);


        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
