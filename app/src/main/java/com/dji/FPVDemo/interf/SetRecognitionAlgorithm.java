package com.dji.FPVDemo.interf;

import com.dji.FPVDemo.enums.TrackerTypeEnum;

/**
 * OverlayView后再添加选择算法
 *
 * @author dongsiyuan
 * @since 2020/11/24 18:32
 */
public interface SetRecognitionAlgorithm {
    void initRecognitionAlgorithm(TrackerTypeEnum.TrackerType trackerType);
}
