package com.wish.videopath.util;

public class BaseString {
    public static String AUDIO = "audio";
    public static String PNG = "png";
    public static String VIDEO = "video";
    public static String TEXT = "text";


    public static String AUDIO_TYPE = "application/octet-stream";
    public static String VIDEO_TYPE = "video/mp4";
    public static String PNG_TYPE = "image/png";
    public static String TEXT_TYPE = "text/plain";

    /**
     * 以下是机器人的提示语句库
     */

    /**
     * 数据库的表名字
     */
    public static String STUDENT_TABLE = "";

    /**
     * USB存储，数据库
     */
    public static String usbPath = "/storage/usbhost";

    public static String datasetTextPath = "/storage/usbhost/dataset.txt";

    public static String datasetImagePath = "/storage/usbhost/dataset";

    public static String serveUrl="http://10.242.187.118:8082/emotionDetect";

    /*public static String chatUrl="http://10.242.187.118:8082/chat";*/
    public static String chatUrl="http://39.100.48.36:8802/chat"; // # 普通chat
    public static String chatSbertUrl="http://10.242.187.118:8082/chat_sbert";
    public static String emotionUrl="http://10.242.187.124:5000/get_emotions";
    public static String uploadArg = "file";

    /**
     * 机器人相关字符窜
     */
    public static final int TYPE_RECEIVE = 1; // 接收到信息, 就是机器人发的消息
    public static final int TYPE_SEND = 0; // 发送信息, 自己发送的消息
}
