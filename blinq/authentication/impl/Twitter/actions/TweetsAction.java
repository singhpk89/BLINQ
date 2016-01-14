package com.blinq.authentication.impl.Twitter.actions;

import com.blinq.authentication.impl.Twitter.Mappers.TweetsMapper;
import com.blinq.authentication.impl.Twitter.TwitterAuthenticator;
import com.blinq.models.social.window.SocialWindowPost;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

/**
 * Works as a handler for the response of the twitter api for the user-tweets request.
 * Created by Exalt Technology on 9/18/2014.
 *
 * @author Osama
 */
public class TweetsAction extends TwitterActionImpl<List<SocialWindowPost>, ResponseList<Status>> {

    private static final String TAG = TweetsAction.class.getSimpleName();

    public TweetsAction(TwitterAuthenticator authenticator) {
        super(authenticator);
    }

    @Override
    protected ResponseList getPageResponse() throws TwitterException {

        Paging paging = new Paging((int) page.getPageNum(), getResultsPerPage());
        ResponseList list = twitter.getUserTimeline(Long.parseLong(getEntity()), paging);
        return list;
    }

    @Override
    protected List<SocialWindowPost> processResponse(ResponseList<Status> object) {

        ResponseList<Status> statuses = object;
        List<SocialWindowPost> tweets = new ArrayList<SocialWindowPost>();
        for (Status status : statuses) {
            tweets.add(TweetsMapper.create(status));
        }
        return tweets;
    }

}
