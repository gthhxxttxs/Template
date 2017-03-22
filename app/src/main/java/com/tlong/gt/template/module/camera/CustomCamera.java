package com.tlong.gt.template.module.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import com.tlong.gt.template.util.LogUtil;

/**
 * 封装的相机
 * new -> open -> startPreview -> stopPreview -> release
 * Created by v_gaoteng on 2017/3/20.
 */

public abstract class CustomCamera {

    protected final String tag = this.getClass().getSimpleName();

    /** 前置摄像头. */
    public static final int FACING_FRONT = frontFacing();
    /** 后置摄像头. */
    public static final int FACING_BACK = backFacing();

    private static int frontFacing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        return CameraCharacteristics.LENS_FACING_FRONT;
    }

    private static int backFacing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        return CameraCharacteristics.LENS_FACING_BACK;
    }

    protected static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    protected int mFacing;
    protected TextureView mTextureView;
    protected int mDisplayRotation = -1;
    protected Point mDisplaySize;

    public CustomCamera(Builder builder) {
        mFacing = builder.getFacing();
        mTextureView = builder.getTextureView();
    }

    /** 获取相机. */
    public abstract void open(CameraOpenCallback openCallback);

    public void open(int facing, CameraOpenCallback openCallback) {
        mFacing = facing;
        open(openCallback);
    }

    /** 释放相机. */
    public abstract void release();

    public void updatePreviewView(@NonNull SurfaceTexture texture, int width, int height) {
        if (width == 0 || height == 0) {
            throw new CameraException(CameraException.TYPE_LOG, "宽{%d},高{%d}都不能为0", width, height);
        }

        if (mDisplayRotation == -1 || mDisplaySize == null) {
            throw new CameraException(CameraException.TYPE_LOG, "需要先调用setDisplayParams()");
        }

        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        int maxPreviewWidth = mDisplaySize.x;
        int maxPreviewHeight = mDisplaySize.y;

        if (isSwappedDimensions(mDisplayRotation)) {
            rotatedPreviewWidth = height;
            rotatedPreviewHeight = width;
            maxPreviewWidth = mDisplaySize.y;
            maxPreviewHeight = mDisplaySize.x;
        }

        updatePreviewView(texture, rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight);
    }

    public void setDisplayParams(@NonNull Activity activity) {
        // 屏幕方向
        mDisplayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        // 屏幕尺寸
        if (mDisplaySize == null) {
            mDisplaySize = new Point();
        }
        activity.getWindowManager().getDefaultDisplay().getSize(mDisplaySize);
        LogUtil.e(tag, "displayRotation=%d, displaySizeX=%d, displaySizeY=%d",
                mDisplayRotation, mDisplaySize.x, mDisplaySize.y);
    }

    public void setupPreviewView(@NonNull TextureView textureView, TextureView.SurfaceTextureListener l) {
        mTextureView = textureView;
        setDisplayParams((Activity) textureView.getContext());
        if (textureView.isAvailable()) {
            updatePreviewView(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight());
        } else {
            if (l == null) {
                l = new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                        updatePreviewView(surface, width, height);
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                        updatePreviewView(surface, width, height);
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                    }
                };
            }
            textureView.setSurfaceTextureListener(l);
        }
    }

    private boolean isSwappedDimensions(int displayRotation) {
        int sensorOrientation = getSensorOrientation();
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                return sensorOrientation == 90 || sensorOrientation == 270;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                return sensorOrientation == 0 || sensorOrientation == 180;
            default:
                throw new CameraException(CameraException.TYPE_LOG, "displayRotation{%d} is error", displayRotation);
        }
    }

    protected int computeDiff(int sizeWidth, int sizeHeight, int viewWidth, int viewHeight) {
        int w = Math.abs(sizeWidth - viewWidth);
        int h = Math.abs(sizeHeight - viewHeight);
        return w * h + w + h;
    }

    /** 获取摄像头传感器方向. */
    protected abstract int getSensorOrientation();

    protected abstract void updatePreviewView(@NonNull SurfaceTexture texture,
                                              int rotatedPreviewWidth, int rotatedPreviewHeight,
                                              int maxPreviewWidth, int maxPreviewHeight);

    /** 开启预览. */
    public abstract void startPreview();

    /** 停止预览. */
    public abstract void stopPreview();

    public static class Builder {

        private Activity activity;
        private int facing;
        private TextureView textureView;

        public Builder activity(@NonNull Activity activity) {
            this.activity = activity;
            return this;
        }

        public Activity getActivity() {
            return activity;
        }

        public Builder facing(int facing) {
            this.facing = facing;
            return this;
        }

        public int getFacing() {
            return facing;
        }

        public Builder preview(@NonNull TextureView textureView) {
            this.textureView = textureView;
            return this;
        }

        public TextureView getTextureView() {
            return textureView;
        }

        public CustomCamera builder() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return new CustomCameraImpl(this);
            }
            if (activity == null) {
                if (textureView == null) {
                    throw new NullPointerException("context is null");
                }
                Context context = textureView.getContext();
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else {
                    throw new CameraException(CameraException.TYPE_LOG, "context{" + context + "} is not Activity");
                }
            }
            return new CustomCameraImpl2(this);
        }
    }

    public interface CameraErrorCallback {
        void onError(Throwable e);
    }

    public interface CameraOpenCallback {
        void onOpen();
        void onClose();
    }
}
