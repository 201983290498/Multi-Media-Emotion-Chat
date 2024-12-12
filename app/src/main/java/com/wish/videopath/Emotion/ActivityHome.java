package com.wish.videopath.Emotion;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.wish.videopath.Emotion.service.ChatService;
import com.wish.videopath.R;

import java.util.ArrayList;

public class ActivityHome extends AppCompatActivity {
    private TextView introduction;
    private String TAG = "ChatHomeActivity";
    private ArrayList<String> introStr;
    private boolean isTyping = true; // 循环标志
    private int currentStrIndex = 0;
    private int currentLength = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 设置屏幕常亮
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_home);
        introStr = new ArrayList<String>();
        introStr.add(getResources().getString(R.string.introduction2));
        introStr.add(getResources().getString(R.string.introduction1));
        Intent intent = new Intent(this, ChatService.class);
        startService(intent);
        bindElement();
    }

    private void bindElement() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                route(v);
            }
        };

        introduction = findViewById(R.id.chat_introduction);
        findViewById(R.id.chat_video).setOnClickListener(clickListener);
        startTyping(currentStrIndex);
    }

    private void route(View v){
        if(v.getId() == R.id.chat_video){
            startActivity(new Intent(this, EmotionActivity.class));
        } /*else if(v.getId() == R.id.chat_sample){
            startActivity(new Intent(this, ChatSampleActivity.class));
        }*/
    }


    private void startTyping(int currentStrIndex) {
        String text = introStr.get(currentStrIndex);
        SpannableString spannableString = new SpannableString(text);
        int length = text.length();
        for(int i = 0; i < length; i++) {
            // 创建一个值动画，改变字符的透明度
            ValueAnimator animator = ValueAnimator.ofInt(0, 255);
            animator.setDuration(200); // 动画持续时间
            animator.start();
            int finalI = i;
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                    int visibility = (int) animation.getAnimatedValue();
                    int argb = Color.argb(visibility, 254, 118, 0);
                    spannableString.setSpan(new ForegroundColorSpan(argb), finalI, finalI + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    introduction.setText(spannableString.subSequence(0, currentLength));
                }
            });
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(@NonNull Animator animation) {}
                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    currentLength += 1;
                    Log.i(TAG, "==================" + isTyping + (currentLength == introStr.get(currentStrIndex).length()));
                    if (isTyping && currentLength == introStr.get(currentStrIndex).length()) {
                        Log.i(TAG, "==================repeat");
                        // 动画结束后，重置文本和循环标志
                        resetTyping();
                    }
                }
                @Override
                public void onAnimationCancel(@NonNull Animator animation) {}
                @Override
                public void onAnimationRepeat(@NonNull Animator animation) {}
            });
            // 等待一段时间后启动下一个字符的动画
            animator.setStartDelay(i * 200); // 每个字符间隔100毫秒
        }
    }

    private void resetTyping() {
        introduction.setText("");
        // 重置循环标志
        isTyping = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isTyping = true;
                currentLength = 1;
                currentStrIndex = (currentStrIndex + 1) % 2;
                startTyping(currentStrIndex); // 开始新一轮逐字出现
            }
        }, 1000);
    }

    protected void onMainServiceConnected() {
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}