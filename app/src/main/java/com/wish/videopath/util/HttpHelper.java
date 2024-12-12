package com.wish.videopath.util;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionSpec;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;

public class HttpHelper {
    private static final String TAG = "HttpHelper";
    /**
     * 初始化一个okHttpClient应用
     */
    private static OkHttpClient okHttpClient = null;

    /**
     * 单例模式获取一个okHttpClient实例
     * @return 返回okhttpClient
     */
    public static OkHttpClient getInstance(){
        if(okHttpClient == null){
            okHttpClient = new OkHttpClient();
        }
        return okHttpClient;
    }

    /**
     * TODO 发送同步的get请求, 暂时不设置返回的内容
     * @param url 传入的url
     */
    public static void getSync(String url, HttpResponseCallback callback){
        new Thread() {
            @Override
            public void run(){
                Request build = new Request.Builder().url(url).build();
                Call call = okHttpClient.newCall(build);
                try {
                    Response response = call.execute();
                    Log.i(TAG, response.body().string());
                    callback.onResponse(response);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }
    /**
     * @param url 异步get请求
     */
    public static void getAsync(String url, HttpResponseCallback callback){
        Request build = new Request.Builder().url(url).build();
        Call call = okHttpClient.newCall(build);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    Log.i(TAG, response.body().string());
                    callback.onResponse(response);
                }
            }
        });
    }

    /**
     * Post 同步请求
     * @param url 请求的地址
     * @param formBody 需要传递的参数
     */
    public static void postSync(String url, FormBody formBody, HttpResponseCallback callback){
        if (formBody == null){
            formBody = new FormBody.Builder().add("a", "1").build();
        }
        Request build = new Request.Builder().url(url).post(formBody).build();
        new Thread(){
            @Override
            public void run(){
                Call call = okHttpClient.newCall(build);
                try {
                    Response response = call.execute();
                    Log.i(TAG, response.body().string());
                    callback.onResponse(response);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

    /**
     * TODO Post 异步请求, 异步请求有需要的操作则变成一个继承某个interface的接口
     * @param url 异步请求的地址
     * @param formBody 异步请求的数据体
     */
    public static void postAsync(String url, FormBody formBody, HttpResponseCallback callback){
        if (formBody == null) {
            formBody = new FormBody.Builder().add("a", "1").build();
        }
        Request build = new Request.Builder().url(url).post(formBody).build();
        Call call = okHttpClient.newCall(build);
        call.enqueue(new Callback(){

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    Log.i(TAG, response.body().string());
                    callback.onResponse(response);
                }
            }
        });
    }

    /**
     * 上传本地的各种文件，支持包括：mp4, wav, png, text
     * @param url 上传的地址
     * @param filePath 文件的路径
     * @param fileName 文件的名称
     * @param type 文件的类型
     * @param callback 上传之后的回调函数
     */
    public static void uploadFileTest(String url, String filePath, String fileName, String type, HttpResponseCallback callback) {
        new Thread(){
            @Override
            public void run() {
                Log.e("Helper", "run http: "+ filePath);
                File file = new File(filePath);
                MultipartBody multipartBody = null;
                MultipartBody.Part file_part = null;
                MultipartBody.Part args_part = MultipartBody.Part.createFormData("folder", file.getParentFile().getName()); // 获取父亲文件夹
                if(type.equals(BaseString.AUDIO)){ // 传递音频数据
                    file_part = MultipartBody.Part.createFormData(fileName, file.getName(), RequestBody.create(MediaType.parse(BaseString.AUDIO_TYPE), file));
                    multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addPart(file_part).addPart(args_part).build();
                } else if (type.equals(BaseString.VIDEO)){  // 传递视频文件
                    file_part = MultipartBody.Part.createFormData(fileName, file.getName(), RequestBody.create(MediaType.parse(BaseString.VIDEO_TYPE), file));
                    multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addPart(file_part).addPart(args_part).build();
                } else if (type.equals(BaseString.PNG)) {
                    file_part = MultipartBody.Part.createFormData(fileName, file.getName(), RequestBody.create(MediaType.parse(BaseString.PNG_TYPE), file));
                    multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addPart(file_part).addPart(args_part).build();
                } else if(type.equals(BaseString.TEXT)){
                    file_part = MultipartBody.Part.createFormData(fileName, file.getName(), RequestBody.create(MediaType.parse(BaseString.TEXT_TYPE), file));
                    multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addPart(file_part).addPart(args_part).build();
                } else{
                    return;
                }
                Request request = new Request.Builder().url(url).post(multipartBody).build();
                Call call = okHttpClient.newCall(request);
                try {
                    Response response = call.execute();
                    Log.i(TAG, response.body().string());
                    if(callback != null)
                        callback.onResponse(response); // 调用回调的处理函数
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

    public static OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            client.sslSocketFactory(new Tls12SocketFactory());
            ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                    .build();

            List<ConnectionSpec> specs = new ArrayList<>();
            specs.add(cs);
            specs.add(ConnectionSpec.COMPATIBLE_TLS);
            specs.add(ConnectionSpec.CLEARTEXT);

            client.connectionSpecs(specs);
        }
        return client;
    }

    public static OkHttpClient getNewHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.HEADERS); // 打印请求和响应头部信息
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .addInterceptor(logging)
                .cache(null);
        return enableTls12OnPreLollipop(client).build();
    }


    public static OkHttpClient getOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        try {
            // 使用 Tls12SocketFactory，并传递一个 TrustManager
            builder.sslSocketFactory(new Tls12SocketFactory(), new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];  // 返回空数组，而不是 null
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }
            });
            Log.d("HttpHelper", "SslSocketFactory and TrustManager are set successfully.");
        } catch (Exception e) {
            Log.e("HttpHelper", "Error while setting up SSL Socket Factory", e);
        }

        try {
            OkHttpClient client = builder.build();
            Log.d("HttpHelper", "OkHttpClient built successfully.");
            return client;
        } catch (Exception e) {
            Log.e("HttpHelper", "Error while building OkHttpClient", e);
            return null;
        }
    }



}
