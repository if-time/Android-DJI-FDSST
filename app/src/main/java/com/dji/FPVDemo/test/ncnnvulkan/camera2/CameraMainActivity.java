package com.dji.FPVDemo.test.ncnnvulkan.camera2;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.FPVDemo.R;
import com.dji.FPVDemo.jni.NativeHelper;
import com.dji.FPVDemo.ncnn.Box;
import com.dji.FPVDemo.test.ncnnvulkan.util.ImageUtil;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CameraMainActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener, Camera2Listener {

    private static final String TAG = "CameraMainActivity";
    private static final int ACTION_REQUEST_PERMISSIONS = 1;
    @BindView(R.id.ivImageViewForFrame)
    ImageView ivImageViewForFrame;
    private Camera2Helper camera2Helper;
    @BindView(R.id.tvCameraTexturePreview)
    TextureView tvCameraTexturePreview;
//    @BindView(R.id.tvInfoCamera)
//    TextView tvInfoCamera;
    // 用于显示原始预览数据
//    private ImageView ivOriginFrame;
//    // 用于显示和预览画面相同的图像数据
//    private ImageView ivPreviewFrame;
    // 默认打开的CAMERA
    private static final String CAMERA_ID = Camera2Helper.CAMERA_ID_BACK;
    // 图像帧数据，全局变量避免反复创建，降低gc频率
    private byte[] nv21;
    // 显示的旋转角度
    private int displayOrientation;
    // 是否手动镜像预览
    private boolean isMirrorPreview;
    // 实际打开的cameraId
    private String openedCameraId;
    // 当前获取的帧数
    private int currentIndex = 0;
    // 处理的间隔帧
    private static final int PROCESS_INTERVAL = 30;
    // 线程池
    private ExecutorService imageProcessExecutor;
    // 需要的权限
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

    private long startTime = 0;
    private long endTime = 0;
    double total_fps = 0;
    int fps_count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_main);
        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        imageProcessExecutor = Executors.newSingleThreadExecutor();
        initView();
        NativeHelper.getInstance().initNcnn(getAssets(), true, false);
    }

    private void initView() {
        ivImageViewForFrame.getLayoutParams().width = tvCameraTexturePreview.getWidth();
        ivImageViewForFrame.getLayoutParams().height = tvCameraTexturePreview.getHeight();
        tvCameraTexturePreview.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    void initCamera() {
        camera2Helper = new Camera2Helper.Builder()
                .cameraListener(this)
                .maxPreviewSize(new Point(1920, 1080))
                .minPreviewSize(new Point(1280, 720))
                .specificCameraId(CAMERA_ID)
                .context(getApplicationContext())
                .previewOn(tvCameraTexturePreview)
                .previewViewSize(new Point(tvCameraTexturePreview.getWidth(), tvCameraTexturePreview.getHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();
        camera2Helper.start();
    }

    @Override
    protected void onPause() {
        if (camera2Helper != null) {
            camera2Helper.stop();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera2Helper != null) {
            camera2Helper.start();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCameraOpened(CameraDevice cameraDevice, String cameraId, final Size previewSize, final int displayOrientation, boolean isMirror) {
        Log.i(TAG, "onCameraOpened:  previewSize = " + previewSize.getWidth() + "x" + previewSize.getHeight());
        this.displayOrientation = displayOrientation;
        this.isMirrorPreview = isMirror;
        this.openedCameraId = cameraId;
    }

    @Override
    public void onPreview(final byte[] y, final byte[] u, final byte[] v, final Size previewSize, final int stride) {
        if (currentIndex++ % PROCESS_INTERVAL == 0) {
            imageProcessExecutor.execute(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    startTime = System.currentTimeMillis();
                    if (nv21 == null) {
                        nv21 = new byte[stride * previewSize.getHeight() * 3 / 2];
                    }
                    // 回传数据是YUV422
                    if (y.length / u.length == 2) {
                        ImageUtil.yuv422ToYuv420sp(y, u, v, nv21, stride, previewSize.getHeight());
                    }
                    // 回传数据是YUV420
                    else if (y.length / u.length == 4) {
                        ImageUtil.yuv420ToYuv420sp(y, u, v, nv21, stride, previewSize.getHeight());
                    }
                    YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, stride, previewSize.getHeight(), null);
                    // ByteArrayOutputStream的close中其实没做任何操作，可不执行
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                    // 由于某些stride和previewWidth差距大的分辨率，[0,previewWidth)是有数据的，而[previewWidth,stride)补上的U、V均为0，因此在这种情况下运行会看到明显的绿边
//                    yuvImage.compressToJpeg(new Rect(0, 0, stride, previewSize.getHeight()), 100, byteArrayOutputStream);

                    // 由于U和V一般都有缺损，因此若使用方式，可能会有个宽度为1像素的绿边
                    yuvImage.compressToJpeg(new Rect(0, 0, previewSize.getWidth(), previewSize.getHeight()), 100, byteArrayOutputStream);

                    // 为了删除绿边，抛弃一行像素
//                    yuvImage.compressToJpeg(new Rect(0, 0, previewSize.getWidth() - 1, previewSize.getHeight()), 100, byteArrayOutputStream);

                    byte[] jpgBytes = byteArrayOutputStream.toByteArray();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    // 原始预览数据生成的bitmap
                    final Bitmap originalBitmap = BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.length, options);
                    Matrix matrix = new Matrix();
                    // 预览相对于原数据可能有旋转
                    matrix.postRotate(Camera2Helper.CAMERA_ID_BACK.equals(openedCameraId) ? displayOrientation : -displayOrientation);

                    // 对于前置数据，镜像处理；若手动设置镜像预览，则镜像处理；若都有，则不需要镜像处理
                    if (Camera2Helper.CAMERA_ID_FRONT.equals(openedCameraId) ^ isMirrorPreview) {
                        matrix.postScale(-1, 1);
                    }
                    // 和预览画面相同的bitmap
                    final Bitmap previewBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false);

                    detectAndDraw(previewBitmap);

                    endTime = System.currentTimeMillis();
                    long dur = endTime - startTime;
                    float fps = (float) (1000.0 / dur);
                    total_fps = (total_fps == 0) ? fps : (total_fps + fps);
                    fps_count++;
                    String modelName = "YOLOv4-tiny-person";
                }
            });
        }
    }

    protected void detectAndDraw(Bitmap image) {
        Box[] result = NativeHelper.getInstance().detectForNcnn(image);
        drawBoxRects(result);
    }

    protected void drawBoxRects(Box[] results) {
        if (results == null || results.length <= 0) {
            return;
        }
        final Bitmap croppedBitmap = Bitmap.createBitmap((int) tvCameraTexturePreview.getWidth(), (int) tvCameraTexturePreview.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(croppedBitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4 * tvCameraTexturePreview.getWidth() / 800.0f);
        boxPaint.setTextSize(30 * tvCameraTexturePreview.getWidth() / 800.0f);
        for (Box box : results) {
            Log.i("Box box", "detectAndDraw: x: " + (box.x0 + 3) + " y: " + (box.y0 + 30 * tvCameraTexturePreview.getWidth() / 1000.0f));
            boxPaint.setColor(box.getColor());
            boxPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(box.getLabel() + String.format(Locale.CHINESE, " %.3f", box.getScore()), box.x0 + 3, box.y0 + 30 * tvCameraTexturePreview.getWidth() / 1000.0f, boxPaint);
            boxPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(box.getRect(), boxPaint);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ivImageViewForFrame.setImageBitmap(croppedBitmap);

                endTime = System.currentTimeMillis();
                long dur = endTime - startTime;
                float fps = (float) (1000.0 / dur);
                total_fps = (total_fps == 0) ? fps : (total_fps + fps);
                fps_count++;
                String modelName = "YOLOv4-tiny-person";
                Log.i("dongfpss", "run: " + fps);
//                tvInfoCamera.setText(String.format(Locale.CHINESE,
//                        "%s\nSize: %dx%d\nTime: %.3f s\nFPS: %.3f\nAVG_FPS: %.3f",
//                        modelName, tvCameraTexturePreview.getHeight(), tvCameraTexturePreview.getWidth() , dur / 1000.0, fps, (float) total_fps / fps_count));
            }
        });

//        ivImageViewForFrame.post(new Runnable() {
//            @Override
//            public void run() {
//                ivImageViewForFrame.setImageBitmap(croppedBitmap);
//            }
//        });
    }

    @Override
    public void onCameraClosed() {
        Log.i(TAG, "onCameraClosed: ");
    }

    @Override
    public void onCameraError(Exception e) {
        e.printStackTrace();
    }

    @Override
    protected void onDestroy() {
        if (imageProcessExecutor != null) {
            imageProcessExecutor.shutdown();
            imageProcessExecutor = null;
        }
        if (camera2Helper != null) {
            camera2Helper.release();
        }
        super.onDestroy();
    }

    public void switchCamera(View view) {
        if (camera2Helper != null) {
            camera2Helper.switchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                initCamera();
            } else {
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 用于监听布局之类的变化，比如某个空间消失了
     */
    @Override
    public void onGlobalLayout() {
        tvCameraTexturePreview.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            initCamera();
        }
    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }
}