package com.dji.FPVDemo.enums;

/**
 * 把Enum从Activity中移除
 *
 * @author dongsiyuan
 * @since 2020/11/24 18:33
 */
public class TrackerTypeEnum {

    public enum TrackerType {USE_KCF, USE_FDSST, USE_TENSORFLOW, USE_TNN, USE_NCNN}

    public static TrackerType trackerType = TrackerType.USE_TENSORFLOW;
}
