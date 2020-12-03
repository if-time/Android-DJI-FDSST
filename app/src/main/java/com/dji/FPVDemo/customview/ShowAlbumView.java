package com.dji.FPVDemo.customview;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ShowAlbumView extends CommonView {
    public ShowAlbumView(@NonNull Context context) {
        super(context);
    }

    public ShowAlbumView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ShowAlbumView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int configLayoutId() {
        return 0;
    }
}
