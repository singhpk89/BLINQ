package com.blinq.authentication.impl.facebook.Actions;

import android.os.Bundle;

import com.facebook.Response;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.blinq.models.social.window.FacebookPost;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookUtils;
import com.blinq.authentication.impl.facebook.Mappers.EventsMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Johan Hansson on 10/15/2014.
 */
public class EventsAction extends FacebookActionImpl<List<SocialWindowPost>> {

    public EventsAction(FacebookAuthenticator facebookAuthenticator) {
        super(facebookAuthenticator);
    }

    @Override
    protected String getGraphPath() {
        return getTarget() + "/" + EventsMapper.Properties.EVENTS_PATH;
    }

    @Override
    protected Bundle getBundle() {

        Bundle bundle = FacebookUtils.buildEventsBundle();
        if (getLimit() > 0)
            bundle.putString(LIMIT_FIELD, String.valueOf(getLimit()));
        return bundle;

    }

    @Override
    protected List<SocialWindowPost> processResponse(Response response) {

        List<GraphUser> graphEvents = FacebookUtils.mappedListFromResponse(
                response, GraphUser.class);

        List<SocialWindowPost> events = new ArrayList<SocialWindowPost>();

        for (GraphObject graphPost : graphEvents) {
            FacebookPost event = EventsMapper.create(graphPost);
            events.add(event);
        }

        return events;
    }
}
