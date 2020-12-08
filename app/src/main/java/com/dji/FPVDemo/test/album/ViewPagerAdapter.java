package com.dji.FPVDemo.test.album;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;

import com.dji.FPVDemo.R;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.media.MediaFile;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ViewPagerAdapter extends PagerAdapter {

    private Context mContext;

    private List<MediaFile> mImgList = new ArrayList<>();
    private List<Bitmap> mBitmapList = new ArrayList<>();

    private OnItemClickListener mListener;
    private OnPlayClickListener onPlayClickListener;

    public ViewPagerAdapter(Context context, List<Bitmap> mBitmapList, List<MediaFile> mImgList) {
        this.mContext = context;
        this.mImgList = mImgList;
        this.mBitmapList = mBitmapList;

    }

    @Override
    public int getCount() {
        return mImgList == null ? 0 : mImgList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object instanceof PhotoView) {
            PhotoView view = (PhotoView) object;
            view.setImageDrawable(null);
            container.removeView((View) view);
        }
    }

    @Override
    public int getItemPosition(Object object) {
        // 最简单解决 notifyDataSetChanged() 页面不刷新问题的方法
        PhotoView view = (PhotoView) object;

        int currentPage = PhotoPreActivity.deletePosition;

        if (currentPage == (Integer) view.getTag()) {
            return POSITION_NONE;
        } else {
            return POSITION_UNCHANGED;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public Object instantiateItem(ViewGroup container, final int position) {

        final View imageLayout = LayoutInflater.from(mContext).inflate(R.layout.item_image_pager, null);
        assert imageLayout != null;

        PhotoView photoView = (PhotoView) imageLayout.findViewById(R.id.photoview);
        final ImageView imgPlay = (ImageView) imageLayout.findViewById(R.id.img_play);

        if (mImgList.get(position).getMediaType() == MediaFile.MediaType.MOV
                || (mImgList.get(position).getMediaType() == MediaFile.MediaType.MP4)) {
            imgPlay.setVisibility(View.VISIBLE);
        }

        imgPlay.setImageResource(R.drawable.ic_play);

        imgPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onPlayClickListener != null) {
                    onPlayClickListener.onPlayClick(position);
                }
            }
        });

        photoView.setImageBitmap(mBitmapList.get(position));

        photoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(position, v);

                }
            }
        });

        photoView.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {
                if (mListener != null) {
                    mListener.onItemClick(position, view);
                }
            }
        });

        container.addView(imageLayout);

        return imageLayout;
    }


    public void setOnItemClickListener(ViewPagerAdapter.OnItemClickListener l) {
        mListener = l;
    }

    public interface OnItemClickListener {
        void onItemClick(int position, View view);
    }

    public void setOnPlayClickListener(OnPlayClickListener onPlayClickListener) {
        this.onPlayClickListener = onPlayClickListener;
    }

    public interface OnPlayClickListener {
        void onPlayClick(int position);
    }
}
