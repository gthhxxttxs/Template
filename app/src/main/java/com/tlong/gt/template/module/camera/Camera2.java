package com.tlong.gt.template.module.camera;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.view.TextureView;

import com.tlong.gt.template.util.LogUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * {@link android.hardware.camera2} API
 * Created by 高腾 on 2017/3/24.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2 {

    public static final int FACING_BACK = CameraCharacteristics.LENS_FACING_BACK;
    public static final int FACING_FRONT = CameraCharacteristics.LENS_FACING_FRONT;
    @IntDef({FACING_BACK, FACING_FRONT})
    @interface FacingType {}

    private Context mContext;
    private CameraManager mManager;
    private SparseArray<String> mFacingIds;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Semaphore mCameraLock = new Semaphore(1);
    private CameraCharacteristics mCharacteristics;
    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    private CameraDevice.StateCallback mOpenCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraLock.release();
            mCamera = camera;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraLock.release();
            camera.close();
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraLock.release();
            camera.close();
            mCamera = null;
        }
    };

    private int mDisplayRotation;
    private Point mDisplaySize;
    private Size mPreviewSize;
    private boolean isPreview;

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

    private String checkFacing(@FacingType int facing) {
        String id = mFacingIds.get(facing);
        if (id == null) {
            throw new CameraException(CameraException.TYPE_LOG, "facing{%d} error", facing);
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

    public void open(@FacingType int facing) {
        try {
            String id = checkFacing(facing);
            mCharacteristics = mManager.getCameraCharacteristics(id);
            startBackgroundThread();
            if (checkPermission()) {
                if (!mCameraLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                mManager.openCamera(id, mOpenCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void release() {
        if (mSession != null) {
            mSession.close();
            mSession = null;
        }
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        stopBackgroundThread();
        mManager = null;
    }

    private void createSession() {
        if (mCamera != null) {
            try {
                mCamera.createCaptureSession(Arrays.asList(new Surface(null)), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        mSession = session;
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        session.close();
                        mSession = null;
                    }
                }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDisplayInfo(@NonNull Activity activity) {
        mDisplayRotation = CameraUtil.getDisplayRotation(activity);
        mDisplaySize = CameraUtil.getDisplaySize(activity);
    }

    public void setupPreviewView(@NonNull TextureView view) {
        if (mCamera == null || !view.isAvailable()) {
            return;
        }
        setDisplayInfo((Activity) view.getContext());
        SurfaceTexture texture = view.getSurfaceTexture();
        updatePreviewSize(texture, view.getWidth(), view.getHeight());
    }

    public void updatePreviewSize(@NonNull SurfaceTexture texture, int width, int height) {
        if (mCamera == null || mDisplaySize == null || width == 0 || height == 0) {
            return;
        }
        StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        assert map != null;
        Size[] choices = map.getOutputSizes(SurfaceTexture.class);
        mPreviewSize = CameraUtil.getPreviewSize(width, height, mDisplaySize.x, mDisplaySize.y,
                mDisplayRotation, getSensorOrientation(), Arrays.asList(choices));
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

    }

    private int getSensorOrientation() {
        if (mCharacteristics == null) {
            return 0;
        }
        Integer sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return sensorOrientation == null ? 0 : sensorOrientation;
    }


}
