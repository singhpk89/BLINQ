package com.blinq.server;


import com.loopj.android.http.*;

/**
 * Created by Musab on 11/5/2014.
 */
public class StaticHTTPClient {
    private static final String BASE_URL = "https://api.blinq.me/";
    private static SyncHttpClient client = new SyncHttpClient(true, 80, 443);
    private static final int NUM_OF_RETRIES = 2;
    private static final int SLEEP_BETWEEN_RETRIES_MS = 1000;

    static {
        client.setMaxRetriesAndTimeout(NUM_OF_RETRIES, SLEEP_BETWEEN_RETRIES_MS);
    }

    public static void get(String url, RequestParams params, JsonHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
