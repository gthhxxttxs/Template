package com.tlong.gt.template.module.camera;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;

import com.tlong.gt.template.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by v_gaoteng on 2017/3/21.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CustomCameraImpl2 extends CustomCamera {

    private Context mContext;
    private CameraManager mManager;
    private SparseArray<String> mFacingIds;
    private String mCameraId;
    private CameraCharacteristics mCharacteristics;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private CameraOpenCallback mOpenCallback;
    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    private List<Surface> mSurfaces = new ArrayList<>();
    private Size mPreviewSize;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession.CaptureCallback mRequestCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            Integer afState = partialResult.get(CaptureResult.CONTROL_AF_STATE);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {

        }
    };

    public CustomCameraImpl2(Builder builder) {
        super(builder);
        initFacingIds(builder.getActivity());
    }

    private void initFacingIds(Context context) {
        try {
            mContext = context;
            mManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
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

    private void createSession(final CameraOpenCallback openCallback) {
        if (mTextureView != null) {
            setupPreviewView(mTextureView);
        }
        try {
            mCamera.createCaptureSession(mSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mSession = session;
                    mOpenCallback = openCallback;
                    mOpenCallback.onOpened();
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

    @Override
    public void open(final CameraOpenCallback openCallback) {
        try {
            mCameraId = checkFacing(mFacing);
            mCharacteristics = mManager.getCameraCharacteristics(mCameraId);
            mSurfaces.clear();
            if (mCamera != null) {
                release();
            }
            startBackgroundThread();
            if (checkPermission()) {
                mManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        mCamera = camera;
                        createSession(openCallback);
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        release();
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        release();
                    }
                }, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
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
        if (mOpenCallback != null) {
            mOpenCallback.onClosed();
        }
    }

    @Override
    protected int getSensorOrientation() {
        Integer sensorOrientation = mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (sensorOrientation == null) {
            throw new NullPointerException("sensorOrientation is null");
        }
        return sensorOrientation;
    }

    @Override
    protected void updatePreviewView(@NonNull SurfaceTexture texture, int rotatedPreviewWidth, int rotatedPreviewHeight, int maxPreviewWidth, int maxPreviewHeight) {
        // 之前已经连接的断开重新连
        if (mSession != null) {
            mSession.close();
            mSession = null;
            createSession(mOpenCallback);
            return;
        }
        chooseOptimalSize(rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight);
        configureTransform();
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(texture);
        try {
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(previewSurface);
            mSurfaces.add(previewSurface);
        } catch (CameraAccessException e) {
            mPreviewSize = null;
            e.printStackTrace();
        }
    }

    private void configureTransform() {
        int viewWidth = mTextureView.getWidth();
        int viewHeight = mTextureView.getHeight();
        int previewWidth = mPreviewSize.getWidth();
        int previewHeight = mPreviewSize.getHeight();
        final Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewHeight, previewWidth);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == mDisplayRotation || Surface.ROTATION_270 == mDisplayRotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewHeight,
                    (float) viewWidth / previewWidth);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (mDisplayRotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == mDisplayRotation) {
            matrix.postRotate(180, centerX, centerY);
        }

        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 必须在主线程调用
                mTextureView.setTransform(matrix);
            }
        });
    }

    private void chooseOptimalSize(int viewWidth, int viewHeight, int maxWidth, int maxHeight) {
        StreamConfigurationMap map = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            return;
        }
        Size[] choices = map.getOutputSizes(SurfaceTexture.class);
        Size chooseSize = mPreviewSize;
        int diff = mPreviewSize == null ? computeDiff(0, 0, viewWidth, viewHeight)
                : computeDiff(mPreviewSize.getWidth(), mPreviewSize.getHeight(), viewWidth, viewHeight);
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight) {
                int tempDiff = computeDiff(option.getWidth(), option.getHeight(), viewWidth, viewHeight);
                if (tempDiff > diff) {
                    continue;
                }
                diff = tempDiff;
                chooseSize = option;
            }
        }
        if (chooseSize == null) {
            chooseSize = choices[0];
        }
        mPreviewSize = chooseSize;
    }

    @Override
    public void startPreview() {
        if (mCamera == null || mSession == null || mPreviewSize == null){
            return;
        }
        try {
            // 对焦模式 AF(Auto Focus) MF(Manual Focus)
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            setAutoFlash(mPreviewRequestBuilder);
            mSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mRequestCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopPreview() {
        if (mCamera == null || mSession == null){
            return;
        }
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mSession.capture(mPreviewRequestBuilder.build(), mRequestCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
