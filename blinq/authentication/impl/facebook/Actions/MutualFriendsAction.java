package com.blinq.authentication.impl.facebook.Actions;

import android.os.Bundle;
import android.util.Log;

import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.authentication.impl.facebook.Mappers.PostsMapper;
import com.blinq.authentication.impl.facebook.Mappers.ProfileMapper;
import com.blinq.models.social.window.MeCard.MutualFriend;
import com.facebook.Response;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roi on 1/8/15.
 */
public class MutualFriendsAction extends FacebookActionImpl<List<MutualFriend>> {

    private final static String TAG = MutualFriendsAction.class.getSimpleName();

    private final String friendName;

    public MutualFriendsAction(FacebookAuthenticator facebookAuthenticator, String name) {
        super(facebookAuthenticator);
        this.friendName = name;
    }

    @Override
    protected String getGraphPath() {
        return PostsMapper.Properties.MUTUAL_FRIENDS_PATH + friendName;
    }

    @Override
    protected List<MutualFriend> processResponse(Response response) {
        List<GraphUser> graphUsers = FacebookUtils.mappedListFromResponse(
                response, GraphUser.class);

        if (graphUsers == null)
            return null;

        List<MutualFriend> friends = new ArrayList<MutualFriend>();

        for (GraphObject graphUser : graphUsers) {
            String id = (FacebookUtils.getPropertyString(graphUser,
                    ProfileMapper.Properties.ID));
            String photoUrl = (getPhotoUrl(graphUser));
            friends.add(new MutualFriend(id, photoUrl));
        }
        return friends;
    }

    private String getPhotoUrl(GraphObject graphUser) {
        GraphObject o = graphUser.getPropertyAs(PostsMapper.Properties.PICTURE, GraphObject.class);
        try {
            return o.getInnerJSONObject().getJSONObject("data").getString("url");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return StringUtils.EMPTY;
    }

    @Override
    protected Bundle getBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(LIMIT_FIELD, String.valueOf(getLimit()));
        bundle.putString(PostsMapper.Properties.FIELDS, PostsMapper.Properties.PICTURE + ".type(large)");
        return bundle;
    }
}
