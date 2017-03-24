package com.tlong.gt.template.module.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.AndroidRuntimeException;
import android.util.SparseIntArray;
import android.view.TextureView;

import com.tlong.gt.template.util.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 使用Android 5.0 过期的API
 * Created by 高腾 on 2017/3/22.
 */
public class CameraDep {

    private static final String TAG = CameraDep.class.getSimpleName();

    public static final int FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    public static final int FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private SparseIntArray mFacingIds;
    private int mCameraId;
    private Camera mCamera;
    private Camera.Parameters mParams;
    private int mDisplayRotation;
    private Point mDisplaySize;
    private Camera.Size mPreviewSize;
    private boolean isPreview;
    private int mPreviewViewWidth;
    private int mPreviewViewHeight;

    public CameraDep() {
        mFacingIds = new SparseIntArray();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            mFacingIds.append(info.facing, i);
        }
    }

    private int checkFacing(int facing) {
        int id = mFacingIds.get(facing, -1);
        if (id == -1) {
            throw new AndroidRuntimeException("facing{" + facing + "} error");
        }
        return id;
    }

    public void open(int facing) {
        release();
        mCameraId = checkFacing(facing);
        mCamera = Camera.open(mCameraId);
        mParams = mCamera.getParameters();
    }

    public void release() {
        if (mCamera != null) {
            stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void startPreview() {
        if (mCamera == null || mPreviewSize == null || isPreview) {
            return;
        }
        mCamera.startPreview();
        isPreview = true;
    }

    public void stopPreview() {
        if (mCamera != null && isPreview) {
            mCamera.stopPreview();
            isPreview = false;
        }
    }

    private void updateParams() {
        mCamera.setParameters(mParams);
        mParams = mCamera.getParameters();
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
        mCamera.setDisplayOrientation(CameraUtil.getDegrees(mDisplayRotation));
        SurfaceTexture texture = view.getSurfaceTexture();
        updatePreviewSize(texture, view.getWidth(), view.getHeight());
    }

    public void updatePreviewSize(@NonNull SurfaceTexture texture, int width, int height) {
        if (mCamera == null || mDisplaySize == null || width == 0 || height == 0) {
            return;
        }
        mPreviewViewWidth = width;
        mPreviewViewHeight = height;
        try {
            mPreviewSize = CameraUtil.getPreviewSize(width, height, mDisplaySize.x, mDisplaySize.y,
                    mDisplayRotation, getSensorOrientation(), mParams.getSupportedPreviewSizes());
            texture.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height);
            mParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            updateParams();
            mCamera.setPreviewTexture(texture);
            LogUtil.e(TAG, "previewSize{w : %d, h : %d} ", mPreviewSize.width, mPreviewSize.height);
            updatePictureSize();
        } catch (IOException e) {
            mPreviewSize = null;
            e.printStackTrace();
        }
    }

    public int getPreviewWidth() {
        if (mPreviewSize != null) return mPreviewSize.width;
        return mParams == null ? 0 : mParams.getPreviewSize().width;
    }

    public int getPreviewHeight() {
        if (mPreviewSize != null) return mPreviewSize.height;
        return mParams == null ? 0 : mParams.getPreviewSize().height;
    }

    public int getPreviewFormat() {
        return mParams == null ? 0 : mParams.getPreviewFormat();
    }

    /** 获取摄像头传感器方向. */
    private int getSensorOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        return info.orientation;
    }

    /** 判断并开启自动对焦模式. */
    private boolean canAutoFocus() {
        String focusMode = mParams.getFocusMode();
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(focusMode)) {
            return true;
        }
        List<String> focusModes = mParams.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(mParams);
            mParams = mCamera.getParameters();
            return true;
        }
        return false;
    }

    /**
     * 定点对焦
     * @param x X坐标
     * @param y Y坐标
     * @param focusAreaSize 对焦区域大小
     */
    public void pointFocus(float x, float y, int focusAreaSize) {
        if (mCamera == null || mPreviewViewWidth == 0 || mPreviewViewHeight == 0 || !canAutoFocus()) {
            return;
        }
        int viewWidth = mPreviewViewWidth;
        int viewHeight = mPreviewViewHeight;
        /*
         * 相机坐标系:横屏长边为x轴，x轴和y轴数值固定是-1000 ~ 1000
         * 定点对焦需要将屏幕坐标系上的点转为相机坐标系的点
         */
        int left;
        int top;
        switch (CameraUtil.getDegrees(mDisplayRotation)) {
            // 横屏，两坐标系x、y轴重合
            case 0:
            case 180:
                left = CameraUtil.clamp((int) (x / viewWidth * 2000 - 1000), -1000, 1000 - focusAreaSize);
                top = CameraUtil.clamp((int) (y / viewHeight * 2000 - 1000), -1000, 1000 - focusAreaSize);
                break;
            // 竖屏，屏幕x轴是相机y轴，屏幕y轴是相机x轴
            case 90:
            case 270:
                left = CameraUtil.clamp((int) (y / viewHeight * 2000 - 1000), -1000, 1000 - focusAreaSize);
                top = CameraUtil.clamp((int) (x / viewWidth * 2000 - 1000), -1000, 1000 - focusAreaSize);
                break;
            default:
                left = 0;
                top = 0;
                break;
        }
        Rect area = new Rect(left, top, left + focusAreaSize, top + focusAreaSize);
        List<Camera.Area> focusAreas = new ArrayList<>();
        focusAreas.add(new Camera.Area(area, 1000)); // 参数是对焦区域和权重
        mParams.setFocusAreas(focusAreas);
        updateParams();
        mCamera.autoFocus(null);
    }

    public void takePicture(String path) {
        LogUtil.e(TAG, "path=%s", path);
        mPicturePath = path;
        mCamera.takePicture(null, null, mPictureCallback);
//        mCamera.setOneShotPreviewCallback(mPreviewCallback);
    }

    private void updatePictureSize() {
        if (mCamera == null || mPreviewSize == null) {
            return;
        }
        List<Camera.Size> sizes = mParams.getSupportedPictureSizes();
        mPictureSize = CameraUtil.chooseLargeSize(sizes, mPreviewSize.width, mPreviewSize.height, 4096, 4096);
        mParams.setPictureSize(mPictureSize.width, mPictureSize.height);
        updateParams();
        LogUtil.e(TAG, "pictureSize{w:%d, h:%d} ", mPictureSize.width, mPictureSize.height);
    }

    private String mPicturePath;
    private Camera.Size mPictureSize;

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            savePicture(data);
        }
    };

    private void savePicture(byte[] data) {
        File pictureFile = new File(mPicturePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(pictureFile);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.setRotate(CameraUtil.getPictureDegrees(mDisplayRotation, getSensorOrientation()));
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, mPictureSize.width, mPictureSize.height, matrix, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//            fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            File pictureFile = new File(mPicturePath);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(pictureFile);
                int format = mParams.getPreviewFormat();
                LogUtil.e(TAG, "previewFormat:" + format);
                YuvImage yuvImage = new YuvImage(data, format, mPreviewSize.width, mPictureSize.height, null);
                yuvImage.compressToJpeg(new Rect(0, 0, mPreviewSize.width, mPictureSize.height), 100, fos);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
}
