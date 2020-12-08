package com.dji.FPVDemo.test.album.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.dji.FPVDemo.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecyclerViewPictureActivity extends AppCompatActivity {

    private RecyclerView rvImg;
    private TextView tvImgMsg;

    String TAG = "shengmingzq";


    /**
     * 文件夹下所有图片的bitmap
     */
    private List<Bitmap> listpath;

    private File[] files;
    /**
     * 文件夹下图片的真实路径
     */
    private String scanpath;
    /**
     * 所有图片的名字
     */
    public String[] allFiles;
    /**
     * 想要查找的文件夹
     */
    private File folder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_picture);

        tvImgMsg = findViewById(R.id.tvImgMsg);
        rvImg = findViewById(R.id.rvImg);
        loadUI();

        Log.i("shengmingzq", "onCreate: RecyclerViewPictureActivity");
        initData();
    }

    private void initData() {
        List<ImgFile> list = new ArrayList<>();
        ImgFile img = new ImgFile();

        folder = new File(Environment.getExternalStorageDirectory().toString() + "/result/");
        /**将文件夹下所有文件名存入数组*/
        allFiles = folder.list();
        /**遍历数组*/
        for (int i = 0; i < allFiles.length; i++) {
            scanpath = folder + "/" + allFiles[i];
            /**将文件转为bitmap如果为空则不是图片文件*/
            Bitmap bitmap = BitmapFactory.decodeFile(scanpath);
            if (bitmap != null) {
                img.setFileName("第一张");
                img.setFileSrc(bitmap);
                img.setFilePath(scanpath);
                list.add(img);
            }
        }

        /** 图片写入适配器*/
        if (list != null && list.size() > 0) {
            GridLayoutManager layoutManager = new GridLayoutManager(RecyclerViewPictureActivity.this, 2);
            rvImg.setLayoutManager(layoutManager);
            imgAdapter adapter = new imgAdapter(list);
            rvImg.setAdapter(adapter);
        } else {
            tvImgMsg.setVisibility(View.VISIBLE);
        }
    }

    public void loadUI() {
//        List<ImgFile> list = new ArrayList<>();
//        ImgFile img = new ImgFile();
//        img.setFileName("第一张");
//        img.setFileSrc("R.drawable.ic_album_play");
//        list.add(img);
//        img = new ImgFile();
//        img.setFileName("第二张");
//        img.setFileSrc("R.drawable.ic_album_play");
//        list.add(img);
//        img = new ImgFile();
//        img.setFileName("第三张");
//        img.setFileSrc("R.drawable.ic_album_play");
//        list.add(img);
//        if (list != null && list.size() > 0) {
//            GridLayoutManager layoutManager = new GridLayoutManager(RecyclerViewPictureActivity.this, 4);
//            rvImg.setLayoutManager(layoutManager);
//            imgAdapter adapter = new imgAdapter(list);
//            rvImg.setAdapter(adapter);
//        } else {
//            tvImgMsg.setVisibility(View.VISIBLE);
//        }
    }

    class imgAdapter extends RecyclerView.Adapter<imgAdapter.ViewHolder> {
        private List<ImgFile> list;

        public imgAdapter(List<ImgFile> list) {
            this.list = list;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View myView;
            TextView tvName;
            ImageView ivImg;

            public ViewHolder(View itemView) {
                super(itemView);
                myView = itemView;
                tvName = (TextView) itemView.findViewById(R.id.tvName);
                ivImg = (ImageView) itemView.findViewById(R.id.ivImg);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(RecyclerViewPictureActivity.this).inflate(R.layout.rv_item_image, parent, false);
            return new ViewHolder(v);
        }

        @SuppressLint("NewApi")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImgFile img = list.get(position);
//            holder.ivImg.setImageResource(R.drawable.ic_album_play);

//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            Bitmap bitmap = list.get(position).getFileSrc();
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
//            byte[] bytes=baos.toByteArray();

            Glide.with(RecyclerViewPictureActivity.this)
                    .load(new File(list.get(position).getFilePath()))
                    .asBitmap()
                    .priority(Priority.NORMAL)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_placeholder)
                    .centerCrop().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(RecyclerViewPictureActivity.this.getResources(), resource);
                    circularBitmapDrawable.setCornerRadius(5f);
                    holder.ivImg.setImageDrawable(circularBitmapDrawable);
                }
            });

//            Glide.with(RecyclerViewPictureActivity.this)
//                    .load(new File(list.get(position).getFilePath()))
//                    .into(holder.ivImg);
            holder.tvName.setText(img.getFileName());
            holder.myView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.getContext().startActivity(
                            new Intent(v.getContext(), PreviewPhotoActivity.class),
                            // 注意这里的sharedView
                            // Content，View（动画作用view），String（和XML一样）
                            ActivityOptions.makeSceneTransitionAnimation((Activity) v.getContext(),
                                    v, "sharedView").toBundle()
                    );
                }
            });
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }
    }

    private String getPath(Context context, Uri uri) {
        String path = null;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return null;
        }
        if (cursor.moveToFirst()) {
            try {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        return path;
    }


    class ImgFile {
        private String fileName;
        private Bitmap fileSrc;
        private String filePath;

        public ImgFile() {
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Bitmap getFileSrc() {
            return fileSrc;
        }

        public void setFileSrc(Bitmap fileSrc) {
            this.fileSrc = fileSrc;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("shengmingzq", "onStart: RecyclerViewPictureActivity");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("shengmingzq", "onPause: RecyclerViewPictureActivity");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: RecyclerViewPictureActivity");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: RecyclerViewPictureActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: RecyclerViewPictureActivity");
    }
}
