package com.tlong.gt.template.module.camera;

import android.Manifest;
import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.tlong.gt.template.R;
import com.tlong.gt.template.ui.BaseActivity;
import com.tlong.gt.template.util.PermissionUtil;

import java.io.File;

public class CameraPreviewActivity extends BaseActivity {

    private TextureView mPreviewView;
//    private CustomCamera mCamera;
    private CameraDep mCamera;
    private int mCameraFacing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        mPreviewView = (TextureView) findViewById(R.id.preview_view);
//        mCamera = new CustomCamera.Builder()
//                .facing(CustomCamera.FACING_BACK)
//                .preview(mPreviewView)
//                .builder();
        mCamera = new CameraDep();
        mCameraFacing = CameraDep.FACING_BACK;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPreviewView.isAvailable()) {
            openCamera();
        } else {
            mPreviewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    mCamera.updatePreviewSize(surface, width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }
    }

    @Override
    protected void onPause() {
        if (mCamera != null) {
            mCamera.release();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void openCamera() {
        PermissionUtil.request(mActivity)
                .permission(Manifest.permission.CAMERA)
                .callback(new PermissionUtil.Callback<PermissionUtil.Permission>() {
                    @Override
                    public void call(PermissionUtil.Permission permission) {
                        if (permission.isGranted()) {
                            mCamera.open(mCameraFacing);
                            mCamera.setupPreviewView(mPreviewView);
                            mCamera.startPreview();
                        }
                    }
                });
    }

    public void takePicture(View view) {
        if (mCamera != null) {
            mCamera.takePicture(getExternalCacheDir().getPath() + File.separator + SystemClock.uptimeMillis() + ".jpg");
        }
    }

    public void change(View view) {
        mCamera.release();
        if (mCameraFacing == CameraDep.FACING_BACK) {
            mCameraFacing = CameraDep.FACING_FRONT;
        } else {
            mCameraFacing = CameraDep.FACING_BACK;
        }
        openCamera();
    }
}
