package com.dji.FPVDemo.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WriteFileUtil {
    public static void putStringToExternalStorage(String content, File parent, String fileName, boolean append){

        FileOutputStream fos = null;

        File file = makeFile(parent,fileName);
        try {
            fos = new FileOutputStream(file,append);
            fos.write(content.getBytes());
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            closeOutputStream(fos);
        }
    }

    public static File makeFile(File base, String fileName){
//        if (StringTools.isEmpty(fileName)) throw new NullPointerException("fileName cant be null");
        if(fileName.indexOf(File.separator) < 0){
            return new File(base,fileName);
        }
        throw new IllegalArgumentException(
                "File " + fileName + " contains a path separator");
    }

    public static void closeOutputStream(OutputStream... os){

        for (int i=0; i<os.length; i++){
            if (os[i] != null) {
                try {
                    os[i].close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
