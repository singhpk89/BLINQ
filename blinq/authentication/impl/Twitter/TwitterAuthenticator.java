package com.blinq.authentication.impl.Twitter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blinq.HeadboxAccountsManager;
import com.blinq.HeadboxAccountsManager.AccountType;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.impl.Twitter.Mappers.UsersMapper;
import com.blinq.authentication.impl.Twitter.actions.SearchUserAction;
import com.blinq.authentication.impl.Twitter.actions.TweetsAction;
import com.blinq.authentication.settings.TwitterSettings;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.utils.Constants;
import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

import twitter4j.PagableResponseList;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


/**
 * @author Johan Hansson
 *         TODO: This class will be only for the twitter api settings.
 *         All other implementation will be moved on a seperate units.
 */
public class TwitterAuthenticator implements Authenticator {

    protected static final String TAG = TwitterAuthenticator.class.getSimpleName();
    /*
        Maximum number of contacts get call to getFriends. We can have only 15 accesses,
        so in each access we bring the most we can
     */
    private static int USERS_PER_PAGE = 200;

    private static TwitterAuthenticator instance = null;
    private static Activity activity;
    private static Context context;
    private static PreferencesManager preferencesManager;
    private static HeadboxAccountsManager headboxAccountsManager;
    private LoginCallBack twitterLoginCallBack;

    /**
     * Private constructor, applying singleton design pattern's rules.
     */
    private TwitterAuthenticator() {

    }

    /**
     * Returns Twitter Authenticator object.
     */
    public static TwitterAuthenticator getInstance(Activity activity) {
        if (instance == null) {
            instance = new TwitterAuthenticator();
        }
        TwitterAuthenticator.activity = activity;
        TwitterAuthenticator.context = activity.getApplicationContext();
        preferencesManager = new PreferencesManager(context);
        headboxAccountsManager = HeadboxAccountsManager.getInstance();
        return instance;
    }

    /**
     * Returns Twitter Authenticator object.
     */
    public static TwitterAuthenticator getInstance(Context context) {
        if (instance == null) {
            instance = new TwitterAuthenticator();
        }
        TwitterAuthenticator.context = context.getApplicationContext();

        return instance;
    }

