package com.tlong.gt.template.module.camera;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;

import com.tlong.gt.template.util.LogUtil;
import com.tlong.gt.template.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Android5.0以下使用
 * Created by 高腾 on 2017/3/21.
 */

class CustomCameraImpl extends CustomCamera {

    private SparseIntArray mFacingIds;
    private int mCameraId;
    private CameraOpenCallback mOpenCallback;
    private Camera mCamera;
    private Camera.Parameters mParams;
    private Camera.Size mPreviewSize;

    CustomCameraImpl(Builder builder) {
        super(builder);
        initFacingIds();
    }

    private void initFacingIds() {
        mFacingIds = new SparseIntArray();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i ++) {
            Camera.getCameraInfo(i, info);
            mFacingIds.append(info.facing, i);
            LogUtil.e(tag, "id=%d, facing=%d, orientation=%d", i, info.facing, info.orientation);
        }
    }

    private int checkFacing(int facing) {
        int id = mFacingIds.get(facing, -1);
        if (id == -1) {
            throw new CameraException(CameraException.TYPE_LOG, "facing{%d} is error", facing);
        }
        return id;
    }

    @Override
    public void open(@NonNull CameraOpenCallback openCallback) {
        if (mCamera != null) {
            release();
        }
        mCameraId = checkFacing(mFacing);
        mCamera = Camera.open(mCameraId);
        mParams = mCamera.getParameters();
        if (mTextureView != null) {
            setupPreviewView(mTextureView);
        }
        mOpenCallback = openCallback;
        mOpenCallback.onOpened();
    }

    @Override
    public void release() {
        if (mCamera != null) {
            stopPreview();
            mCamera.release();
            mCamera = null;
            if (mOpenCallback != null) {
                mOpenCallback.onClosed();
            }
        }
    }

    @Override
    protected void updatePreviewView(@NonNull final SurfaceTexture texture,
                                     final int rotatedPreviewWidth, final int rotatedPreviewHeight,
                                     final int maxPreviewWidth, final int maxPreviewHeight) {
        if (mCamera == null || mTextureView == null) {
            return;
        }
        try {
            chooseOptimalSize(rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight);
            mParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setParameters(mParams);
            mParams = mCamera.getParameters();
            mCamera.setDisplayOrientation(getDegrees());
            texture.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height);
            mCamera.setPreviewTexture(texture);
        } catch (IOException e) {
            mPreviewSize = null;
            e.printStackTrace();
        }
    }

    private void chooseOptimalSize(int viewWidth, int viewHeight, int maxWidth, int maxHeight) {
        List<Camera.Size> sizes = mParams.getSupportedPreviewSizes();
        Camera.Size chooseSize = mPreviewSize;
        int diff = mPreviewSize == null ? computeDiff(0, 0, viewWidth, viewHeight)
                : computeDiff(mPreviewSize.width, mPreviewSize.height, viewWidth, viewHeight);
        for (Camera.Size option : sizes) {
            if (option.width <= maxWidth && option.height <= maxHeight) {
                int tempDiff = computeDiff(option.width, option.height, viewWidth, viewHeight);
                if (tempDiff > diff) {
                    continue;
                }
                diff = tempDiff;
                chooseSize = option;
            }
        }
        if (chooseSize == null) {
            chooseSize = sizes.get(0);
        }
        mPreviewSize = chooseSize;
    }

    /** 获取摄像头传感器方向. */
    protected int getSensorOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        return info.orientation;
    }

    private int getDegrees() {
        if (mDisplayRotation == -1) {
            throw new CameraException(CameraException.TYPE_LOG, "需要先调用setDisplayParams()");
        }
        return ORIENTATIONS.get(mDisplayRotation);
    }

    public void startPreview() {
        if (mCamera == null || mPreviewSize == null) {
            return;
        }
        mCamera.startPreview();
    }

    public void stopPreview() {
        if (mCamera == null) {
            return;
        }
        mCamera.stopPreview();
    }

    /**
     * 开启自动对焦模式
     */
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

    public void pointFocus(float x, float y, int viewWidth, int viewHeight, int focusAreaSize) {
        if (mCamera == null || !canAutoFocus()) {
            return;
        }
        /*
         * 相机坐标系:横屏长边为x轴，x轴和y轴数值固定是-1000 ~ 1000
         * 定点对焦需要将屏幕坐标系上的点转为相机坐标系的点
         */
        int left;
        int top;
        switch (getDegrees()) {
            // 横屏，两坐标系x、y轴重合
            case 0:
            case 180:
                left = Util.clamp((int) (x / viewWidth * 2000 - 1000), -1000, 1000 - focusAreaSize);
                top = Util.clamp((int) (y / viewHeight * 2000 - 1000), -1000, 1000 - focusAreaSize);
                break;
            // 竖屏，屏幕x轴是相机y轴，屏幕y轴是相机x轴
            case 90:
            case 270:
                left = Util.clamp((int) (y / viewHeight * 2000 - 1000), -1000, 1000 - focusAreaSize);
                top = Util.clamp((int) (x / viewWidth * 2000 - 1000), -1000, 1000 - focusAreaSize);
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
        mCamera.setParameters(mParams);
        mParams = mCamera.getParameters();
        mCamera.autoFocus(null);
    }

}
