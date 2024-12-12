package com.wish.videopath.Emotion;

import static com.wish.videopath.demo8.Demo8Activity.RTMPURL;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wish.videopath.Emotion.entity.Msg;
import com.wish.videopath.Emotion.service.ChatService;


import com.wish.videopath.R;
import com.wish.videopath.demo8.x264.AudioHelper;
import com.wish.videopath.demo8.x264.LivePush;
import com.wish.videopath.demo8.x264.VideoHelper;
import com.wish.videopath.util.BaseString;
import com.wish.videopath.util.HttpResponseCallback;
import com.wish.videopath.util.KDVoiceRegUtils;
import com.wish.videopath.util.MsgAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Response;

public class EmotionActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG="Emotion";
    public static final String RTMPURL = "rtmp://10.242.187.124:1935/live";
    private VideoHelper helper;
    private AudioHelper audioHelper;
    private LivePush livePush;
    private TextureView textureView;
    private RecyclerView recyclerView;
    private MsgAdapter adapter;
    private ArrayList<Msg> msgList;
    private HttpResponseCallback callback;
    private HttpResponseCallback emotionCallback;
    TextView emotionTextView;
    private boolean isProcess = false;
    private KDVoiceRegUtils voiceRegUtils;
    private boolean isListening = false; //
    public ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("Emotion", "=======connection clientService====="); // 绑定成功
            chatClient = (ChatService.ChatClient) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };
    private ChatService.ChatClient chatClient;
    private Button btnWakeUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_chat);
        bindElement();

        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO,
                        }, 666);
            } else {
                textureView.setSurfaceTextureListener(this);
            }
        } else {
            textureView.setSurfaceTextureListener(this);
        }
        //语音识别
        voiceRegUtils = KDVoiceRegUtils.getInstance();
        voiceRegUtils.initVoiceRecognize(this); // 初始化语音识别

        voiceRegUtils.setOnVoiceResultListener(new KDVoiceRegUtils.OnVoiceResultListener() {
            @Override
            public void onResult(String result) {
                // 将识别结果显示在 RecyclerView 中
                insertMsg(new Msg(result, BaseString.TYPE_SEND));
                Log.i(TAG, "onResult: 转化结果===="+result);
                // 将识别结果发送给后端
                if (chatClient != null) {
                    Log.i(TAG, "onResult: 也不为空啊");
                    chatClient.sendMessage(BaseString.chatUrl, result, callback);
                    Log.i(TAG, "onResult: 已发送消息");
                } else {
                    Log.e("Emotion", "ChatClient 未初始化");
                }
            }

            @Override
            public void onStart() {
                // 语音识别开始，可以更新 UI，例如显示一个麦克风图标
                Log.d("EmotionActivity", "语音识别开始");
                btnWakeUp.setText("识别中...");
            }

            @Override
            public void onError(String error) {
                // 处理识别错误
                Toast.makeText(EmotionActivity.this, "识别错误：" + error, Toast.LENGTH_SHORT).show();
                // 您可以选择在错误后重新启动语音识别，或者等待用户再次按下唤醒按钮
                isListening = false;
                btnWakeUp.setText("唤醒");
            }
        });


        //RecycleView
        msgList=new ArrayList<>();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MsgAdapter(msgList);
        recyclerView.setAdapter(adapter);
        onMainServiceConnected();
        //后端回调
        callback = new HttpResponseCallback() {
            @Override
            public void onResponse(Response response) {
                try {
                    JSONObject jsonResp = new JSONObject(response.body().string());
                    String msg = jsonResp.getString("response").replace("\n", "").replace(" ", "");
                    insertMsg(new Msg(msg, BaseString.TYPE_RECEIVE));
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e); 
                }
                isProcess = false;
            }
        };


        emotionCallback = new HttpResponseCallback() {
            @Override
            public void onResponse(Response response) {
                // TODO: 处理情感分析的响应
                try {
                    JSONObject jsonResp = new JSONObject(response.body().string());
                    String detectEmotion = jsonResp.getString("emotion").replace("\n", "").replace(" ", "");
                    insertEmotion(detectEmotion);
                    Log.e(TAG, "情绪为：" + detectEmotion);
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // 每隔一定时间请求一次情感分析
                        if (chatClient != null) {
                            Log.i(TAG, "请求情感分析...");
                            chatClient.getEmotion(BaseString.emotionUrl, emotionCallback);
                        } else {
                            Log.e("Emotion", "ChatClient 未初始化");
                        }
                        // 延迟一段时间后再进行下一次请求（例如每10秒请求一次）
                        Thread.sleep(5000);  // 10秒
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        //按钮唤醒识别功能
        btnWakeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 检查权限并开始语音识别
                handleWakeUpButtonClick();
            }
        });
    }


    private void bindElement(){
        textureView = findViewById(R.id.chat_media);
        recyclerView = findViewById(R.id.emotion_chat_dialog);
        btnWakeUp=findViewById(R.id.emotion_wake_up_btn);
        emotionTextView = findViewById(R.id.emotion_textview);
    }

    private void handleWakeUpButtonClick() {
        if (!isListening) {
            // 当前未在识别，尝试开始识别
            checkPermissionsAndStart();
        } else {
            // 当前正在识别，尝试停止识别
            voiceRegUtils.stopVoice();
            isListening = false;
            btnWakeUp.setText("唤醒");
            Log.d("Emotion", "语音识别已停止");
        }
    }

    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        1000);
            } else {
                // 开始语音识别
                startVoiceRecognition();
            }
        } else {
            // 开始语音识别
            startVoiceRecognition();
        }
    }

    private void startVoiceRecognition() {
        voiceRegUtils.startVoice(this);
        isListening = true;
        btnWakeUp.setText("识别中...");
        Log.d("Emotion", "开始语音识别");
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        startRTMP();
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    private void insertMsg(Msg msg) {
        new Handler(Looper.getMainLooper()).post(() -> {
            msgList.add(msg);
            adapter.notifyItemInserted(msgList.size() - 1);  // 定位到最后一行
            recyclerView.scrollToPosition(msgList.size() - 1); // 定位到最后一行
        });
    }

    private void insertEmotion(String emotion) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (emotionTextView != null) {
                    emotionTextView.setText(emotion);
                } else {
                    Log.e("EmotionRenderer", "TextView is not initialized");
                }

            }
        });
    }

    protected void onMainServiceConnected() {
        insertMsg(new Msg("你好，有什么可以帮助你的吗？", BaseString.TYPE_RECEIVE));
        btnWakeUp.setText("聆听中...");
        // 绑定聊天服务
        Intent chatIntent = new Intent(this, ChatService.class);
        bindService(chatIntent, connection, Context.BIND_AUTO_CREATE);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startRTMP() {
        //发送层
        livePush = new LivePush();

        //视频数据
        helper = new VideoHelper(this, textureView, livePush);
        helper.start();

        //音频数据
        audioHelper = new AudioHelper(livePush);
        audioHelper.startAudio();

        livePush.startLive(RTMPURL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (helper != null) {
            helper.closeCamera();
        }

        if (audioHelper != null) {
            audioHelper.stopAudio();
        }

        if (livePush != null) {
            livePush.stopLive();
        }
        if (voiceRegUtils != null) {
            voiceRegUtils.stopVoice();
        }
    }

}


