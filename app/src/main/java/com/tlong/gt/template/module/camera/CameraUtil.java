package com.tlong.gt.template.module.camera;

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AndroidRuntimeException;
import android.view.Surface;

import java.util.List;

/**
 * 相机工具
 * Created by 高腾 on 2017/3/22.
 */

public class CameraUtil {

    /** 屏幕方向. */
    public static int getDisplayRotation(@NonNull Activity activity) {
        return activity.getWindowManager().getDefaultDisplay().getRotation();
    }

    /** 屏幕像素. */
    public static Point getDisplaySize(@NonNull Activity activity) {
        Point outSize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(outSize);
        return outSize;
    }

    /**
     * 屏幕和传感器方向不一致，在计算预览尺寸时需要调换宽高
     * @param displayRotation 屏幕方向
     * @param sensorOrientation 摄像头传感器方向
     * @return 是否需要调换宽高
     */
    public static boolean isSwappedDimensions(int displayRotation, int sensorOrientation) {
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                return sensorOrientation == 90 || sensorOrientation == 270;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                return sensorOrientation == 0 || sensorOrientation == 180;
            default:
                throw new AndroidRuntimeException("displayRotation{" + displayRotation + "} error");
        }
    }

    /** 根据屏幕方向获取相机数据转换角度. */
    public static int getDegrees(int displayRotation) {
        switch (displayRotation) {
            case Surface.ROTATION_0:
                return 90;
            case Surface.ROTATION_90:
                return 0;
            case Surface.ROTATION_180:
                return 270;
            case Surface.ROTATION_270:
                return 180;
            default:
                throw new AndroidRuntimeException("displayRotation{" + displayRotation + "} error");
        }
    }

    /** 获取预览尺寸. */
    public static <T> T getPreviewSize(@NonNull Activity activity,
                                       int width, int height,
                                       int sensorOrientation,
                                       List<T> sizes) {

        int displayRotation = getDisplayRotation(activity);
        Point displaySize = getDisplaySize(activity);

        return getPreviewSize(width, height,
                displaySize.x, displaySize.y,
                displayRotation, sensorOrientation,
                sizes);
    }

    public static <T> T getPreviewSize(int width, int height,
                                       int maxWidth, int maxHeight,
                                       int displayRotation, int sensorOrientation,
                                       List<T> sizes) {
        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        int maxPreviewWidth = maxWidth;
        int maxPreviewHeight = maxHeight;

        if (isSwappedDimensions(displayRotation, sensorOrientation)) {
            rotatedPreviewWidth = height;
            rotatedPreviewHeight = width;
            maxPreviewWidth = maxHeight;
            maxPreviewHeight = maxWidth;
        }

        return chooseOptimalSize(sizes, rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight);
    }

    /**
     * 选择最佳尺寸
     * @param sizes 备选尺寸集合
     * @param rotatedWidth 调整后的宽度
     * @param rotatedHeight 调整后的高度
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @param <T> Android 5.0 以下是{@link android.hardware.Camera.Size}, 之上是{@link android.util.Size}
     * @return 最佳尺寸
     */
    private static <T> T chooseOptimalSize(List<T> sizes,
                                           int rotatedWidth, int rotatedHeight,
                                           int maxWidth, int maxHeight) {
        T chooseSize = null;
        int diff = diff(0, 0, rotatedWidth, rotatedHeight);
        for (T option : sizes) {
            int tempDiff = compare(option, rotatedWidth, rotatedHeight, maxWidth, maxHeight, diff);
            if (tempDiff == diff) {
                continue;
            }
            diff = tempDiff;
            chooseSize = option;
        }
        return chooseSize;
    }

    private static <T> int compare(T t, int w, int h, int maxW, int maxH, int diff) {
        if (t instanceof android.hardware.Camera.Size) {
            android.hardware.Camera.Size size = (android.hardware.Camera.Size) t;
            return compare(size.width, size.height, w, h, maxW, maxH, diff);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && t instanceof android.util.Size) {
            android.util.Size size = (android.util.Size) t;
            return compare(size.getWidth(), size.getHeight(), w, h, maxW, maxH, diff);
        }
        return diff;
    }

    private static int compare(int w1, int h1, int w2, int h2, int maxW, int maxH, int diff) {
        if (w1 > maxW || h1 > maxH) {
            return diff;
        }
        int tempDiff = diff(w1, h1, w2, h2);
        return tempDiff < diff ? tempDiff : diff;
    }

    private static int diff(int w1, int h1, int w2, int h2) {
        int w = Math.abs(w1 - w2);
        int h = Math.abs(h1 - h2);
        return w * h + w + h;
    }

    /**
     * 数字过滤
     * @param num 源数字
     * @param min 最小值
     * @param max 最大值
     * @return 介于最小值和最大值之间的数字
     */
    public static int clamp(int num, int min, int max) {
        if (num > max) {
            return max;
        }
        if (num < min) {
            return min;
        }
        return num;
    }
}
