package com.blinq.authentication.impl.facebook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.blinq.HeadboxAccountsManager;
import com.blinq.HeadboxAccountsManager.AccountType;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.impl.facebook.Actions.EventsAction;
import com.blinq.authentication.impl.facebook.Actions.LikesAction;
import com.blinq.authentication.impl.facebook.Actions.MutualFriendsAction;
import com.blinq.authentication.impl.facebook.Actions.PostsAction;
import com.blinq.authentication.impl.facebook.Actions.SearchUserAction;
import com.blinq.authentication.impl.facebook.Mappers.FeedMapper;
import com.blinq.authentication.impl.facebook.Mappers.FeedMapper.Builder.Parameters;
import com.blinq.authentication.impl.facebook.Mappers.PostsMapper;
import com.blinq.authentication.impl.facebook.Mappers.ProfileMapper;
import com.blinq.authentication.settings.FacebookSettings;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.models.social.window.MeCard;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Log;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.FacebookDialog.Callback;
import com.facebook.widget.FacebookDialog.PendingCall;
import com.facebook.widget.FacebookDialog.ShareDialogFeature;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johan Hansson
 */
public final class FacebookAuthenticator implements Authenticator {

    private static FacebookAuthenticator instance = null;
    private static Activity mActivity;
    private static Fragment mFragment;
    // 0: If activity, 1: If fragment.
    private static int loginMode = 0;
    private static SessionStatusCallback sessionStatusCallback = null;
    private Callback mFacebookDialogCallback;
    private long startLoadingTime, startTimeForLoadMessages;
    private static PreferencesManager preferencesManager;
    private static UiLifecycleHelper uiLifecycleHelper;

    private static FacebookSettings settings = new FacebookSettings();
    private static Context context;
    private static final String TAG = FacebookAuthenticator.class
            .getSimpleName();

    private static String SIGNED_IN_USER = "ME";

    private String facebookId;
    private String facebookName;

    /**
     * private constructor, applying singleton design pattern's rules.
     */
    private FacebookAuthenticator() {
        sessionStatusCallback = new SessionStatusCallback();
    }

    /**
     * Returns facebook Authenticator object.
     */
    public static FacebookAuthenticator getInstance(Activity activity) {

        if (instance == null) {
            instance = new FacebookAuthenticator();
        }
        FacebookAuthenticator.mActivity = activity;
        FacebookAuthenticator.context = activity.getApplicationContext();
        uiLifecycleHelper = new UiLifecycleHelper(mActivity,
                sessionStatusCallback);
        preferencesManager = new PreferencesManager(context);
        return instance;
    }

    public static FacebookAuthenticator getInstance(Activity activity,
                                                    Fragment fragment) {
        if (instance == null) {
            instance = new FacebookAuthenticator();
        }
        loginMode = 1;
        FacebookAuthenticator.mFragment = fragment;
        uiLifecycleHelper = new UiLifecycleHelper(mActivity,
                sessionStatusCallback);
        FacebookAuthenticator.context = activity;
        preferencesManager = new PreferencesManager(context);
        return instance;
    }

    /**
     * @param context the Activity or Service starting the Session with facebook
     * @return single instance
     */
    public static FacebookAuthenticator getInstance(Context context) {
        if (instance == null) {
            instance = new FacebookAuthenticator();
        }
        FacebookAuthenticator.context = context.getApplicationContext();
        preferencesManager = new PreferencesManager(context);
        return instance;
    }

    /**
     * Set Facebook settings object.
     */
    public static void setSettings(FacebookSettings settings) {
        FacebookAuthenticator.settings = settings;
    }

    @Override
    public String getAccountName(Context context) {
        return null;
    }

    /**
     * Login to Facebook.
     */
    public void login(LoginCallBack loginCallBack) {

        if (isConnected()) {
            if (loginCallBack != null) {
                loginCallBack.onLoggedIn();
            }
        } else {
            Session session = Session.getActiveSession();
            if (session == null || session.getState().isClosed()) {
                session = new Session.Builder(context).setApplicationId(
                        settings.getAppId()).build();
                Session.setActiveSession(session);
            }

            sessionStatusCallback.loginCallBack = loginCallBack;
            session.addCallback(sessionStatusCallback);

			/*
             * Open the session if is not opened.
			 */
            if (!session.isOpened()) {
                openSession(session, true);
            } else {
                if (loginCallBack != null) {
                    loginCallBack.onLoggedIn();
                }
            }
        }
    }

