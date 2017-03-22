package com.tlong.gt.template.module.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.SparseArray;

/**
 * Created by v_gaoteng on 2017/3/21.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CustomCameraImpl2 extends CustomCamera {

    CameraManager mManager;
    SparseArray<String> mFacingIds;

    public CustomCameraImpl2(Builder builder) {
        super(builder);
        initFacingIds(builder.getActivity());
    }

    private void initFacingIds(Context context) {
        try {
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

    @Override
    public void open(CameraOpenCallback openCallback) {

    }


    @Override
    public void release() {

    }

    @Override
    protected int getSensorOrientation() {
        return 0;
    }

    @Override
    protected void updatePreviewView(@NonNull SurfaceTexture texture, int rotatedPreviewWidth, int rotatedPreviewHeight, int maxPreviewWidth, int maxPreviewHeight) {

    }

    @Override
    public void startPreview() {

    }

    @Override
    public void stopPreview() {

    }
}
