package com.tlong.gt.template.module.camera;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.TextureView;

import com.tlong.gt.template.R;
import com.tlong.gt.template.ui.BaseActivity;
import com.tlong.gt.template.util.PermissionUtil;

public class CameraPreviewActivity extends BaseActivity {

    private TextureView mPreviewView;
    private CustomCamera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        mPreviewView = (TextureView) findViewById(R.id.preview_view);
        mCamera = new CustomCamera.Builder()
                .facing(CustomCamera.FACING_BACK)
                .preview(mPreviewView)
                .builder();
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
                    mCamera.updatePreviewView(surface, width, height);
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
    protected void onDestroy() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        super.onDestroy();
    }

    private void openCamera() {
        PermissionUtil.request(mActivity)
                .permission(Manifest.permission.CAMERA)
                .callback(new PermissionUtil.Callback<PermissionUtil.Permission>() {
                    @Override
                    public void call(PermissionUtil.Permission permission) {
                        if (permission.isGranted()) {
                            mCamera.open(new CustomCamera.CameraOpenCallback() {
                                @Override
                                public void onOpened() {
                                    mCamera.startPreview();
                                }

                                @Override
                                public void onClosed() {

                                }
                            });
                        }
                    }
                });
    }
}
