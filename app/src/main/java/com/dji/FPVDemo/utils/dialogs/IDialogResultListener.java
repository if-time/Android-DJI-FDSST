package com.dji.FPVDemo.utils.dialogs;

/**
 * 弹出窗
 * @author dongsiyuan
 * @time 2020/11/2 2:23
 */

public interface IDialogResultListener<T> {
    void onDataResult(T result);
}
