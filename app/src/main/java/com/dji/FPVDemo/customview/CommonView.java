package com.dji.FPVDemo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;

import butterknife.ButterKnife;

/**
 * 自定义view的基类
 *
 * @author dongsiyuan
 * @since 2020/11/20 16:00
 */

public abstract class CommonView<D> extends RelativeLayout {

    private D data;

    public CommonView(@NonNull Context context) {
        this(context, null);
    }

    public CommonView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommonView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        int[] configAttrs = configAttrs();
        if (configAttrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, configAttrs);
            if (typedArray != null) {
                onApplyAttrs(typedArray);
                typedArray.recycle();
            }
        }
    }

    protected void init() {
        int layoutId = configLayoutId();
        View view = View.inflate(getContext(), layoutId, this);
        ButterKnife.bind(this, view);
    }

    /**
     * 配置根布局
     *
     * @return
     */
    protected abstract @LayoutRes
    int configLayoutId();

    /**
     * 配置自定义属性
     *
     * @return
     */
    protected @StyleableRes
    int[] configAttrs() {
        return new int[]{};
    }

    /**
     * 应用自定义属性
     *
     * @param attrs
     */
    protected void onApplyAttrs(TypedArray attrs) {

    }

    public void setData(D data) {
        this.data = data;
    }

    public D getData() {
        return this.data;
    }
}