    /**
     * Get Twitter's User Profile Information
     *
     * @param profileRequestCallback -responds to the profile request statuses.
     */
    @Override
    public void getProfile(final String userId, final ProfileRequestCallback profileRequestCallback) {
        if (isConnected()) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    Twitter twitter = createTwitter();

                    User twitterUser = null;
                    if (twitter != null) {
                        try {
                            twitterUser = twitter.showUser(Long.valueOf(userId));

                            if (profileRequestCallback != null) {
                                Contact contact = UsersMapper.create(twitterUser);
                                profileRequestCallback.onComplete(contact);
                            }

                        } catch (TwitterException e) {

                            if (profileRequestCallback != null) {
                                profileRequestCallback.onException(
                                        e.getMessage(), e);
                            }
                        }
                    }

                }
            }).start();

            if (profileRequestCallback != null) {
                profileRequestCallback.onGettingProfile();
            }
        } else {

            if (profileRequestCallback != null) {
                profileRequestCallback.onFail();
            }
        }
    }


    @Override
    public void getProfile(ProfileRequestCallback profileRequestCallback) {

        Twitter twitter = createTwitter();
        if (twitter != null) {
            try {
                String userID = String.valueOf(twitter.verifyCredentials()
                        .getId());
                getProfile(userID, profileRequestCallback);
            } catch (TwitterException e) {
                if (profileRequestCallback != null) {
                    profileRequestCallback.onException(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void login(LoginCallBack loginCallBack) {

        if (!isConnected()) {
            this.twitterLoginCallBack = loginCallBack;
            Intent intent = new Intent(activity, TwitterLoginActivity.class);
            activity.startActivity(intent);
        } else {
            if (loginCallBack != null) {
                loginCallBack.onLoggedIn();
            }
        }
    }

    /**
     * Logs out the user from Twitter
     */
    @Override
    public void logout(LogoutCallBack logoutCallback) {
        if (isConnected()) {
            preferencesManager.setTWitterLoggedIn(false);
            if (logoutCallback != null) {
                logoutCallback.onLoggedOut();
            }

        } else {

            if (logoutCallback != null) {
                logoutCallback.onLoggedOut();
            }
        }

    }

    /**
     * Get Twitter's Friends Profile Information
     * TODO: SPRINT-20 - REMOVE THIS - build a separate FriendAction CLASS.
     *
     * @param {link FriendsRequestCallBack} -responds to the friends profile request .
     */
    @Override
    public void getFriends(final FriendsRequestCallBack friendsRequestCallBack) {

        if (isConnected()) {
            preferencesManager.setTwitterContactsSyncStatus(Constants.SYNC_STARTED);
            new Thread(new Runnable() {

                @Override
                public void run() {

                    long startTime = System.currentTimeMillis();
                    Twitter twitter = createTwitter();

                    long cursor = -1;
                    PagableResponseList<User> twitterFriends;
                    List<Contact> friends = new ArrayList<Contact>();
                    try {
                        long twitterUserId = twitter.verifyCredentials()
                                .getId();
                        do {
                            twitterFriends = twitter.getFriendsList(
                                    twitterUserId, cursor, USERS_PER_PAGE);

                            for (User twitterFriend : twitterFriends) {
                                friends.add(UsersMapper.create(twitterFriend));
                            }

                            cursor = twitterFriends.getNextCursor();

                        } while (cursor != 0);

                        if (friendsRequestCallBack != null) {
                            setLoginCompleted(true);
                            // Call back to return list of Twitter's friends.
                            friendsRequestCallBack.onComplete(friends);
                        }

                    } catch (TwitterException exception) {
                        Crashlytics.logException(exception);
                        if (friends != null && friends.size() > 0) {
                            Log.e(TAG, "load contacts exception for twitter "
                                    + exception.toString());
                            if (friendsRequestCallBack != null) {
                                setLoginCompleted(true);
                                // Call back to return list of Twitter's friends.
                                friendsRequestCallBack.onComplete(friends);
                            }
                        } else if (friendsRequestCallBack != null) {
                            setLoginCompleted(false);
                            friendsRequestCallBack.onException(exception.getMessage(),
                                    exception);
                        }
                    }

                    long endTime = System.currentTimeMillis();
                    long timeTaken = (endTime - startTime);
                    Log.i(TAG, "Twitter: time taken to load Twitter contacts = " +
                            timeTaken
                            + " milliseconds");

                }
            }).start();

            if (friendsRequestCallBack != null) {
                friendsRequestCallBack.onGettingFriends();
            }

        } else {
            setLoginCompleted(false);
            String message = context.getString(R.string.you_are_not_logged_in);
            if (friendsRequestCallBack != null) {
                friendsRequestCallBack.onFail(message);
            }
        }
    }

    /**
     * To Retrieve the status updates posted by a given page or user .
     *
     * @param listener - Responsible to handle the request.
     * @param entityId - User or Page id.
     * @param maxPages - Number of pages to be returned.
     * @param perPage  - Number of result to be returned per page.
     */
    public List<SocialWindowPost> getPosts(final OnActionListener listener, String entityId, int maxPages, int perPage) {

        TweetsAction action = new TweetsAction(this);
        action.setEntity(entityId);
        action.setMaxPages(maxPages);
        action.setRequestType(Action.RequestType.SYNC);
        action.setResultsPerPage(perPage);
        action.setActionListener(listener);
        action.execute();
        return action.getResult();
    }

    @Override
    public Action search(final OnActionListener listener, String queryString, int limit) {

        SearchUserAction action = new SearchUserAction(this);
        action.setActionListener(listener);
        action.setQueryString(queryString);
        action.setRequestType(Action.RequestType.ASYNC);
        return action;
    }

    /**
     * Update status on twitter.
     *
     * @param publishRequestCallBack
     * @param status
     */
    public void updateStatus(
            final PublishRequestCallBack publishRequestCallBack,
            final String status) {

        if (isConnected()) {

            new Thread(new Runnable() {

                @Override
                public void run() {

                    Twitter twitter = createTwitter();

                    try {
                        twitter.updateStatus(new StatusUpdate(status));
                    } catch (TwitterException e) {
                        if (publishRequestCallBack != null) {
                            publishRequestCallBack.onException(e);
                        }
                    }

                    if (publishRequestCallBack != null
                            && status != null) {
                        publishRequestCallBack.onComplete("");
                    }

                }
            }).start();

            if (publishRequestCallBack != null) {
                publishRequestCallBack.whilePublishing();
            }

        } else {
            String message = context.getString(R.string.you_are_not_logged_in);
            if (publishRequestCallBack != null) {
                publishRequestCallBack.onFail(message);
            }
        }
    }

    @Override
    public String refreshToken(Context context) {

        return null;
    }

    @Override
    public boolean isConnected() {
        return preferencesManager.isTWitterLoggedIn();
    }

    @Override
    public String getAccessToken(Context context) {

        return null;
    }

    @Override
    public String getAccountName(Context context) {

        return null;
    }

    @Override
    public void getPosts(PostsRequestCallback postsRequestCallBack, String userId) {
    }

    public LoginCallBack getTwitterLoginCallBack() {
        return twitterLoginCallBack;
    }

    public void setTwitterLoginCallBack(LoginCallBack loginCallBack) {
        this.twitterLoginCallBack = loginCallBack;
    }

    @Override
    public boolean isLoginCompleted() {

        return preferencesManager.isContactsLoaded(Platform.TWITTER);
    }

    @Override
    public void setLoginCompleted(boolean contactsLoaded) {

        preferencesManager.setContactsLoaded(Platform.TWITTER, contactsLoaded);
    }

    /**
     * Persist Twitter signed in account
     *
     * @param accountName              - Twitter account name.
     * @param twitterAccessToken
     * @param twitterAccessTokenSecret
     */
    public void saveLoggedInTwitterAccount(String accountName,
                                           String twitterAccessToken, String twitterAccessTokenSecret) {

        headboxAccountsManager.addAccount(AccountType.TWITTER, accountName, twitterAccessToken);
        preferencesManager.setTwitterTokens(twitterAccessToken,
                twitterAccessTokenSecret);
    }

    /**
     * Build Twitter's Configuration
     */
    private Configuration getTwitterConfiguration() {

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder
                .setOAuthConsumerKey(TwitterSettings.TWITTER_CONSUMER_KEY);
        configurationBuilder
                .setOAuthConsumerSecret(TwitterSettings.TWITTER_CONSUMER_SECRET);
        configurationBuilder.setDebugEnabled(false);

        return configurationBuilder.build();

    }

    /**
     * Create Twitter object to access twitter API.
     */
    public Twitter createTwitter() {
        TwitterFactory twitterFactory = new TwitterFactory(
                getTwitterConfiguration());
        Boolean authenticated = isConnected();

        if (!authenticated) {
            return twitterFactory.getInstance();
        } else {
            // return twitter with access token
            AccessToken twitterAccessToken = new AccessToken(
                    preferencesManager.getTwitterAccessToken(),
                    preferencesManager.getTwitterAccessTokenSecret());
            return twitterFactory.getInstance(twitterAccessToken);
        }
    }

    public String getTwitterId() {
        try {
            return Long.toString(createTwitter().verifyCredentials().getId());
        } catch (TwitterException e) {
            return "";
        }
    }
}
