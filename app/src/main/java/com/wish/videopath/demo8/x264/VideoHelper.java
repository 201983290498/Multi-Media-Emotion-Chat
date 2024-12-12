package com.wish.videopath.demo8.x264;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.wish.videopath.util.ImageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import static com.wish.videopath.MainActivity.LOG_TAG;

/**
 * Camera2 获取数据通过LivePush发送给x264进行编码然后推送到rtmp服务器
 */
public class VideoHelper {

    private LivePush livePush;
    private TextureView textureView;
    private Context context;
    private CameraManager mCameraManager;
    private String mBackCameraId, mFrontCameraId;
    private CameraCharacteristics mBackCameraCharacteristics, mFrontCameraCharacteristics;
    private Integer mSensorOrientation;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCRBuilder;
    private Handler mCameraHandler;
    private CameraCaptureSession mCameraSesstion;
    private CameraYUVReadListener readListener;
    Point maxPreviewSize = new Point(1920, 1080);
    Point minPreviewSize = new Point(1280, 720);
    Point previewViewSize;
    private Size mPreviewSize;
    private byte[] y;
    private byte[] u;
    private byte[] v;

    private byte[] yBuffer;
    private byte[] uBuffer;
    private byte[] vBuffer;
    private ReentrantLock lock = new ReentrantLock();
    private byte[] nv21, nv21_rotated;
    private boolean isLive = true;

    private int totalRotation;

