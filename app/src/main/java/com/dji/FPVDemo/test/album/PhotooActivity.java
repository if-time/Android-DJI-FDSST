package com.dji.FPVDemo.test.album;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.dji.FPVDemo.R;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class PhotooActivity extends AppCompatActivity {

    /**
     * 显示图片的GridView
     */
    private GridView gvPhoto;
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
     * 显示图片的适配器
     */
    private Photodaapter adapter;
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
        setContentView(R.layout.activity_photoo);

        gvPhoto = (GridView) findViewById(R.id.gv_photo);
        initData();

        files = getImages(Environment.getExternalStorageDirectory().toString() + "/result/");
    }

    private void initData() {
        listpath = new ArrayList<>();
        folder = new File(Environment.getExternalStorageDirectory().toString() + "/result/");
        /**将文件夹下所有文件名存入数组*/
        allFiles = folder.list();
        /**遍历数组*/
        for (int i = 0; i < allFiles.length; i++) {
            scanpath = folder + "/" + allFiles[i];
            /**将文件转为bitmap如果为空则不是图片文件*/
            Bitmap bitmap = BitmapFactory.decodeFile(scanpath);
            if (bitmap != null) {
                listpath.add(bitmap);
            }
        }

        /** 图片写入适配器*/
        adapter = new Photodaapter(listpath, this);
        gvPhoto.setAdapter(adapter);
    }

    private File[] getImages(String folderPath) {
        File folder = new File(folderPath);
        if (folder.isDirectory()) {
            File[] fs = folder.listFiles();
            return fs;
        }
        return null;
    }

    private FileFilter imageFilter = new FileFilter() {

        @Override
        public boolean accept(File file) {
            String name = file.getName();
            return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
        }
    };

    public class Photodaapter extends BaseAdapter {
        private List<Bitmap> mlist;
        private Context mcontext;
        private LayoutInflater minflater;

        public Photodaapter(List<Bitmap> list, Context context) {
            super();
            this.mlist = list;
            this.mcontext = context;
            this.minflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mlist.size();
        }

        @Override
        public Object getItem(int position) {
            return mlist.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            VIewHolder vh;
            if (convertView == null) {
                vh = new VIewHolder();
                convertView = minflater.inflate(R.layout.lv_iv_item, null);
                vh.iv = (ImageView) convertView.findViewById(R.id.iv_item);
                convertView.setTag(vh);
            } else {
                vh = (VIewHolder) convertView.getTag();
            }
//            vh.iv.setImageBitmap(mlist.get(position));
            Log.i("files", "getView: " + files[position].getAbsolutePath());
            Glide.with(PhotooActivity.this).load(files[position]).into(vh.iv);
            return convertView;
        }
    }

    class VIewHolder {
        ImageView iv;
    }
}
