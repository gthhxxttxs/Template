package com.tlong.gt.template.module.camera;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;

/**
 * {@link android.hardware.camera2} API
 * Created by 高腾 on 2017/3/24.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2 {

    public static final int FACING_BACK = CameraCharacteristics.LENS_FACING_BACK;
    public static final int FACING_FRONT = CameraCharacteristics.LENS_FACING_FRONT;

    private Context mContext;
    private CameraManager mManager;
    private SparseArray<String> mFacingIds;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private CameraCharacteristics mCharacteristics;
    private CameraDevice.StateCallback mOpenCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    public Camera2(@NonNull Context context) {
        mContext = context;
        mManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        initFacingIds();
    }

    private void initFacingIds() {
        try {
            mFacingIds = new SparseArray<>();
            for (String id : mManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null) {
                    continue;
                }
                mFacingIds.put(facing, id);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private String checkFacing(int facing) {
        String id = mFacingIds.get(facing);
        if (id == null) {
            throw new CameraException(CameraException.TYPE_LOG, "facing{%s} is error", facing);
        }
        return id;
    }
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkPermission() {
        int i = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        return i == PackageManager.PERMISSION_GRANTED;
    }

    public void open(int facing) {
        try {
            String id = checkFacing(facing);
            startBackgroundThread();
            if (checkPermission()) {
                mManager.openCamera(id, mOpenCallback, mBackgroundHandler);
            }
            mCharacteristics = mManager.getCameraCharacteristics(id);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void release() {

        stopBackgroundThread();
        mManager = null;
    }
}
