package com.wish.videopath.Emotion.entity;

public class Msg {
    public String msg; // 消息的内容

    public int type; // 1. 机器人发送的消息, 0. 用户发送的消息

    public Msg(String s, int typeReceive) {
        msg = s;
        type = typeReceive;
    }
}
