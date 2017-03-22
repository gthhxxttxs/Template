package com.tlong.gt.template.module.camera;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.AndroidRuntimeException;
import android.util.SparseIntArray;
import android.view.TextureView;

import com.tlong.gt.template.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用Android 5.0 过期的API
 * Created by 高腾 on 2017/3/22.
 */
public class CameraDep {

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
        } catch (IOException e) {
            mPreviewSize = null;
            e.printStackTrace();
        }
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
}
