package com.wish.videopath.util;

import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

public class ImageUtil {

    /**
     * Camera2 返回yuv三路数据转换成NV21,V在前，u在后
     *
     * @param y      Y 数据
     * @param u      U 数据
     * @param v      V 数据
     * @param nv21   生成的nv21，需要预先分配内存
     * @param stride 步长
     * @param height 图像高度
     */
    public static void yuvToNv21(byte[] y, byte[] u, byte[] v, byte[] nv21, int stride, int height) {
        System.arraycopy(y, 0, nv21, 0, y.length);
        // 注意，若length值为 y.length * 3 / 2 会有数组越界的风险，需使用真实数据长度计算
        int length = y.length + u.length / 2 + v.length / 2;
        int uIndex = 0, vIndex = 0;
        for (int i = stride * height; i < length; i += 2) {
            nv21[i] = v[vIndex];
            nv21[i + 1] = u[uIndex];
            vIndex += 2;
            uIndex += 2;
        }
    }
    public static void yuvToNv21test(byte[] y, byte[] u, byte[] v, byte[] nv21) {
        int ySize = y.length;
        int uvSize = u.length;

        // 复制 Y 数据
        System.arraycopy(y, 0, nv21, 0, ySize);

        // 交织 U 和 V 数据（NV21 格式为 VU 交错）
        for (int i = 0; i < uvSize; i++) {
            nv21[ySize + i * 2] = v[i];
            nv21[ySize + i * 2 + 1] = u[i];
        }
    }


    /**
     * 旋转yuv数据，将横屏数据转换为竖屏
     */
    public static void revolveYuv(byte[] nv21, byte[] nv21_rotated, int width, int height) {
        int y_size = width * height;
        //uv高度
        int uv_height = height >> 1;
        //旋转y,左上角跑到右上角，左下角跑到左上角，从左下角开始遍历
        int k = 0;
        for (int i = 0; i < width; i++) {
            for (int j = height - 1; j > -1; j--) {
                nv21_rotated[k++] = nv21[width * j + i];
            }
        }
        //旋转uv
        for (int i = 0; i < width; i += 2) {
            for (int j = uv_height - 1; j > -1; j--) {
                nv21_rotated[k++] = nv21[y_size + width * j + i];
                nv21_rotated[k++] = nv21[y_size + width * j + i + 1];
            }
        }
    }

    /**
     * nv21 转换成nv12
     */
    public static void nv21ToNv12(byte[] nv21, byte[] nv12, int width, int height) {
        int frameSize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, frameSize);
        for (i = 0; i < frameSize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j - 1] = nv21[j + frameSize];
        }
        for (j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j] = nv21[j + frameSize - 1];
        }
    }
    public static void getBytesFromImage(Image image, byte[] nv21) {
        Log.i("util", "getBytesFromImage: 进入变换函数");
        Image.Plane[] planes = image.getPlanes();
        int width = image.getWidth();
        int height = image.getHeight();

        // NV21 格式的总字节数
        int ySize = width * height;
        int uvSize = width * height / 2;

        // 拿到各个平面的缓冲区
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        // 拿到各个平面的跨度
        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride();
        int uRowStride = planes[1].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vRowStride = planes[2].getRowStride();
        int vPixelStride = planes[2].getPixelStride();

        // 复制 Y 平面数据
        yBuffer.position(0);
        for (int row = 0; row < height; row++) {
            int yOffset = row * width;
            int bufferPos = row * yRowStride;
            for (int col = 0; col < width; col++) {
                nv21[yOffset + col] = yBuffer.get(bufferPos + col * yPixelStride);
            }
        }

        // 复制 UV 平面数据（NV21 格式为 VU 排列）
        int uvHeight = height / 2;
        for (int row = 0; row < uvHeight; row++) {
            int uvOffset = ySize + row * width;
            int uBufferPos = row * uRowStride;
            int vBufferPos = row * vRowStride;
            for (int col = 0; col < width / 2; col++) {
                // 注意，这里 width / 2，因为 UV 平面的宽度是 Y 平面的一半
                int uIndex = uBufferPos + col * uPixelStride;
                int vIndex = vBufferPos + col * vPixelStride;
                nv21[uvOffset + col * 2] = vBuffer.get(vIndex);       // V
                nv21[uvOffset + col * 2 + 1] = uBuffer.get(uIndex);   // U
            }
        }
    }

    public static void getDataFromPlane(Image.Plane plane, byte[] data, int width, int height) {
        ByteBuffer buffer = plane.getBuffer();

        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();

        int offset = 0;

        byte[] rowData = new byte[rowStride];

        for (int row = 0; row < height; row++) {
            // 确保不读取超过缓冲区剩余数据
            int bytesToRead = Math.min(buffer.remaining(), rowStride);
            buffer.get(rowData, 0, bytesToRead);

            int maxWidth = Math.min(width, bytesToRead / pixelStride);
            for (int col = 0; col < maxWidth; col++) {
                data[offset++] = rowData[col * pixelStride];
            }
        }
    }


    public static void rotateYUV420Degree90(byte[] data, byte[] output, int width, int height) {
        int ySize = width * height;
        int uvHeight = height / 2;

        // 旋转 Y 平面
        int i = 0;
        for (int x = 0; x < width; x++) {
            for (int y = height - 1; y >= 0; y--) {
                output[i++] = data[y * width + x];
            }
        }

        // 旋转 UV 平面
        for (int x = 0; x < width; x += 2) {
            for (int y = uvHeight - 1; y >= 0; y--) {
                int index = ySize + y * width + x;
                output[i++] = data[index + 1]; // V
                output[i++] = data[index];     // U
            }
        }
    }

    public static void rotateYUV420Degree180(byte[] data, byte[] output, int width, int height) {
        int ySize = width * height;
        int uvSize = ySize / 2;

        // 旋转 Y 平面
        for (int i = 0; i < ySize; i++) {
            output[i] = data[ySize - i - 1];
        }

        // 旋转 UV 平面
        int totalSize = ySize + uvSize;
        for (int i = ySize; i < totalSize - 1; i += 2) {
            output[i] = data[totalSize - (i - ySize) - 2];
            output[i + 1] = data[totalSize - (i - ySize)];
        }
    }

    // 旋转 270 度
    public static void rotateYUV420Degree270(byte[] data, byte[] output, int width, int height) {
        int ySize = width * height;
        int uvHeight = height / 2;

        // 旋转 Y 平面
        int i = 0;
        for (int x = width - 1; x >= 0; x--) {
            for (int y = 0; y < height; y++) {
                output[i++] = data[y * width + x];
            }
        }

        // 旋转 UV 平面
        for (int x = width - 2; x >= 0; x -= 2) {
            for (int y = 0; y < uvHeight; y++) {
                int index = ySize + y * width + x;
                output[i++] = data[index + 1]; // V
                output[i++] = data[index];     // U
            }
        }
    }

}
