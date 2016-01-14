package com.blinq.authentication.impl.facebook.Actions;

import android.os.Bundle;

import com.facebook.Response;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.blinq.models.Contact;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.authentication.impl.facebook.Mappers.ProfileMapper;

import java.util.ArrayList;
import java.util.List;

public class FriendsAction extends FacebookActionImpl<List<Contact>> {


    private static final String TAG = FriendsAction.class.getSimpleName();

    public FriendsAction(FacebookAuthenticator facebookAuthenticator) {

        super(facebookAuthenticator);
    }

    @Override
    protected String getGraphPath() {
        return ProfileMapper.Properties.FRIENDS_PATH;
    }

    @Override
    protected List<Contact> processResponse(Response response) {

        List<GraphUser> graphUsers = FacebookUtils.mappedListFromResponse(
                response, GraphUser.class);

        if (graphUsers == null)
            return null;

        List<Contact> friends = new ArrayList<Contact>();

        for (GraphObject graphUser : graphUsers) {
            friends.add(ProfileMapper.create(graphUser));
        }

        return friends;
    }

    @Override
    protected Bundle getBundle() {
        Bundle bundle = FacebookUtils.buildProfileBundle();
        return bundle;
    }
}