    public VideoHelper(Context context, TextureView textureView, LivePush livePush) {
        this.context = context;
        this.textureView = textureView;
        this.livePush = livePush;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public synchronized void start() {
        // 获取Camera2服务
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            // 当前手机支持的摄像头id
            String[] idList = mCameraManager.getCameraIdList();
            for (String cameraId : idList) {
                // 拿到装在所有相机信息的  CameraCharacteristics 类
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                // 拿到相机的方向，前置，后置，外置
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        // 前置摄像头优先使用
                        mFrontCameraId = cameraId;
                        mFrontCameraCharacteristics = characteristics;
                        Log.i(LOG_TAG, "获取到前置摄像头");
                    } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        // 后置摄像头备用
                        mBackCameraId = cameraId;
                        mBackCameraCharacteristics = characteristics;
                        Log.i(LOG_TAG, "获取到后置摄像头");
                    }
                }
            }

            // 如果找到了前置摄像头，则优先打开前置摄像头，否则使用后置摄像头
            if (mFrontCameraId != null) {
                openCamera(mFrontCameraId);
            } else if (mBackCameraId != null) {
                openCamera(mBackCameraId);
            } else {
                Log.e(LOG_TAG, "没有找到可用的摄像头");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开摄像头
     */
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera(String cameraId) throws CameraAccessException {
        // 获取配置的map
        StreamConfigurationMap map = mBackCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mSensorOrientation = mBackCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Log.i(LOG_TAG, "Camera sensor orientation: " + mSensorOrientation);

        // 获取设备旋转角度
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        // 对于后置摄像头，计算总的旋转角度
        totalRotation = (mSensorOrientation - degrees + 360) % 360;
        Log.i(LOG_TAG, "totalRotation: " + totalRotation);

        // 获取预览尺寸
        Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
        // 获取最佳尺寸
        mPreviewSize = getBestSize(previewSizes);
        Log.i(LOG_TAG, "获取最佳尺寸,width: " + mPreviewSize.getWidth() + " height:" + mPreviewSize.getHeight());

        // 初始化ImageReader
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);

        int videoWidth = mPreviewSize.getWidth();
        int videoHeight = mPreviewSize.getHeight();

        if (totalRotation == 90 || totalRotation == 270) {
            // 交换宽高
            videoWidth = mPreviewSize.getHeight();
            videoHeight = mPreviewSize.getWidth();
        }

        if (isLive && livePush != null) {
            livePush.native_setVideoEncInfo(videoWidth, videoHeight,
                    15, videoWidth * videoHeight * 3 / 2);
        }

        // 启动摄像头线程和图像监听
        HandlerThread cameraThread = new HandlerThread("camera");
        cameraThread.start();
        mCameraHandler = new Handler(cameraThread.getLooper());

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                if (image == null) return;

                if (image.getFormat() == ImageFormat.YUV_420_888) {
                    Image.Plane[] planes = image.getPlanes();
                    lock.lock();
                    try {
                        int width = image.getWidth();
                        int height = image.getHeight();

                        // 初始化缓冲区大小
                        int ySize = width * height;
                        int uvSize = width * height / 4; // U 和 V 各占总像素的 1/4

                        if (yBuffer == null || yBuffer.length != ySize) {
                            yBuffer = new byte[ySize];
                            uBuffer = new byte[uvSize];
                            vBuffer = new byte[uvSize];
                            nv21 = new byte[ySize + uvSize * 2];
                            nv21_rotated = new byte[ySize + uvSize * 2];
                        }

                        // 获取 YUV 数据
                        ImageUtil.getDataFromPlane(planes[0], yBuffer, width, height);
                        ImageUtil.getDataFromPlane(planes[1], uBuffer, width / 2, height / 2);
                        ImageUtil.getDataFromPlane(planes[2], vBuffer, width / 2, height / 2);

                        // 转换为 NV21 格式
                        ImageUtil.yuvToNv21test(yBuffer, uBuffer, vBuffer, nv21);

                        // 根据 totalRotation 旋转图像数据
                        switch (totalRotation) {
                            case 0:
                                System.arraycopy(nv21, 0, nv21_rotated, 0, nv21.length);
                                break;
                            case 90:
                                ImageUtil.rotateYUV420Degree90(nv21, nv21_rotated, width, height);
                                break;
                            case 180:
                                ImageUtil.rotateYUV420Degree180(nv21, nv21_rotated, width, height);
                                break;
                            case 270:
                                ImageUtil.rotateYUV420Degree270(nv21, nv21_rotated, width, height);
                                break;
                        }

                        // 推送视频数据
                        if (isLive && livePush != null) {
                            livePush.native_pushVideo(nv21_rotated);
                        }
                    } finally {
                        lock.unlock();
                        image.close();
                    }
                }
            }
        }, mCameraHandler);

        // 打开摄像头
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                // 摄像头被打开
                Log.i(LOG_TAG, "摄像头被打开了 onOpened回调");
                mCameraDevice = camera;
                try {
                    createCameraSession();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.i(LOG_TAG, "摄像头关闭 onDisconnected");
                camera.close();
                mCameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.i(LOG_TAG, "摄像头出错 " + error);
                camera.close();
                mCameraDevice = null;
            }
        }, mCameraHandler);
    }

    //建立摄像头数据会话，设置两个数据出口
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createCameraSession() throws CameraAccessException {
        //构建Surface，将摄像头数据输入到我们TextureView中
        SurfaceTexture texture = textureView.getSurfaceTexture();
        /**
         * 配置预览属性
         * 与 Camera1 不同的是，Camera2 是把尺寸信息给到 Surface (SurfaceView 或者 ImageReader)，
         * Camera2 会根据 Surface 配置的大小，输出对应尺寸的画面;
         * 注意摄像头的 width > height ，而我们使用竖屏，所以宽高要变化一下
         */
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Log.i(LOG_TAG, "当前设置的size尺寸,width: " + textureView.getWidth() + " height:" + textureView.getHeight());

        Surface surface = new Surface(texture);

        //开启请求
        mCRBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mCRBuilder.addTarget(surface);

        //设置拍照模式
        mCRBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        //摄像头数据输出到通道中
        mCRBuilder.addTarget(mImageReader.getSurface());

        //构建会话链接,输出的surface数量代表输出的通道数量
        mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                Log.i(LOG_TAG, "摄像头会话建立 ");
                mCameraSesstion = session;
                //设置重复请求
                try {
                    mCameraSesstion.setRepeatingRequest(mCRBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    }, mCameraHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        }, mCameraHandler);

        // 配置预览的变换矩阵
        configureTransform(textureView.getWidth(), textureView.getHeight());
    }

    /**
     * 配置预览的变换矩阵
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == textureView || null == mPreviewSize) {
            return;
        }
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    public void closeCamera() {
        isLive = false;
        if (mCameraSesstion != null) {
            mCameraSesstion.close();
        }
    }

    /**
     * 根据当前texture宽高从摄像头支持的分辨率中获取最合适的分辨率
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Size getBestSize(Size[] previewSizes) {
        List<Size> sizes = Arrays.asList(previewSizes);
        Size defaultSize = sizes.get(0);
        Size[] tempSizes = sizes.toArray(new Size[0]);
        Arrays.sort(tempSizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.getWidth() > o2.getWidth()) {
                    return -1;
                } else if (o1.getWidth() == o2.getWidth()) {
                    return o1.getHeight() > o2.getHeight() ? -1 : 1;
                } else {
                    return 1;
                }
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));
        for (int i = sizes.size() - 1; i >= 0; i--) {
            if (maxPreviewSize != null) {
                if (sizes.get(i).getWidth() > maxPreviewSize.x || sizes.get(i).getHeight() > maxPreviewSize.y) {
                    sizes.remove(i);
                    continue;
                }
            }
            if (minPreviewSize != null) {
                if (sizes.get(i).getWidth() < minPreviewSize.x || sizes.get(i).getHeight() < minPreviewSize.y) {
                    sizes.remove(i);
                }
            }
        }
        if (sizes.size() == 0) {
            String msg = "can not find suitable previewSize, now using default";
            return defaultSize;
        }
        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {
            previewViewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }

        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }

        for (Size s : sizes) {
            if (Math.abs((s.getHeight() / (float) s.getWidth()) - previewViewRatio) < Math.abs(bestSize.getHeight() / (float) bestSize.getWidth() - previewViewRatio)) {
                bestSize = s;
            }
        }
        return bestSize;
    }

    public void setOnCameraDataPreviewListener(CameraYUVReadListener readListener) {
        this.readListener = readListener;
    }

    public interface CameraYUVReadListener {
        void onPreview(byte[] y, byte[] u, byte[] v, Size width, int stride);
    }

}
