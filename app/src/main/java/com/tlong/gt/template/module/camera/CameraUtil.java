package com.tlong.gt.template.module.camera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.Surface;

import com.tlong.gt.template.util.LogUtil;

import java.nio.ByteBuffer;
import java.util.List;

import static android.util.Log.VERBOSE;

/**
 * 相机工具
 * Created by 高腾 on 2017/3/22.
 */

public class CameraUtil {

    private static final String TAG = CameraUtil.class.getSimpleName();

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

    /**
     * 前置摄像头因镜像关系需多旋转180度
     * @param displayRotation 屏幕方向
     * @param sensorOrientation 摄像头传感器方向
     * @return takePicture的data旋转角度
     */
    public static int getPictureDegrees(int displayRotation, int sensorOrientation) {
        return (getDegrees(displayRotation) + sensorOrientation + 270) % 360;
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

        return chooseSimilarSize(sizes, rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight);
    }

    private static final int SIMILAR = 0;
    private static final int LARGE = 1;

    private static <T> int dispense(int type, T t, int w, int h, int maxW, int maxH, int diff) {
        if (t instanceof android.hardware.Camera.Size) {
            android.hardware.Camera.Size size = (android.hardware.Camera.Size) t;
            switch (type) {
                case SIMILAR:
                    return similar(size.width, size.height, w, h, maxW, maxH, diff);
                case LARGE:
                    return large(size.width, size.height, w, h, maxW, maxH, diff);
                default:
                return diff;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && t instanceof android.util.Size) {
            android.util.Size size = (android.util.Size) t;
            switch (type) {
                case SIMILAR:
                    return similar(size.getWidth(), size.getHeight(), w, h, maxW, maxH, diff);
                case LARGE:
                    return large(size.getWidth(), size.getHeight(), w, h, maxW, maxH, diff);
                default:
                    return diff;
            }
        }
        return diff;
    }

    /**
     * 选择和传人的宽高最接近的尺寸
     * @param sizes 备选尺寸集合
     * @param rotatedWidth 调整后的宽度
     * @param rotatedHeight 调整后的高度
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @param <T> Android 5.0 以下是{@link android.hardware.Camera.Size}, 之上是{@link android.util.Size}
     * @return 最接近的尺寸
     */
    public static <T> T chooseSimilarSize(List<T> sizes,
                                           int rotatedWidth, int rotatedHeight,
                                           int maxWidth, int maxHeight) {
        T chooseSize = null;
        int diff = diffSimilar(0, 0, rotatedWidth, rotatedHeight);
        for (T option : sizes) {
            int tempDiff = dispense(SIMILAR, option, rotatedWidth, rotatedHeight, maxWidth, maxHeight, diff);
            if (tempDiff == diff) {
                continue;
            }
            diff = tempDiff;
            chooseSize = option;
        }
        return chooseSize;
    }

    private static int similar(int w1, int h1, int w2, int h2, int maxW, int maxH, int diff) {
        if (w1 > maxW || h1 > maxH) {
            return diff;
        }
        int tempDiff = diffSimilar(w1, h1, w2, h2);
        return tempDiff < diff ? tempDiff : diff;
    }

    private static int diffSimilar(int w1, int h1, int w2, int h2) {
        int w = Math.abs(w1 - w2);
        int h = Math.abs(h1 - h2);
        return w * h + w + h;
    }

    /**
     * 选择和传人宽高比例相同且最大尺寸
     * @param sizes 备选尺寸集合
     * @param normWidth 标准宽度
     * @param normHeight 标准高度
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @param <T> {@link android.hardware.Camera.Size}, {@link android.util.Size}
     * @return 最大尺寸
     */
    public static <T> T chooseLargeSize(List<T> sizes,
                                        int normWidth, int normHeight,
                                        int maxWidth, int maxHeight) {
        T chooseSize = null;
        int diff = 0;
        for (T size : sizes) {
            int tempDiff = dispense(LARGE, size, normWidth, normHeight, maxWidth, maxHeight, diff);
            if (tempDiff == diff) {
                continue;
            }
            diff = tempDiff;
            chooseSize = size;
        }
        return chooseSize;
    }

    private static int large(int w1, int h1, int w2, int h2, int maxW, int maxH, int diff) {
        if (w1 > maxW || h1 > maxH) {
            return diff;
        }
        int tempDiff = w1 + h1;
        return h1 == w1 * h2 / w2 && tempDiff > diff ? tempDiff : diff;
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

    /**
     * @param format YUV格式
     * @param width 像素宽度
     * @param height 像素高度
     * @return YUV数据长度
     */
    public static int yuvLength(int format, int width, int height) {
        int bitsPerPixel = ImageFormat.getBitsPerPixel(format);
        return width * height * bitsPerPixel / 8;
    }

    /**
     * YUV格式数据顺时针旋转90°
     * @param data 原始数据
     * @param imageWidth image宽度
     * @param imageHeight image高度
     * @return YUV数据
     */
    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        // 获取yuv数组
        byte[] yuv = new byte[data.length];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }

        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    public static boolean rotateYUV420Degree180(byte[] data, byte[] yuv, int imageWidth, int imageHeight) {
        int i;
        int count = 0;
        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }

        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }

