// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.example.ncnnlibrary;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

/**
 * NCNN识别module
 * 
 * @author dongsiyuan
 * @since 2020/11/23 16:15
 */
public class NcnnJni {

    static {
        System.loadLibrary("ncnn_jni");
    }

    final static String TAG = "NcnnJni";

    private static NcnnJni mInstance;

    private NcnnJni() {
    }

    public static NcnnJni getInstance() {
        if (mInstance == null) {
            synchronized (NcnnJni.class) {
                if (mInstance == null) {
                    mInstance = new NcnnJni();
                }
            }
        }
        return mInstance;
    }

    public void initNcnn(AssetManager manager, boolean v4tiny, boolean useGPU) {
        nativeInitNcnn(manager, v4tiny, useGPU);
    }

    public Box[] detectForNcnn(Bitmap bitmap) {
        return nativeDetectForNcnn(bitmap);
    }

    /**
     *
     * @param param
     * @param bin
     * @return
     */
    public boolean initNcnn4Example(byte[] param, byte[] bin) {
        return init(param, bin);
    }

    public float[] detect4Example(Bitmap bitmap) {
        return detect(bitmap);
    }

    private static native void nativeInitNcnn(AssetManager manager, boolean v4tiny, boolean useGPU);

    private static native Box[] nativeDetectForNcnn(Bitmap bitmap);

    private native boolean init(byte[] param, byte[] bin);

    private native float[] detect(Bitmap bitmap);

}
