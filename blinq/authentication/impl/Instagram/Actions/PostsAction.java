package com.blinq.authentication.impl.Instagram.Actions;

import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Instagram.Mappers.PostsMapper;
import com.blinq.authentication.settings.InstagramSettings;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Johan Hansson on 9/25/2014.
 */
public class PostsAction extends InstagramActionImpl<List<SocialWindowPost>, JSONObject> {


    private static final String TAG = PostsAction.class.getSimpleName();

    public PostsAction(Authenticator authenticator) {
        super(authenticator);
    }

    @Override
    protected List<SocialWindowPost> processResponse(JSONObject response) throws JSONException {

        if (response == null)
            return null;

        JSONArray postsArray = response.getJSONArray(InstagramSettings.INSTAGRAM_DATA);

        if (postsArray == null)
            return null;

        List<SocialWindowPost> posts = new ArrayList<SocialWindowPost>();

        for (int index = 0; index < postsArray.length(); index++) {

            SocialWindowPost post = PostsMapper.create(postsArray.getJSONObject(index));

            if (post != null) {
                posts.add(post);
            }
        }

        return posts;
    }

    @Override
    protected String getUrl() {
        return getEndPoint()
                + getEntity()
                + InstagramSettings.INSTAGRAM_MEDIA_ENDPOINT;
    }

    @Override
    protected JSONObject getResponse() throws Exception {
        return new JSONObject(request.createRequest(RequestMethod.GET.name(),
                getUrl(), requestParameters));
    }

    @Override
    protected void initializeRequest() {

        super.initializeRequest();

        BasicNameValuePair countPair
                = new BasicNameValuePair(InstagramSettings.INSTAGRAM_COUNT,
                String.valueOf(getLimit()));
        requestParameters = new ArrayList<NameValuePair>();
        requestParameters.add(countPair);
    }

    @Override
    protected void updateCursor(JSONObject response) throws JSONException {

        super.updateCursor(response);

        if (response.isNull(InstagramSettings.INSTAGRAM_PAGINATION)) {
            page.setNextPage(0);
            return;
        }
        JSONObject pagination = response.getJSONObject(InstagramSettings.INSTAGRAM_PAGINATION);

        // check for more pages
        if (pagination.isNull(InstagramSettings.INSTAGRAM_NEXT_MAX_ID)) {
            page.setNextPage(0);
            return;
        }

        String nextMaxId = pagination.getString(InstagramSettings.INSTAGRAM_NEXT_MAX_ID);
        if (nextMaxId.isEmpty()) {
            page.setNextPage(0);
            return;
        }

        requestParameters = new ArrayList<NameValuePair>();
        requestParameters.add(new BasicNameValuePair(InstagramSettings.INSTAGRAM_MAX_ID, nextMaxId));
        BasicNameValuePair countParameter =
                new BasicNameValuePair(InstagramSettings.INSTAGRAM_COUNT, String.valueOf(getLimit()));
        requestParameters.add(countParameter);

    }

}
