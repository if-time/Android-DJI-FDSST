package com.dji.FPVDemo.utils.dialogs;

import android.content.Context;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.Calendar;

/**
 * 弹出窗
 * @author dongsiyuan
 * @time 2020/11/2 2:27
 */
public class DialogUtils {

    /**
     * 选择时间的弹出窗
     */
    public static void showTimeDialog(final Context context, FragmentManager fragmentManager) {
        String titleTime = "请选择时间";
        Calendar calendarTime = Calendar.getInstance();
        DialogFragmentHelper.showTimeDialog(context, fragmentManager, titleTime, calendarTime, new IDialogResultListener<Calendar>() {
            @Override
            public void onDataResult(Calendar result) {
                showToast(context, String.valueOf(result.getTime().getDate()));
            }
        }, true);
    }

    /**
     * 输入密码的弹出窗
     */
    public static void showPasswordInsertDialog(final Context context, FragmentManager fragmentManager) {
        String titlePassword = "请输入密码";
        DialogFragmentHelper.showPasswordInsertDialog(context, fragmentManager, titlePassword, new IDialogResultListener<String>() {
            @Override
            public void onDataResult(String result) {
                showToast(context,"密码为：" + result);
            }
        }, true);
    }

    /**
     * 显示列表的弹出窗
     */
    public static void showListDialog(final Context context, FragmentManager fragmentManager, String titleList, final String[] languanges) {

        DialogFragmentHelper.showListDialog(context, fragmentManager, titleList, languanges, new IDialogResultListener<Integer>() {
            @Override
            public void onDataResult(Integer result) {
                showToast(context, languanges[result]);
            }
        }, true);
    }

    /**
     * 两个输入框的弹出窗
     */
    public static void showIntervalInsertDialog(final Context context, FragmentManager fragmentManager) {
        String title = "请输入想输入的内容";
        DialogFragmentHelper.showIntervalInsertDialog(context, fragmentManager, title, new IDialogResultListener<String[]>() {
            @Override
            public void onDataResult(String[] result) {
                showToast(context,result[0] + result[1]);
            }
        }, true);
    }

    public static void showInsertDialog(final Context context, FragmentManager fragmentManager) {
        String titleInsert  = "请输入想输入的内容";
        DialogFragmentHelper.showInsertDialog(context, fragmentManager, titleInsert, new IDialogResultListener<String>() {
            @Override
            public void onDataResult(String result) {
                showToast(context, result);
            }
        }, true);
    }

    /**
     * 选择日期的弹出窗
     */
    public void showDateDialog(final Context context, FragmentManager fragmentManager) {
        String titleDate = "请选择日期";
        Calendar calendar = Calendar.getInstance();
        DialogFragment mDialogFragment = DialogFragmentHelper.showProgress(context, fragmentManager, "正在加载中");
        mDialogFragment = DialogFragmentHelper.showDateDialog(context, fragmentManager, titleDate, calendar, new IDialogResultListener<Calendar>() {
            @Override
            public void onDataResult(Calendar result) {
                showToast(context, String.valueOf(result.getTime().getDate()));
            }
        }, true);
    }

    /**
     * 确认和取消的弹出窗
     */
    public static void showConfirmDialog(final Context context, FragmentManager fragmentManager) {
        DialogFragmentHelper.showConfirmDialog(context, fragmentManager, "是否选择 Android？", new IDialogResultListener<Integer>() {
            @Override
            public void onDataResult(Integer result) {
                showToast(context, "You Click Ok");
            }
        }, true, new CommonDialogFragment.OnDialogCancelListener() {
            @Override
            public void onCancel() {
                showToast(context,"You Click Cancel");
            }
        });
    }


    /**
     *
     * @param message 想要显示的信息
     */
    private static void showToast(Context context, String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
