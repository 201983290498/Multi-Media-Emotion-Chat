package com.wish.videopath.util;

import okhttp3.Response;

public interface HttpResponseCallback {
    void onResponse(Response response);
}
