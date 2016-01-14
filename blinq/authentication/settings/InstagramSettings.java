package com.blinq.authentication.settings;

public interface InstagramSettings {

    public static final String INSTAGRAM_CLIENT_ID = "a3fe3bfe676a4dae8f0286035a16fc4f";
    public static final String INSTAGRAM_CLIENT_SECRET = "4058ebd61f7c41b3af6e390d7361f44d";
    public static final String INSTAGRAM_REDIRECT_URI = "http://api.blinq.me/auth/instagram/callback";
    public static final String INSTAGRAM_DATA = "data";

    public static final String INSTAGRAM_USERS_ENDPOINT = "/users/";
    public static final String INSTAGRAM_USERS_SEARCH_ENDPOINT = "/users/search";
    public static final String INSTAGRAM_FOLLOW_ENDPOINT = "/follows";
    public static final String INSTAGRAM_FOLLOW_BY_ENDPOINT = "/followed-by";
    public static final String INSTAGRAM_MEDIA_ENDPOINT = "/media/recent";
    public static final String INSTAGRAM_PAGINATION = "pagination";
    public static final String INSTAGRAM_NEXT_CURSOR = "next_cursor";
    public static final String INSTAGRAM_COUNT = "count";
    public static final String INSTAGRAM_CURSOR = "cursor";
    public static final String INSTAGRAM_NEXT_MAX_ID = "next_max_id";
    public static final String INSTAGRAM_MAX_ID = "max_id";

    public static final String INSTAGRAM_USERS_SEARCH_QUERY_STRING = "q";


}
