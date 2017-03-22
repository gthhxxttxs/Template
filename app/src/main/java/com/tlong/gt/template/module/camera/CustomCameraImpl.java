package com.tlong.gt.template.module.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;
import android.view.TextureView;

import com.tlong.gt.template.util.LogUtil;

import java.io.IOException;
import java.util.List;

/**
 * Created by v_gaoteng on 2017/3/21.
 */

public class CustomCameraImpl extends CustomCamera {

    private SparseIntArray mFacingIds;
    private int mCameraId;
    private CameraOpenCallback mOpenCallback;
    private Camera mCamera;
    private Camera.Parameters mParams;
    private Camera.Size mPreviewSize;

    public CustomCameraImpl(Builder builder) {
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
            setupPreviewView(mTextureView, null);
        }
        mOpenCallback = openCallback;
        mOpenCallback.onOpen();
    }

    @Override
    public void release() {
        if (mCamera != null) {
            stopPreview();
            mCamera.release();
            mCamera = null;
            if (mOpenCallback != null) {
                mOpenCallback.onClose();
            }
        }
    }

    /**
     * 开启自动对焦模式
     */
    private void autoFocus() {
        String focusMode = mParams.getFocusMode();
        if (!Camera.Parameters.FOCUS_MODE_AUTO.equals(focusMode)) {
            List<String> focusModes = mParams.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(mParams);
                mParams = mCamera.getParameters();
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
            mCamera.setDisplayOrientation(ORIENTATIONS.get(mDisplayRotation));
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

    public void startPreview() {
        if (mCamera == null || mTextureView == null || mPreviewSize == null) {
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
}