        return true;
    }

    public static boolean rotateYUV420Degree270(byte[] data, byte[] yuv, int imageWidth, int imageHeight) {
        // Rotate the Y luma
        int i = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            for (int y = 0; y < imageHeight; y++) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }

        // Rotate the U and V color components
        i = imageWidth * imageHeight;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i++;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i++;
            }
        }
        return true;
    }

//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    private static byte[] getDataFromImage(Image image, int colorFormat) {
//        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
//            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
//        }
//        if (!isImageFormatSupported(image)) {
//            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
//        }
//        Rect crop = image.getCropRect();
//        int format = image.getFormat();
//        int width = crop.width();
//        int height = crop.height();
//
//        Image.Plane[] planes = image.getPlanes();
//        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
//        byte[] rowData = new byte[planes[0].getRowStride()];
//        int channelOffset = 0;
//        int outputStride = 1;
//        for (int i = 0; i < planes.length; i++) {
//            switch (i) {
//                case 0: // Y
//                    channelOffset = 0;
//                    outputStride = 1;
//                    break;
//                case 1: // U
//                    if (colorFormat == COLOR_FormatI420) {
//                        channelOffset = width * height;
//                        outputStride = 1;
//                    } else if (colorFormat == COLOR_FormatNV21) {
//                        channelOffset = width * height + 1;
//                        outputStride = 2;
//                    }
//                    break;
//                case 2: // V
//                    if (colorFormat == COLOR_FormatI420) {
//                        channelOffset = (int) (width * height * 1.25);
//                        outputStride = 1;
//                    } else if (colorFormat == COLOR_FormatNV21) {
//                        channelOffset = width * height;
//                        outputStride = 2;
//                    }
//                    break;
//            }
//            ByteBuffer buffer = planes[i].getBuffer();
//            int rowStride = planes[i].getRowStride();
//            int pixelStride = planes[i].getPixelStride();
//
//            Log.v(TAG, "pixelStride " + pixelStride);
//            Log.v(TAG, "rowStride " + rowStride);
//            Log.v(TAG, "width " + width);
//            Log.v(TAG, "height " + height);
//            Log.v(TAG, "buffer size " + buffer.remaining());
//
//            int shift = (i == 0) ? 0 : 1;
//            int w = width >> shift;
//            int h = height >> shift;
//            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
//            for (int row = 0; row < h; row++) {
//                int length;
//                if (pixelStride == 1 && outputStride == 1) {
//                    length = w;
//                    buffer.get(data, channelOffset, length);
//                    channelOffset += length;
//                } else {
//                    length = (w - 1) * pixelStride + 1;
//                    buffer.get(rowData, 0, length);
//                    for (int col = 0; col < w; col++) {
//                        data[channelOffset] = rowData[col * pixelStride];
//                        channelOffset += outputStride;
//                    }
//                }
//                if (row < h - 1) {
//                    buffer.position(buffer.position() + rowStride - length);
//                }
//            }
//
//            Log.v(TAG, "Finished reading data from plane " + i);
//        }
//        return data;
//    }
}
