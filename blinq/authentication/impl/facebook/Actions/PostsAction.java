package com.blinq.authentication.impl.facebook.Actions;

import android.os.Bundle;

import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookStatusType;
import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.authentication.impl.facebook.Mappers.PostsMapper;
import com.blinq.models.social.window.FacebookPost;
import com.blinq.models.social.window.PostTypeTag;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.utils.Log;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible to request data from facebook posts path and handle the returned response.
 *
 * @author Johan Hansson.
 */
public class PostsAction extends FacebookActionImpl<List<SocialWindowPost>> {


    private static final String TAG = PostsAction.class.getSimpleName();

    public PostsAction(FacebookAuthenticator facebookAuthenticator) {

        super(facebookAuthenticator);
    }

    @Override
    protected Bundle getBundle() {

        Bundle bundle = FacebookUtils.buildPostsBundle();
        if (getLimit() > 0)
            bundle.putString(LIMIT_FIELD, String.valueOf(getLimit()));
        return bundle;

    }

    @Override
    protected String getGraphPath() {
        return getTarget() + "/" + PostsMapper.Properties.POSTS_PATH;
    }

    @Override
    protected List<SocialWindowPost> processResponse(Response response) {

        List<GraphUser> graphPosts = FacebookUtils.mappedListFromResponse(
                response, GraphUser.class);

        List<SocialWindowPost> posts = new ArrayList<SocialWindowPost>();

        for (GraphObject graphPost : graphPosts) {
            FacebookPost post = PostsMapper.create(graphPost, getEntityName());
            if (isPostValid(post)) {
                posts.add(post);
            }
        }

        return posts;
    }

    private static List<FacebookStatusType> typesToExclude =
            Arrays.asList(
                    FacebookStatusType.APPROVED_FRIEND,
                    FacebookStatusType.WALL_POST,
                    FacebookStatusType.APP_CREATED_STORY,
                    FacebookStatusType.CREATED_NOTE,
                    FacebookStatusType.PUBLISHED_STORY);

    private static List<PostTypeTag> tagsToExclude =
            Arrays.asList(
                    PostTypeTag.COMMENTED_ON,
                    PostTypeTag.LIKES_A,
                    PostTypeTag.POSTED_ON_OTHER,
                    PostTypeTag.SELF_TAG);
    /**
     * Check to see if we should filter post
     * True if we should filter out. False otherwise
     */
    private static boolean isPostValid(FacebookPost post) {

        for (FacebookStatusType type : typesToExclude) {
            if (post.getStatusType() == type) {
                return false;
            }
        }
        for (PostTypeTag tag : tagsToExclude) {
            if (post.getTag() == tag) {
                return false;
            }
        }
        return true;
    }

    /**
     * Uses the object id to make another call and get the high res image for that object.
     *
     * @param objectId                    - represents a photo or a cover id.
     * @param position                    - represents the post position on the social window list.
     * @param onHighResolutionImageLoaded - callback responds to the high resolution image loader.
     */
    public static void setHighResolutionImageUrl(final String objectId, final int position, final OnHighResolutionImageLoaded onHighResolutionImageLoaded) {

        if (StringUtils.isBlank(objectId))
            return;

        Bundle bundle = new Bundle();
        bundle.putString(PostsMapper.Properties.FIELDS, PostsMapper.Properties.IMAGES);

        /**
         *  Calling async means we will modify this object after it was returned to ui - careful
         */
        Request request = new Request(Session.getActiveSession(), objectId,
                bundle, HttpMethod.GET, new Request.Callback() {

            @Override
            public void onCompleted(Response response) {

                FacebookRequestError error = response.getError();
                if (error == null) {
                    JSONObject imagesArray = response.getGraphObject().getInnerJSONObject();
                    String highResImageUrl = FacebookUtils.getHighResImageFromImages(imagesArray);
                    if (highResImageUrl != null) {

                        onHighResolutionImageLoaded.onHighResolutionImageLoaded(highResImageUrl, position);
                    }
                } else {
                    Log.d(TAG, "Error while loading image for post " + objectId + " : " + error.getErrorMessage());
                }
            }
        });

        RequestAsyncTask task = new RequestAsyncTask(request);
        task.execute();
    }

    /**
     * Callback to notify social fragment when post high resolution image loaded.
     */
    public interface OnHighResolutionImageLoaded {

        /**
         * Called when high resolution image of the post is loaded.
         *
         * @param highResImageUrl url for post high resolution image.
         * @param position        post position in the list.
         */
        void onHighResolutionImageLoaded(String highResImageUrl, int position);
    }
}