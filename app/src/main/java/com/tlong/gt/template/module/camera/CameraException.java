package com.tlong.gt.template.module.camera;

import android.util.AndroidRuntimeException;

import com.tlong.gt.template.util.Util;

/**
 * Created by v_gaoteng on 2017/3/21.
 */

public class CameraException extends AndroidRuntimeException {

    public static final int TYPE_LOG = 1;

    private int type;

    public CameraException(int type, String msg) {
        super(msg);
        this.type = type;
    }

    public CameraException(int type, String msg, Object... args) {
        super(Util.formatString(msg, args));
        this.type = type;
    }

    public CameraException(int type, String msg, Throwable e) {
        super(msg, e);
        this.type = type;
    }

    public CameraException(Exception e) {
        super(e);
    }
}
