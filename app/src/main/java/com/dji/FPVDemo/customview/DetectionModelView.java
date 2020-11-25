package com.dji.FPVDemo.customview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.dji.FPVDemo.R;
import com.dji.FPVDemo.enums.TrackerTypeEnum;
import com.dji.FPVDemo.interf.AddOverlayView;
import com.dji.FPVDemo.interf.SetRecognitionAlgorithm;
import com.dji.FPVDemo.utils.CommonUtils;
import com.dji.FPVDemo.utils.dialogs.DialogFragmentHelper;
import com.dji.FPVDemo.utils.dialogs.IDialogResultListener;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 选择识别算法
 *
 * @author dongsiyuan
 * @since 2020/11/24 15:34
 */
public class DetectionModelView extends CommonView {

    @BindView(R.id.tvTitle)
    TextView tvTitle;
    @BindView(R.id.btnSelect)
    Button btnSelect;

    Context context;
    FragmentManager fragmentManager;

    private AddOverlayView addOverlayViewCallback;

    private SetRecognitionAlgorithm setRecognitionAlgorithmCallback;

    public void initView(Context context, FragmentManager fragmentManager) {
        this.context = context;
        this.fragmentManager = fragmentManager;
    }

    public DetectionModelView(@NonNull Context context) {
        super(context);
    }

    public DetectionModelView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DetectionModelView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int configLayoutId() {
        return R.layout.item_layout_detection_model_view;
    }

    @OnClick(R.id.btnSelect)
    public void selectDetectionModel() {
        String titleList = "选择哪种识别算法？";
        final String[] languanges = new String[]{"TensorFlow", "TNN", "NCNN"};
        DialogFragmentHelper.showListDialog(context, fragmentManager, titleList, languanges, new IDialogResultListener<Integer>() {
            @Override
            public void onDataResult(Integer result) {
                CommonUtils.showToast(context, languanges[result]);
                switch (result) {
                    case 0:
                        TrackerTypeEnum.trackerType = TrackerTypeEnum.TrackerType.USE_TENSORFLOW;
                        addOverlayViewCallback();
                        initRecognitionAlgorithm(TrackerTypeEnum.trackerType);
                        break;
                    case 1:
                        TrackerTypeEnum.trackerType = TrackerTypeEnum.TrackerType.USE_TNN;
                        break;
                    default:
                        break;
                }
            }
        }, true);
    }

    public void setAddOverlayViewCallback(AddOverlayView addOverlayViewCallback) {
        this.addOverlayViewCallback = addOverlayViewCallback;
    }

    /**
     * 添加OverlayView
     */
    public void addOverlayViewCallback() {
        addOverlayViewCallback.addOverlay();
    }

    public void setRecognitionAlgorithmCallback(SetRecognitionAlgorithm setRecognitionAlgorithmCallback) {
        this.setRecognitionAlgorithmCallback = setRecognitionAlgorithmCallback;
    }

    public void initRecognitionAlgorithm(TrackerTypeEnum.TrackerType trackerType) {
        setRecognitionAlgorithmCallback.initRecognitionAlgorithm(trackerType);
    }
}
