package com.wish.videopath.Emotion.utils;

public class LHelper {
    static {
        System.loadLibrary("native-lib");
    }

    public LHelper() {
        init();
    }

    public void startLive(String rtmpurl) {
        Livestart(rtmpurl);
    }


    public void stopLive() {
        Livestop();
        Liverelease();
    }
    public native void init();

    public native void Livestart(String url);

    public native void setVideoEncInfo(int width, int height, int fps, int bitrate);

    public native void pushVideo(byte[] data);

    public native void Livestop();

    public native void Liverelease();

    public native int initAudioCodec(int sampleRate, int channelCount);

    public native void pushAudio(byte[] buffer);
}
