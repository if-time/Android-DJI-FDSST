package com.dji.FPVDemo.test.album;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewConfigurationCompat;
import androidx.viewpager.widget.ViewPager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.FPVDemo.DJIApplication;
import com.dji.FPVDemo.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;

import static android.animation.ObjectAnimator.ofFloat;

public class PhotoPreActivity extends AppCompatActivity implements Handler.Callback {

    private Handler handler;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private static final String TAG = PhotoPreActivity.class.getName();

    ViewPager bigImgVp;

    private TextView tvIndicator;
    //状态栏
    private RelativeLayout rlTopBar;

    private int position;
    public static int deletePosition;

    private boolean isShowBar = true;

    private ViewPagerAdapter mViewPagerAdapter;

    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager mMediaManager;
    private ArrayList<Bitmap> prePhoto = new ArrayList<>();

    int mTouchSlop;

    File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Gallery/");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_pre);

        handler = new Handler(Looper.getMainLooper(), this);

        Intent intent = getIntent();
        position = intent.getIntExtra("position", 0);
        //        mediaFileList = intent.getParcelableExtra("mediaFile");
//        mediaFileList = MediaBrowserActivity.mediaFileList;
//        //prePhoto = intent.getParcelableArrayListExtra("Bitmap");
//        prePhoto = MediaBrowserActivity.prePhoto;
        Log.i("prePhoto", "prePhoto : " + prePhoto.size());

        initView();
        initListener();
        initViewPager();

        ViewConfiguration configuration = ViewConfiguration.get(this);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        System.out.print(mTouchSlop);

        tvIndicator.setText(1 + "/" + mediaFileList.size());

        bigImgVp.setCurrentItem(position);
    }

    private void initView() {
        mMediaManager = DJIApplication.getCameraInstance().getMediaManager();

        tvIndicator = (TextView) findViewById(R.id.tv_indicator);
        rlTopBar = (RelativeLayout) findViewById(R.id.rl_top_bar);

        bigImgVp = (ShowImagesViewPager) findViewById(R.id.big_img_vp);
    }

    private void initListener() {
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void moveToPosition(int postion) {

        mMediaManager.moveToPosition(postion,
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (null != error) {
                            setResultToToast("Move to video position failed" + error.getDescription());
                        } else {
                            DJILog.e(TAG, "Move to video position successfully.");
                        }
                    }
                });
    }

    /**
     * 初始化ViewPager
     */
    private void initViewPager() {
//        mViewPagerAdapter = new ViewPagerAdapter(this, MediaBrowserActivity.prePhoto, mediaFileList);
        if ((mediaFileList.get(position).getMediaType() == MediaFile.MediaType.MOV) || (mediaFileList.get(position).getMediaType() == MediaFile.MediaType.MP4)) {
//            ivPlay.setVisibility(View.VISIBLE);

        } else {
//            rlSeeker.setVisibility(View.GONE);
        }
        //        bigImgVp.setAdapter(mImagePagerAdapter);
        bigImgVp.setAdapter(mViewPagerAdapter);
        bigImgVp.setOnTouchListener(new View.OnTouchListener() {
            int touchFlag = 0;
            float x = 0, y = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchFlag = 0;
                        x = event.getX();
                        y = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float xDiff = Math.abs(event.getX() - x);
                        float yDiff = Math.abs(event.getY() - y);
                        if (xDiff == 0 && yDiff == 0) {
                            touchFlag = 0;
                        } else {
                            touchFlag = -1;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (touchFlag == 0) {
                            Toast.makeText(PhotoPreActivity.this, "click_setOnTouchListener", Toast.LENGTH_LONG).show();
                            if (isShowBar) {
                                hideBar();
                            } else {
                                showBar();
                            }
                        } else if (touchFlag == -1) {
                            if (isShowBar) {
                                hideBar();
                            }
                        }
                        break;
                }
                return false;
            }
        });

        mViewPagerAdapter.setOnItemClickListener(new ViewPagerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                if (isShowBar) {
                    hideBar();
                } else {
                    showBar();
                }
            }
        });

        mViewPagerAdapter.setOnPlayClickListener(new ViewPagerAdapter.OnPlayClickListener() {
            @Override
            public void onPlayClick(int position) {
                Intent intent = new Intent(PhotoPreActivity.this, PhotoPreActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

                intent.putExtra("position", position);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        bigImgVp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                tvIndicator.setText(position + 1 + "/" + mediaFileList.size());
                deletePosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /**
     * 显示和隐藏状态栏
     *
     * @param show
     */
    private void setStatusBarVisible(boolean show) {
        if (show) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    /**
     * 显示头部
     */
    private void showBar() {

        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;         // 屏幕宽度（像素）
        int height = dm.heightPixels;       // 屏幕高度（像素）

        isShowBar = true;
        setStatusBarVisible(true);
        //添加延时，保证StatusBar完全显示后再进行动画。
        rlTopBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (rlTopBar != null) {
                    ObjectAnimator animator = ofFloat(rlTopBar, "translationY",
                            rlTopBar.getTranslationY(), 0).setDuration(300);
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            if (rlTopBar != null) {
                                rlTopBar.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                    animator.start();
                }
            }
        }, 100);
    }

    /**
     * 隐藏头部
     */
    private void hideBar() {
        isShowBar = false;
        ObjectAnimator animator = ObjectAnimator.ofFloat(rlTopBar, "translationY",
                0, -rlTopBar.getHeight()).setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (rlTopBar != null) {
                    rlTopBar.setVisibility(View.GONE);
                    //添加延时，保证rlTopBar完全隐藏后再隐藏StatusBar。
                    rlTopBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setStatusBarVisible(false);
                        }
                    }, 5);
                }
            }
        });
        animator.start();

    }

    private void setResultToToast(final String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PhotoPreActivity.this, result, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean handleMessage(Message msg) {
        String toastMsg = (String) msg.obj;
        showToast(toastMsg);
        return false;
    }
}