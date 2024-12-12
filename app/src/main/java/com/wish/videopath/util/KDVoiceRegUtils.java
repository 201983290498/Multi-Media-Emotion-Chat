package com.wish.videopath.util;

import android.content.Context;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * 科大讯飞
 * 语音识别工具类
 */
public class KDVoiceRegUtils {

    private SpeechRecognizer mIat;
    private RecognizerListener mRecognizerListener;

    private StringBuilder result = new StringBuilder();

    private static KDVoiceRegUtils instance;

    private OnVoiceResultListener onVoiceResultListener;

    private boolean isListening = false; // 标记当前是否正在进行语音识别
    private boolean isInitialized = false; // 标记是否初始化完成

    private KDVoiceRegUtils() {
        // 私有构造方法，防止外部实例化
    }

    /**
     * 获取单例实例
     */
    public static KDVoiceRegUtils getInstance() {
        if (instance == null) {
            synchronized (KDVoiceRegUtils.class) {
                if (instance == null) {
                    instance = new KDVoiceRegUtils();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化语音识别
     */
    public void initVoiceRecognize(Context context) {
        if (mIat != null) {
            Log.d("KDVoiceRegUtils", "SpeechRecognizer 已经初始化");
            return; // 已经初始化
        }

        // 创建 SpeechRecognizer 对象
        mIat = SpeechRecognizer.createRecognizer(context, new InitListener() {
            @Override
            public void onInit(int code) {
                Log.d("KDVoiceRegUtils", "SpeechRecognizer init() code = " + code);
                if (code != ErrorCode.SUCCESS) {
                    Log.e("KDVoiceRegUtils", "初始化失败，错误码：" + code);
                    if (onVoiceResultListener != null) {
                        onVoiceResultListener.onError("语音识别初始化失败，错误码：" + code);
                    }
                } else {
                    isInitialized = true;
                    setIatParam(); // 设置参数
                    Log.d("KDVoiceRegUtils", "语音识别初始化成功");
                }
            }
        });

        if (mIat != null) {
            // 初始化识别监听器
            mRecognizerListener = new RecognizerListener() {
                @Override
                public void onBeginOfSpeech() {
                    Log.d("KDVoiceRegUtils", "开始说话");
                    if (onVoiceResultListener != null) {
                        onVoiceResultListener.onStart();
                    }
                }

                @Override
                public void onError(SpeechError error) {
                    Log.e("KDVoiceRegUtils", "识别错误：" + error.getPlainDescription(true));
                    isListening = false; // 重置标记
                    if (onVoiceResultListener != null) {
                        onVoiceResultListener.onError(error.getPlainDescription(true));
                    }
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d("KDVoiceRegUtils", "结束说话");
                }

                @Override
                public void onResult(RecognizerResult results, boolean isLast) {
                    String text = parseIatResult(results.getResultString());
                    result.append(text);
                    Log.d("KDVoiceRegUtils", "识别结果：" + text);
                    if (isLast) {
                        String finalResult = result.toString();
                        result.setLength(0);
                        isListening = false; // 重置标记
                        if (onVoiceResultListener != null) {
                            onVoiceResultListener.
                                    onResult(finalResult);
                        }
                    }
                }

                @Override
                public void onVolumeChanged(int volume, byte[] data) {
                    Log.d("KDVoiceRegUtils", "当前音量：" + volume);
                    // 可根据需要实现音量显示
                }

                @Override
                public void onEvent(int eventType, int arg1, int arg2, android.os.Bundle obj) {
                    // 处理其他事件
                }
            };
        }
    }

    /**
     * 设置语音识别参数
     */
    private void setIatParam() {
        if (mIat == null) {
            Log.e("KDVoiceRegUtils", "SpeechRecognizer 未初始化，无法设置参数");
            return;
        }

        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);

        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000"); // 4秒

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入，自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000"); // 1秒

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");

        Log.d("KDVoiceRegUtils", "语音识别参数设置完成");
    }

    /**
     * 开始语音识别
     */
    public void startVoice(Context context) {
        if (!isInitialized) {
            Log.e("KDVoiceRegUtils", "语音识别未初始化完成");
            if (onVoiceResultListener != null) {
                onVoiceResultListener.onError("语音识别未初始化完成");
            }
            return;
        }
        if (isListening) {
            Log.d("KDVoiceRegUtils", "正在进行语音识别，无法再次启动");
            return;
        }
        if (mIat == null) {
            Log.e("KDVoiceRegUtils", "SpeechRecognizer 未初始化");
            if (onVoiceResultListener != null) {
                onVoiceResultListener.onError("SpeechRecognizer 未初始化");
            }
            return;
        }
        int ret = mIat.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.e("KDVoiceRegUtils", "听写失败，错误码：" + ret);
            if (onVoiceResultListener != null) {
                onVoiceResultListener.onError("听写失败，错误码：" + ret);
            }
        } else {
            isListening = true;
            Log.d("KDVoiceRegUtils", "开始听写");
        }
    }

    /**
     * 停止语音识别
     */
    public void stopVoice() {
        if (mIat != null) {
            mIat.stopListening();
            isListening = false;
            Log.d("KDVoiceRegUtils", "停止听写");
        }
    }

    /**
     * 解析识别结果
     */
    private String parseIatResult(String json) {
        StringBuilder ret = new StringBuilder();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                ret.append(obj.getString("w"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret.toString();
    }

    /**
     * 设置语音识别结果监听器
     */
    public void setOnVoiceResultListener(OnVoiceResultListener listener) {
        this.onVoiceResultListener = listener;
    }

    /**
     * 语音识别结果监听接口
     */
    public interface OnVoiceResultListener {
        /**
         * 识别结果回调
         *
         * @param result 识别出的文本
         */
        void onResult(String result);

        /**
         * 识别开始回调
         */
        void onStart();

        /**
         * 识别错误回调
         *
         * @param error 错误信息
         */
        void onError(String error);
    }
}
