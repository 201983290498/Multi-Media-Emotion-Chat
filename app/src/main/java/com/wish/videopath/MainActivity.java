package com.wish.videopath;

import android.Manifest;
import android.content.Intent;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.wish.videopath.Emotion.ActivityHome;
import com.wish.videopath.demo10.FFmpegActivity;
import com.wish.videopath.demo11.MusicActivity;
import com.wish.videopath.demo2.Demo2Activity;
import com.wish.videopath.demo3.Demo3Activity;
import com.wish.videopath.demo4.Demo4Activity;
import com.wish.videopath.demo5.Demo5Activity;
import com.wish.videopath.demo6.Demo6Activity;
import com.wish.videopath.demo7.Demo7Activity;
import com.wish.videopath.demo8.Demo8Activity;
import com.wish.videopath.demo9.Demo9Activity;
import com.wish.videopath.util.HttpHelper;

public class MainActivity extends AppCompatActivity {

    public static String LOG_TAG = "音视频";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,
                    }, 666);
        }
        bindElement();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        HttpHelper.getInstance();
    }
    private void bindElement() {
        findViewById(R.id.emotion_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hardwareTest(findViewById(R.id.emotion_btn));
            }
        });
    }

    public void hardwareTest(View view) {
        System.out.println(view.getId());

        if (view.getId()==R.id.emotion_btn){
            startActivity(new Intent(this, ActivityHome.class));
        }
    }


    /*//AudioRecord 和 AudioTrack API 完成音频 PCM 数据的采集和播放
    public void startUp2(View view) {
        Intent intent = new Intent(this, Demo2Activity.class);
        startActivity(intent);
    }

    // SurfaceView、TextureView 来预览 Camera
    public void startUp3(View view) {
        Intent intent = new Intent(this, Demo3Activity.class);
        startActivity(intent);
    }

    //MediaExtractor 和 MediaMuxer API，知道如何解析和封装 mp4 文件
    public void startUp4(View view) {
        Intent intent = new Intent(this, Demo4Activity.class);
        startActivity(intent);
    }

    //MediaCodec AAC编解码,将aac解析成pcm再编码回aac
    public void startUp5(View view) {
        Intent intent = new Intent(this, Demo5Activity.class);
        startActivity(intent);
    }

    //MediaCodec h264编解码,将摄像头采集的yuv数据编码成h264显示在surface中
    public void startUp6(View view) {
        Intent intent = new Intent(this, Demo6Activity.class);
        startActivity(intent);
    }

    //Camera2使用
    public void startUp7(View view) {
        Intent intent = new Intent(this, Demo7Activity.class);
        startActivity(intent);
    }

    //rtmp推流
    public void startUp8(View view) {
        Intent intent = new Intent(this, Demo8Activity.class);
        startActivity(intent);
    }

    //openGL
    public void startUp9(View view) {
        Intent intent = new Intent(this, Demo9Activity.class);
        startActivity(intent);
    }

    //ffmpeg
    public void startUp10(View view) {
        Intent intent = new Intent(this, FFmpegActivity.class);
        startActivity(intent);
    }

    //ffmpeg解码音乐使用opensl es播放
    public void startUp11(View view) {
        Intent intent = new Intent(this, MusicActivity.class);
        startActivity(intent);
    }

    public void startUp12(View view) {
        Intent intent = new Intent(this, ActivityHome .class);
        startActivity(intent);
    }*/
}