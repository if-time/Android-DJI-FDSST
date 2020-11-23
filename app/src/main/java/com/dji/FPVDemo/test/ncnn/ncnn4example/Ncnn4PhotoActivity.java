package com.dji.FPVDemo.test.ncnn.ncnn4example;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dji.FPVDemo.R;
import com.example.ncnnlibrary.NcnnJni;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 使用照片测试ncnn
 *
 * @author dongsiyuan
 * @since 2020/11/23 11:25
 */
public class Ncnn4PhotoActivity extends Activity {

    private static final String TAG = Ncnn4PhotoActivity.class.getName();
    private static final int USE_PHOTO = 1001;
    private String camera_image_path;
    private ImageView show_image;
    private TextView result_text;
    private boolean load_result = false;
    private int[] ddims = {1, 3, 224, 224};
    private int model_index = 1;
    private List<String> resultLabel = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ncnn4_photo);

        try {
            initSqueezeNcnn();
        } catch (IOException e) {
            Log.e("MainActivity", "initSqueezeNcnn error");
        }

        init_view();
        readCacheLabelFromLocalFile();
    }

    private void initSqueezeNcnn() throws IOException {
        byte[] param = null;
        byte[] bin = null;

        {
            InputStream assetsInputStream = getAssets().open("mobilenet_v2.param.bin");
            int available = assetsInputStream.available();
            param = new byte[available];
            int byteCode = assetsInputStream.read(param);
            assetsInputStream.close();
        }
        {
            InputStream assetsInputStream = getAssets().open("mobilenet_v2.bin");
            int available = assetsInputStream.available();
            bin = new byte[available];
            int byteCode = assetsInputStream.read(bin);
            assetsInputStream.close();
        }

        load_result = NcnnJni.getInstance().initNcnn4Example(param, bin);
        Log.d("load_model", "result:" + load_result);
    }

    // initialize view
    private void init_view() {
        request_permissions();
        show_image = (ImageView) findViewById(R.id.ivShowImage);
        result_text = (TextView) findViewById(R.id.tvResultText);
        result_text.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button use_photo = (Button) findViewById(R.id.btnUsePhoto);
//        Button start_photo = (Button) findViewById(R.id.start_camera);


        // use photo click
        use_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!load_result) {
                    Toast.makeText(Ncnn4PhotoActivity.this, "never load model", Toast.LENGTH_SHORT).show();
                    return;
                }
                PhotoUtil.use_photo(Ncnn4PhotoActivity.this, USE_PHOTO);
            }
        });

        // start camera click
//        start_photo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (!load_result) {
//                    Toast.makeText(MainActivity.this, "never load model", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                camera_image_path = PhotoUtil.start_camera(MainActivity.this, START_CAMERA);
//            }
//        });
    }


    private void readCacheLabelFromLocalFile() {
        try {
            AssetManager assetManager = getApplicationContext().getAssets();
            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open("synset.txt")));
            String readLine = null;
            while ((readLine = reader.readLine()) != null) {
                resultLabel.add(readLine);
            }
            reader.close();
        } catch (Exception e) {
            Log.e("labelCache", "error " + e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        String image_path;
        RequestOptions options = new RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case USE_PHOTO:
                    if (data == null) {
                        Log.w(TAG, "user photo data is null");
                        return;
                    }
                    Uri image_uri = data.getData();
                    Glide.with(Ncnn4PhotoActivity.this).load(image_uri).apply(options).into(show_image);
                    // get image path from uri
                    image_path = PhotoUtil.get_path_from_URI(Ncnn4PhotoActivity.this, image_uri);
                    // predict image
                    predict_image(image_path);
                    break;
//                case START_CAMERA:
//                    // show photo
//                    Glide.with(MainActivity.this).load(camera_image_path).apply(options).into(show_image);
//                    // predict image
//                    predict_image(camera_image_path);
//                    break;
            }
        }
    }

    //  predict image
    private void predict_image(String image_path) {
        // picture to float array
        Bitmap bmp = PhotoUtil.getScaleBitmap(image_path);
//        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.zheng);
        if (bmp != null) {
            Bitmap rgba = bmp.copy(Bitmap.Config.ARGB_8888, true);

            // resize to 227x227
            Bitmap input_bmp = Bitmap.createScaledBitmap(rgba, ddims[2], ddims[3], false);
            try {
                // Data format conversion takes too long
                // Log.d("inputData", Arrays.toString(inputData));
                long start = System.currentTimeMillis();
                // get predict result
                float[] result = NcnnJni.getInstance().detect4Example(input_bmp);
                long end = System.currentTimeMillis();
                Log.d(TAG, "origin predict result:" + Arrays.toString(result));
                long time = end - start;
                Log.d("result length", String.valueOf(result.length));
                // show predict result and time
                int r = get_max_result(result);
                String show_text = "result：" + r + "\nname：" + resultLabel.get(r) + "\nprobability：" + result[r] + "\ntime：" + time + "ms";
                result_text.setText(show_text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(Ncnn4PhotoActivity.this, "No No", Toast.LENGTH_SHORT).show();

            Bitmap bitMBitmap;

            File file = new File(image_path);
            if (file.exists()) {
                bitMBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                Log.i("load_model", "predict_image: " + bitMBitmap.getHeight());
            } else {
                Log.i("load_model", "! file.exists()");
            }

        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 400;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }


    // get max probability label
    private int get_max_result(float[] result) {
        float probability = result[0];
        int r = 0;
        for (int i = 0; i < result.length; i++) {
            if (probability < result[i]) {
                probability = result[i];
                r = i;
            }
        }
        return r;
    }

    // request permissions
    private void request_permissions() {

        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        // if list is not empty will request permissions
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {

                        int grantResult = grantResults[i];
                        if (grantResult == PackageManager.PERMISSION_DENIED) {
                            String s = permissions[i];
                            Toast.makeText(this, s + " permission was denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }
}