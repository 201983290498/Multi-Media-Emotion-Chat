package com.wish.videopath.Emotion.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;


import com.wish.videopath.util.HttpHelper;
import com.wish.videopath.util.HttpResponseCallback;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatService extends Service {

    private final static String TAG = "ChatService";

    public class ChatClient extends Binder {
        private final static String TAG = "ChatClient";

        private OkHttpClient client;
        public ChatClient(){
            client = HttpHelper.getOkHttpClient();
        }

        public void sendMessage(String armUrl, String message, HttpResponseCallback callback) {
            // 处理接收到的消息
            HttpUrl.Builder urlBuilder = HttpUrl.parse(armUrl).newBuilder();
            urlBuilder.addQueryParameter("message", message);
            HttpUrl url = urlBuilder.build();
            Request build = new Request.Builder().url(url.toString()).get().build();
            Log.e(TAG, "sendMessage: " + message + "===============" + url.toString());
            Call call = client.newCall(build);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "聊天失败, 错误信息: " + e.getMessage());

                    // 打印堆栈跟踪信息
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(response.isSuccessful()){
                        callback.onResponse(response);
                    }
                }
            });
        }

        public void getEmotion(String emotionUrl,HttpResponseCallback callback) {
            // 向新的情感分析接口发送请求
            Request request = new Request.Builder().url(emotionUrl).get().build();
            Call call = client.newCall(request);
            Log.e(TAG, "getEmotion: ===============" + emotionUrl);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "获取情感失败, 错误信息: " + e.getMessage());
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(response.isSuccessful()){
                        callback.onResponse(response);
                    }
                }
            });
        }



    }

    private ChatClient chatClient;

    @Override
    public void onCreate() {
        super.onCreate();
        chatClient = new ChatClient();
        Log.e(TAG, "===OnCreate");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "====onBind===");
        return chatClient;
    }

}

