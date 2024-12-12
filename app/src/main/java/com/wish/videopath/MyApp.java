package com.wish.videopath;

import android.app.Application;
import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class MyApp extends Application {

    public void onCreate() {
        super.onCreate();
        // 初始化科大讯飞 SpeechUtility
        String appId = getString(R.string.IflytekAPP_id);
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=" + appId);
        Log.d("MyApp", "SpeechUtility initialized with APPID: " + appId);
    }
}