    /**
     * Logout from facebook.
     *
     * @param logoutCallback - logout callback object.
     */
    public void logout(LogoutCallBack logoutCallback) {

        if (isConnected()) {
            Session session = Session.getActiveSession();
            if (session != null && !session.isClosed()) {
                sessionStatusCallback.logoutCallBack = logoutCallback;
                session.closeAndClearTokenInformation();
                session.removeCallback(sessionStatusCallback);

                if (logoutCallback != null) {
                    logoutCallback.onLoggedOut();
                }
            }
        } else {
            // if already logged out.
            if (logoutCallback != null) {
                logoutCallback.onLoggedOut();
            }
        }
    }

    /**
     * Refreshes Facebook access token.
     */
    @Override
    public String refreshToken(Context context) {

        return null;
    }

    @Override
    public boolean isLoginCompleted() {

        return preferencesManager.isContactsLoaded(Platform.FACEBOOK);
    }

    @Override
    public void setLoginCompleted(boolean contactsLoaded) {

        preferencesManager.setContactsLoaded(Platform.FACEBOOK, contactsLoaded);
    }

    /**
     * Restores the saved session from a Bundle, if any. Returns the restored
     * Session or creates a new one if it could not be restored and then Logs a
     * user to Facebook.
     *
     * @param context            -the Activity or Service creating the Session, must not be
     *                           null.
     * @param statusCallback     -the callback to notify for Session state changes, can be null
     * @param savedInstanceState -the bundle to restore the Session from.
     */
    public void restoreSession(Context context, StatusCallback statusCallback,
                               Bundle savedInstanceState) {

        Session session = getActiveSession();
        if (session == null) {

            if (savedInstanceState != null) {
                session = Session.restoreSession(context, null, statusCallback,
                        savedInstanceState);
            }

            if (session == null) {
                session = new Session(context);
            }

            Session.setActiveSession(session);
            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
                session.openForRead(new Session.OpenRequest((Activity) context)
                        .setCallback(statusCallback));
            }
        }

    }

    /**
     * Saves the Session object into the supplied Bundle. This method is
     * intended to be called from an Activity or Fragment's onSaveInstanceState
     * method in order to preserve Sessions across Activity life cycle events.
     *
     * @param bundle the Bundle to save the Session to
     */
    public void saveSession(Bundle bundle) {

        Session session = Session.getActiveSession();
        Session.saveSession(session, bundle);
    }

    /**
     * Returns the current active Session, or null if there is none.
     *
     * @return Session Object
     */
    public Session getActiveSession() {
        return Session.getActiveSession();
    }

    /**
     * Returns a boolean indicating whether the session is opened. a boolean
     * indicating whether the session is opened.
     */
    @Override
    public boolean isConnected() {
        return isConnected(context);
    }

    public boolean isConnected(Context context) {
        Session session = getActiveSession();

        if (session == null) {
            if (session == null) {
                session = new Session.Builder(context.getApplicationContext())
                        .setApplicationId(settings.getAppId()).build();
                Session.setActiveSession(session);
            }
        }
        if (session.isOpened()) {
            return true;
        }

        if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
            List<String> permissions = session.getPermissions();
            if (permissions.containsAll(settings.getPermissions())) {
                reOpenSession();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Re-Open the active session.
     */
    private void reOpenSession() {
        Session session = getActiveSession();
        if (session != null
                && session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
            List<String> permissions = session.getPermissions();
            if (permissions.containsAll(settings.getPermissions())) {
                openSession(session, true);
            }
        }
    }

    /**
     * Open session to logs a user in to Facebook.
     */
    private void openSession(Session session, boolean isRead) {

        Session.OpenRequest request = null;
        if (loginMode == 1) {
            request = new Session.OpenRequest(mFragment);
        } else {
            request = new Session.OpenRequest(mActivity);
        }
        if (request != null) {
            request.setDefaultAudience(settings.getDefaultAudience());
            request.setPermissions(settings.getPermissions());
            request.setLoginBehavior(settings.getLoginBehavior());

            if (isRead) {
                // Open session with read permissions
                session.openForRead(request);
            } else {
                session.openForPublish(request);
            }
        }
    }

    /**
     * Returns the access token String.
     *
     * @return the access token String, or null if there is no access token
     */
    @Override
    public String getAccessToken(Context context) {
        Session session = getActiveSession();
        if (session != null) {
            Log.d(TAG, "Facebook token : " + session.getAccessToken());
            return session.getAccessToken();
        }
        return null;
    }

    /**
     * Call this inside the activity
     */
    public boolean onActivityResult(Activity activity, int requestCode,
                                    int resultCode, Intent data) {
        if (Session.getActiveSession() != null) {
            return Session.getActiveSession().onActivityResult(activity,
                    requestCode, resultCode, data);
        } else {
            return false;
        }
    }

    /**
     * Persist signed in account.
     */
    private void saveLoggedInAccount(Contact contact) {

        String accessToken = getAccessToken(mActivity);
        HeadboxAccountsManager.getInstance().addAccount(contact.getContactId(), accessToken,
                contact.getTimeZone(), AccountType.FACEBOOK);
        facebookId = contact.getContactId();
        facebookName = contact.getName();

    }

    /**
     * Removes a StatusCallback from this Session.
     *
     * @param statusCallback - the callback
     */
    public void removeCallback(StatusCallback statusCallback) {
        getActiveSession().removeCallback(statusCallback);
    }

    /**
     * TODO: Remove this method
     * @param userId
     * @param profileRequestCallback -responds to the profile request statuses.
     */
    @Override
    public void getProfile(final String userId,
                           final ProfileRequestCallback profileRequestCallback) {

        // if we are logged in
        if (isConnected()) {
            Session session = getActiveSession();
            Bundle bundle = FacebookUtils.buildProfileBundle();
            Request request = new Request(session, userId, bundle,
                    HttpMethod.GET, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    GraphUser graphUser = response
                            .getGraphObjectAs(GraphUser.class);

                    FacebookRequestError error = response.getError();
                    if (error != null) {
                        // do when exception.
                        if (profileRequestCallback != null) {
                            profileRequestCallback.onException(
                                    "an exception happened",
                                    error.getException());
                        }
                    } else {
                        // when complete.
                        if (profileRequestCallback != null) {
                            Contact contact = ProfileMapper.create(graphUser);
                            if (userId == SIGNED_IN_USER) {
                                saveLoggedInAccount(contact);
                            }
                            profileRequestCallback.onComplete(contact);
                        }
                    }
                }
            }
            );

            RequestAsyncTask task = new RequestAsyncTask(request);
            task.execute();

            if (profileRequestCallback != null) {
                profileRequestCallback.onGettingProfile();
            }
        } else {
            // if we are not logged in.
            if (profileRequestCallback != null) {
                profileRequestCallback.onFail();
            }
        }
    }

    /**
     * Get facebook friends.
     * TODO: Remove this method.
     *
     * @param - {@link FriendsRequestCallBack}.
     */
    public void getFriends(final FriendsRequestCallBack friendsRequestCallBack) {

        if (isConnected()) {
            Session session = getActiveSession();
            Bundle bundle = FacebookUtils.buildProfileBundle();
            startLoadingTime = System.currentTimeMillis();
            Request request = new Request(session, ProfileMapper.Properties.FRIENDS_PATH, bundle,
                    HttpMethod.GET, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {

                    List<GraphUser> graphUsers = FacebookUtils.mappedListFromResponse(
                            response, GraphUser.class);

                    FacebookRequestError error = response.getError();
                    if (error != null) {
                        String message = context
                                .getString(R.string.error_while_getting_facebook_friends);

                        if (friendsRequestCallBack != null) {
                            setLoginCompleted(false);
                            friendsRequestCallBack.onException(message,
                                    error.getException());
                        }
                    } else {
                        if (friendsRequestCallBack != null) {

                            List<Contact> friends = new ArrayList<Contact>(
                                    graphUsers.size());
                            for (GraphUser graphUser : graphUsers) {
                                friends.add(ProfileMapper.create(graphUser));
                            }
                            if (friendsRequestCallBack != null) {
                                setLoginCompleted(true);

                                Log.i(TAG,
                                        "---Time taken to fetch facebook's contacts ="
                                                + AppUtils
                                                .findTime(startLoadingTime)
                                );
                                // Call back to return list of friends.
                                friendsRequestCallBack
                                        .onComplete(friends);

                            }
                        }
                    }
                }
            }
            );

            RequestAsyncTask task = new RequestAsyncTask(request);
            startLoadingTime = System.currentTimeMillis();
            task.execute();

            // callback while getting friends.
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
     * @param entityId - User or page id.
     * @param entityName - User display name - used to filter posts
     * @param limit    - number of posts to be returned
     */
    public List<SocialWindowPost> getPosts(final OnActionListener listener,
                                           String entityId,
                                           String entityName, int limit) {

        PostsAction action = new PostsAction(this);
        action.setEntity(entityId);
        action.setEntityName(entityName);
        action.setRequestType(Action.RequestType.SYNC);
        action.setLimit(limit);
        action.execute();
        List<SocialWindowPost> statuses = action.getResult();
        return statuses;
    }

    public List<SocialWindowPost> getEvents(final OnActionListener listener, String entityId, int limit) {

        EventsAction action = new EventsAction(this);
        action.setEntity(entityId);
        action.setRequestType(Action.RequestType.SYNC);
        action.setLimit(limit);
        action.execute();
        List<SocialWindowPost> events = action.getResult();
        return events;
    }

    public String getLikesCount(final String facebookId, int limit) {
        LikesAction action = new LikesAction(this);
        action.setEntity(facebookId);
        action.setRequestType(Action.RequestType.SYNC);
        action.setLimit(limit);
        action.execute();
        String result = action.getResult();
        return result;
    }

    public List<MeCard.MutualFriend> getMutualFriends(final String facebookId) {
        MutualFriendsAction action = new MutualFriendsAction(this, facebookId);
        action.setRequestType(Action.RequestType.SYNC);
        action.setLimit(50);
        action.execute();
        return action.getResult();
    }

    @Override
    public Action search(final OnActionListener listener, String queryString, int limit) {

        SearchUserAction action = new SearchUserAction(this);
        action.setActionListener(listener);
        action.setQueryString(queryString);
        action.setRequestType(Action.RequestType.ASYNC);
        action.setLimit(limit);
        return action;
    }


    /**
     * Retrieves Facebook last post of the friend .
     *
     * @param postsRequestCallBack called on response to the request.
     * @param friendId             - user id.
     */
    @Override
    public void getPosts(final PostsRequestCallback postsRequestCallBack,
                         String friendId) {
    }

    public void publishFeed(
            final PublishRequestCallBack publishRequestCallBack,
            final FeedMapper feed) {

        if (!isConnected())
            return;

        if (FacebookDialog.canPresentShareDialog(mActivity,
                ShareDialogFeature.SHARE_DIALOG)) {

            Bundle bundle = feed.getBundle();

            FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(
                    mActivity)
                    .setApplicationName(context.getString(R.string.app_name))
                    .setRef(bundle.getString(Parameters.NAME))
                    .setCaption(bundle.getString(Parameters.CAPTION))
                    .setDescription(bundle.getString(Parameters.DESCRIPTION))
                    .setName(bundle.getString(Parameters.NAME))
                    .setPicture(bundle.getString(Parameters.PICTURE))
                    .setLink(bundle.getString(Parameters.LINK)).build();
            PendingCall pendingCall = shareDialog.present();
            trackFacebookDialogPendingCall(pendingCall,
                    new FacebookDialog.Callback() {

                        @Override
                        public void onError(PendingCall pendingCall,
                                            Exception error, Bundle data) {
                            untrackPendingCall();
                            if ("".equals(error.getMessage())) {
                            }
                            shareWithWebDialog(publishRequestCallBack, feed);
                        }

                        @Override
                        public void onComplete(PendingCall pendingCall,
                                               Bundle data) {
                            untrackPendingCall();
                            boolean didComplete = FacebookDialog
                                    .getNativeDialogDidComplete(data);
                            String postId = FacebookDialog
                                    .getNativeDialogPostId(data);
                            if (didComplete && postId != null) {
                                publishRequestCallBack.onComplete(postId);
                            } else {
                                publishRequestCallBack.onFail(context
                                        .getString(R.string.share_canceled));
                            }
                        }
                    }
            );
        } else {
            shareWithWebDialog(publishRequestCallBack, feed);
        }
    }

    private void shareWithWebDialog(
            final PublishRequestCallBack publishRequestCallBack,
            FeedMapper feed) {
        WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(mActivity,
                Session.getActiveSession(), feed.getBundle()))
                .setOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(Bundle values,
                                           FacebookException error) {
                        if (error == null) {
                            final String postId = values.getString(PostsMapper.Properties.ID);
                            if (postId != null) {
                                publishRequestCallBack.onComplete(postId);
                            } else {
                                publishRequestCallBack.onFail(context
                                        .getString(R.string.share_canceled));
                            }
                        } else if (error instanceof FacebookOperationCanceledException) {
                            publishRequestCallBack.onFail(context
                                    .getString(R.string.share_canceled));
                        } else {
                            publishRequestCallBack.onException(error);
                        }
                    }

                }).build();
        feedDialog.show();
    }

    public void trackFacebookDialogPendingCall(PendingCall pendingCall,
                                               FacebookDialog.Callback callback) {
        mFacebookDialogCallback = callback;
        uiLifecycleHelper.trackPendingDialogCall(pendingCall);
    }

    public void untrackPendingCall() {
        mFacebookDialogCallback = null;
    }

    /**
     * Get user profile from facebook.
     */
    public void getProfile(final ProfileRequestCallback profileRequestCallback) {

        getProfile(SIGNED_IN_USER, profileRequestCallback);
    }

    /**
     * Responds to the Facebook session status change.
     */
    private class SessionStatusCallback implements Session.StatusCallback {

        LoginCallBack loginCallBack = null;
        LogoutCallBack logoutCallBack = null;
        OnReopenSessionListener mOnReopenSessionListener = null;
        private boolean mAskPublishPermissions = false;
        private boolean mDoOnLogin = false;

        @Override
        public void call(Session session, SessionState state,
                         Exception exception) {

            List<String> permissions = getPermissionsList(session);

            if (exception != null) {

                // If the Exception indicates that an operation was canceled
                // before it completed.
                if (exception instanceof FacebookOperationCanceledException) {
                    /*
                     * If the user canceled permission dialog.
					 */
                    if (permissions.size() == 0) {
                        if (loginCallBack != null)
                            loginCallBack.onCancel();
                    }
                } else {
                    if (loginCallBack != null)
                        loginCallBack.onException(exception.getMessage(),
                                exception);
                }
            }
            switch (state) {
                case CLOSED:
                    if (logoutCallBack != null)
                        logoutCallBack.onLoggedOut();
                    break;
                case CLOSED_LOGIN_FAILED:
                    break;

                case CREATED:
                    break;

                case CREATED_TOKEN_LOADED:
                    break;

                case OPENING:
                    if (loginCallBack != null)
                        loginCallBack.doWhileLogin();
                    break;

                case OPENED:

				/*
                 * Check if we came from publishing actions where we ask again
				 * for publish permissions
				 */
                    if (mOnReopenSessionListener != null) {
                        mOnReopenSessionListener.onNotAcceptingPermissions();
                        mOnReopenSessionListener = null;
                    }

				/*
                 * Check if WRITE permissions were also defined in the
				 * configuration. If so, then ask in another dialog for WRITE
				 * permissions.
				 */
                    else if (mAskPublishPermissions
                            && session.getState().equals(SessionState.OPENED)) {
                        if (mDoOnLogin) {
                        /*
                         * If user didn't accept the publish permissions, we
						 * still want to notify about complete
						 */
                            mDoOnLogin = false;
                            if (loginCallBack != null) {
                                loginCallBack.onLoggedIn();
                            }
                        } else {

                            mDoOnLogin = true;
                            mAskPublishPermissions = false;
                        }
                    } else {
                        if (loginCallBack != null) {
                            loginCallBack.onLoggedIn();
                        }

                    }
                    break;

                case OPENED_TOKEN_UPDATED:

				/*
                 * Check if came from publishing actions and we need to ask for
				 * publish permissions
				 */
                    if (mOnReopenSessionListener != null) {
                        mOnReopenSessionListener.onSuccess();
                        mOnReopenSessionListener = null;
                    } else if (mDoOnLogin) {
                        mDoOnLogin = false;
                        if (loginCallBack != null) {
                            loginCallBack.onLoggedIn();
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        public void askPublishPermissions() {
            mAskPublishPermissions = true;
        }

        /**
         * Returns the list of permissions associated with the session.
         *
         * @param session - active session object.
         */
        private List<String> getPermissionsList(Session session) {
            return session.getPermissions();
        }

    }

    public String getFacebookId() {
        return facebookId;
    }

    public String getFacebookName() {
        return facebookName;
    }

}