package com.blinq.authentication.impl.Instagram;

import android.app.Activity;
import android.content.Context;

import com.blinq.HeadboxAccountsManager;
import com.blinq.HeadboxAccountsManager.AccountType;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.Instagram.Actions.PostsAction;
import com.blinq.authentication.impl.Instagram.Actions.SearchUserAction;
import com.blinq.authentication.impl.Instagram.Mappers.ContactsMapper;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.settings.InstagramSettings;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.utils.Constants;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.crashlytics.android.Crashlytics;

import net.londatiga.android.instagram.Instagram;
import net.londatiga.android.instagram.Instagram.InstagramAuthListener;
import net.londatiga.android.instagram.InstagramRequest;
import net.londatiga.android.instagram.InstagramSession;
import net.londatiga.android.instagram.InstagramUser;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InstagramAuthenticator implements Authenticator {

    protected static final String TAG = InstagramAuthenticator.class.getSimpleName();
    private static InstagramAuthenticator instance;
    private static Activity activity;
    private static Context context;
    private static LoginCallBack instagramLoginCallBack;
    private static InstagramSession instagramSession;
    private static Instagram instagram;
    private static HeadboxAccountsManager headboxAccountsManager;
    private String getMethod = "GET";
    private static PreferencesManager preferencesManager;

    /**
     * Private constructor, applying singleton design pattern's rules.
     */
    private InstagramAuthenticator() {

    }

    /**
     * Returns Instagram Authenticator object.
     */
    public static InstagramAuthenticator getInstance(Activity activity) {
        if (instance == null) {
            instance = new InstagramAuthenticator();
        }
        InstagramAuthenticator.activity = activity;
        InstagramAuthenticator.context = activity.getApplicationContext();
        initInstagram();
        preferencesManager = new PreferencesManager(context);
        headboxAccountsManager = HeadboxAccountsManager.getInstance();


        return instance;
    }

    public static InstagramAuthenticator getInstance(Context activity) {
        if (instance == null) {
            instance = new InstagramAuthenticator();
        }
        InstagramAuthenticator.context = activity.getApplicationContext();
        if (activity instanceof Activity) {
            InstagramAuthenticator.activity = (Activity) activity;
        }
        initInstagram();
        preferencesManager = new PreferencesManager(context);
        headboxAccountsManager = HeadboxAccountsManager.getInstance();

        return instance;
    }


    private static void initInstagram() {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                instagram = new Instagram(activity,
                        InstagramSettings.INSTAGRAM_CLIENT_ID,
                        InstagramSettings.INSTAGRAM_CLIENT_SECRET,
                        InstagramSettings.INSTAGRAM_REDIRECT_URI);
                instagramSession = instagram.getSession();

            }
        });

    }

    public static Instagram getInstagram() {
        return instagram;
    }

    @Override
    public void login(LoginCallBack loginCallBack) {

        instagramLoginCallBack = loginCallBack;

        if (!isConnected()) {
            instagram.authorize(instagramAuthListener);

        } else {
            if (loginCallBack != null) {
                loginCallBack.onLoggedIn();
            }
        }

    }

    /**
     * Get the profile of authenticated Instagram user.
     */

    @Override
    public void getProfile(final ProfileRequestCallback profileRequestCallback) {
        if (isConnected()) {
            getProfile(instagramSession.getUser().id,
                    profileRequestCallback);
        } else {

            if (profileRequestCallback != null) {
                profileRequestCallback.onFail();
            }
        }

    }

    /**
     * Get Instagram user's Profile Information
     *
     * @param instagramUserId
     * @param profileRequestCallback
     */
    @Override
    public void getProfile(final String instagramUserId,
                           final ProfileRequestCallback profileRequestCallback) {

        if (isConnected()) {
            new Thread(new Runnable() {

                @Override
                public void run() {

                    try {

                        Contact profile = getInstagramUserProfile(instagramUserId);
                        if (profileRequestCallback != null) {
                            profileRequestCallback
                                    .onComplete(profile);
                        }
                    } catch (JSONException e) {

                        if (profileRequestCallback != null) {
                            profileRequestCallback.onException("Bad Response: "
                                    + e.getMessage(), e);
                        }
                    } catch (Exception e) {

                        if (profileRequestCallback != null) {
                            profileRequestCallback.onException(e.getMessage(),
                                    e);
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

    /**
     * Get profile information for instagram user.
     *
     * @param instagramUserId
     * @return
     * @throws JSONException
     * @throws Exception
     */
    private Contact getInstagramUserProfile(String instagramUserId)
            throws JSONException, Exception {

        InstagramRequest request = new InstagramRequest(
                instagramSession.getAccessToken());

        String userProfileUrl = InstagramSettings.INSTAGRAM_USERS_ENDPOINT
                + instagramUserId;
        String userProfileResponse = request.createRequest(getMethod,
                userProfileUrl, new ArrayList<NameValuePair>());

        JSONObject jsonResponse = new JSONObject(userProfileResponse);

        return ContactsMapper.convert(jsonResponse.getJSONObject(InstagramSettings.INSTAGRAM_DATA));

    }

    /**
     * Get the Instagram authenticated user's friends.
     */
    @Override
    public void getFriends(final FriendsRequestCallBack friendsRequestCallBack) {

        if (isConnected()) {
            preferencesManager.setInstagramContactsSyncStatus(Constants.SYNC_STARTED);
            new Thread(new Runnable() {

                @Override
                public void run() {

                    long startTime = System.currentTimeMillis();
                    try {
                        List<Contact> friends = new ArrayList<Contact>();
                        String instagramFollowingUrl = InstagramSettings.INSTAGRAM_USERS_ENDPOINT
                                + instagramSession.getUser().id
                                + InstagramSettings.INSTAGRAM_FOLLOW_ENDPOINT;
                        String instagramFollowerUrl = InstagramSettings.INSTAGRAM_USERS_ENDPOINT
                                + instagramSession.getUser().id
                                + InstagramSettings.INSTAGRAM_FOLLOW_BY_ENDPOINT;

                        friends
                                .addAll(getInstagramFriends(instagramFollowingUrl));
                        friends
                                .addAll(getInstagramFriends(instagramFollowerUrl));

                        if (friendsRequestCallBack != null) {
                            setLoginCompleted(true);
                            friendsRequestCallBack
                                    .onComplete(friends);
                        }
                    } catch (JSONException e) {
                        Crashlytics.logException(e);
                        setLoginCompleted(false);
                        if (friendsRequestCallBack != null) {
                            friendsRequestCallBack.onException("Bad Response: "
                                    + e.getMessage(), e);
                        }
                    } catch (Exception e) {
                        Crashlytics.logException(e);
                        setLoginCompleted(false);
                        if (friendsRequestCallBack != null) {
                            friendsRequestCallBack.onException(e.getMessage(),
                                    e);
                        }

                    }

                    long endTime = System.currentTimeMillis();

                    long timeTaken = (endTime - startTime);
                    Log.i(TAG, "Instagram: time taken to load Instagram contacts = " +
                            timeTaken
                            + " milliseconds");

                }
            }).start();

            // callback while getting friends.
            if (friendsRequestCallBack != null) {
                friendsRequestCallBack.onGettingFriends();
            }
        } else {
            setLoginCompleted(false);
            String message = activity.getString(R.string.you_are_not_logged_in);
            if (friendsRequestCallBack != null) {
                friendsRequestCallBack.onFail(message);
            }
        }
    }

    /**
     * TODO:Remove this method.
     */
    @Override
    public void getPosts(
            final PostsRequestCallback postsRequestCallBack,
            final String instagramUserId) {
    }

    /**
     * To Retrieve the status updates posted by a given user .
     *
     * @param listener - Responsible to handle the request.
     * @param userId   - User to get posts for.
     * @param limit    - number of posts to be returned
     */
    public List<SocialWindowPost> getPosts(final OnActionListener listener, String userId, int limit) {

        PostsAction action = new PostsAction(this);
        action.setEntity(userId);
        action.setRequestType(Action.RequestType.SYNC);
        action.setLimit(limit);
        action.setEndPoint(InstagramSettings.INSTAGRAM_USERS_ENDPOINT);
        action.execute();
        List<SocialWindowPost> statuses = action.getResult();
        return statuses;
    }

    /**
     * Persist signed in account.
     *
     * @param accountName - Instagram account name.
     */
    public void saveLoggedInAccount(String accountName) {

        String accessToken = getAccessToken(activity);
        headboxAccountsManager.addAccount(AccountType.INSTAGRAM, accountName, accessToken);

    }

    @Override
    public void logout(LogoutCallBack logoutCallback) {

        instagramSession.reset();

    }

    @Override
    public Action search(OnActionListener listener, String queryString, int limit) {
        SearchUserAction action = new SearchUserAction(this);
        action.setActionListener(listener);
        action.setQueryString(queryString);
        action.setLimit(limit);
        action.setRequestType(Action.RequestType.ASYNC);
        return action;
    }

    @Override
    public String refreshToken(Context context) {

        return null;
    }

    @Override
    public boolean isConnected() {

        return instagramSession.isActive();

    }

    @Override
    public String getAccessToken(Context context) {

        return instagramSession.getAccessToken();

    }

    @Override
    public String getAccountName(Context context) {
        return null;
    }

    /**
     * Get the Instagram friends.
     *
     * @param instagramRequestUrl - (following,follower) URL
     * @throws JSONException
     * @throws Exception
     */
    private List<Contact> getInstagramFriends(String instagramRequestUrl)
            throws JSONException, Exception {

        List<Contact> instagramFriendsContacts = new ArrayList<Contact>();
        List<NameValuePair> instagramFriendsRequestParams = new ArrayList<NameValuePair>();
        InstagramRequest instagramRequest = new InstagramRequest(
                instagramSession.getAccessToken());
        String instagramFriendsResponse = "";
        boolean moreFriends = false;
        do {

            instagramFriendsResponse = instagramRequest.createRequest(
                    getMethod, instagramRequestUrl,
                    instagramFriendsRequestParams);

            moreFriends = false;
            instagramFriendsRequestParams = new ArrayList<NameValuePair>();

            if (!instagramFriendsResponse.equals(StringUtils.EMPTY_STRING)) {
                JSONObject friendsResponseJSONObject = new JSONObject(
                        instagramFriendsResponse);

                JSONArray instagramFriends = friendsResponseJSONObject
                        .getJSONArray(InstagramSettings.INSTAGRAM_DATA);

                if (instagramFriends != null) {
                    for (int friendIndex = 0; friendIndex < instagramFriends
                            .length(); friendIndex++) {
                        Contact profile = ContactsMapper
                                .convert(instagramFriends
                                        .getJSONObject(friendIndex));
                        instagramFriendsContacts.add(profile);

                    }

                }

                if (!friendsResponseJSONObject
                        .isNull(InstagramSettings.INSTAGRAM_PAGINATION)) {
                    // checking for more friends
                    JSONObject pagination = friendsResponseJSONObject
                            .getJSONObject(InstagramSettings.INSTAGRAM_PAGINATION);

                    if (!pagination
                            .isNull(InstagramSettings.INSTAGRAM_NEXT_CURSOR)) {
                        String nextCursor = pagination
                                .getString(InstagramSettings.INSTAGRAM_NEXT_CURSOR);
                        if (!nextCursor.isEmpty()) {
                            moreFriends = true;
                            instagramFriendsRequestParams = new ArrayList<NameValuePair>();
                            instagramFriendsRequestParams
                                    .add(new BasicNameValuePair(
                                            InstagramSettings.INSTAGRAM_CURSOR,
                                            nextCursor));
                        }
                    }
                }
                instagramFriendsResponse = "";
            }

        } while (moreFriends);
        return instagramFriendsContacts;
    }

    /**
     * Listener to Instagram authentication.
     */
    public InstagramAuthListener instagramAuthListener = new Instagram.InstagramAuthListener() {
        @Override
        public void onSuccess(InstagramUser user) {

            if (instagramLoginCallBack != null) {
                instagramLoginCallBack.onLoggedIn();
            }
            saveLoggedInAccount(user.username);
        }

        @Override
        public void onError(String error) {

            if (instagramLoginCallBack != null) {
                instagramLoginCallBack.onException(error, new Exception(error));
            }

        }

        @Override
        public void onCancel() {

            if (instagramLoginCallBack != null) {
                instagramLoginCallBack.onCancel();
            }
        }
    };

    @Override
    public boolean isLoginCompleted() {

        return preferencesManager.isContactsLoaded(Platform.INSTAGRAM);
    }

    @Override
    public void setLoginCompleted(boolean contactsLoaded) {

        preferencesManager.setContactsLoaded(Platform.INSTAGRAM, contactsLoaded);
    }

    public String getInstagramId() {
        return instagramSession.getUser().id;
    }

}
