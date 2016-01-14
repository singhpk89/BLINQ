package com.blinq.authentication.impl.facebook.Actions;

import android.os.Bundle;
import android.util.Log;

import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.facebook.Mappers.PostsMapper;
import com.blinq.authentication.impl.facebook.Mappers.ProfileMapper;
import com.facebook.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LikesAction extends FacebookActionImpl<String> {


    private static final String TAG = LikesAction.class.getSimpleName();

    public LikesAction(FacebookAuthenticator facebookAuthenticator) {

        super(facebookAuthenticator);
    }

    @Override
    protected String getGraphPath() {
        return PostsMapper.Properties.MY_POSTS;
    }

    @Override
    protected String processResponse(Response response) {

        int count = 0;

        if (response == null || response.getGraphObject() == null) {
            Log.d(TAG, "response is null");
            return Integer.toString(count);
        }

        try {
            JSONObject result = response.getGraphObject().getInnerJSONObject();
            JSONArray resultArray = result.getJSONArray(PostsMapper.Properties.DATA);
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject o = resultArray.getJSONObject(i);
                if (!o.has(PostsMapper.Properties.LIKES)) {
                    continue;
                }
                JSONObject likesJson = o.getJSONObject(PostsMapper.Properties.LIKES);
                JSONArray likesArray = likesJson.getJSONArray(PostsMapper.Properties.DATA);
                for (int j = 0; j < likesArray.length(); j++) {
                    JSONObject like = likesArray.getJSONObject(j);
                    if (like.get(ProfileMapper.Properties.ID).equals(getEntity())) {
                        count++;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed parsing JSON response: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed getting response: " + e.getMessage());
        }
        return Integer.toString(count);
    }

    @Override
    protected Bundle getBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(LIMIT_FIELD, Integer.toString(getLimit()));
        bundle.putString(PostsMapper.Properties.FIELDS, "likes");
        return bundle;
    }
}