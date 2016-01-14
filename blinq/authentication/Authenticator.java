package com.blinq.authentication;

import android.content.ContentValues;
import android.content.Context;

import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.models.SearchResult;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.OnActionListener;

import java.util.List;
import java.util.Map;

/**
 * This interface holds the connection properties with 3rd party services such
 * as Google or Facebook. It is used to :
 * <p/>
 * <p>
 * <ul>
 * <li>1- Open a connection (login)
 * <li>2- Close a connection (logout)
 * <li>3- check connectivity.
 * <li>3- Check Token validity.
 * <li>4- Refresh Token.
 * <li>5- Request user profile.
 * </ul>
 * </p>
 *
 * @author Johan Hansson
 */
public interface Authenticator {

    /**
     * Connects to a 3rd party provider such as Facebook or Google
     */
    public void login(LoginCallBack loginCallBack);

    /**
     * Get User Profile Information
     *
     * @param profileRequestCallback -responds to the profile request statuses.
     */
    public void getProfile(ProfileRequestCallback profileRequestCallback);

    /**
     * Get specific user Profile Information
     *
     * @param profileRequestCallback -responds to the profile request statuses.
     */
    public void getProfile(String userId, ProfileRequestCallback profileRequestCallback);

    /**
     * Get Friends Profile Information
     *
     * @param friendsRequestCallBack -responds to the friends request statuses.
     */
    public void getFriends(FriendsRequestCallBack friendsRequestCallBack);

    /**
     * Logs out the user and destroys the session.
     */
    public void logout(LogoutCallBack logoutCallback);

    /**
     * To Search certain user given a query string.
     *
     * @param listener    - responds to the search action statuses.
     * @param queryString - user to search for.
     * @param limit       - The max results to be returned.
     */
    public Action search(OnActionListener listener, String queryString, int limit);

    /**
     * Refreshes the authentication token.
     *
     * @param context application context.
     * @return valid token.
     */
    public String refreshToken(Context context);

    /**
     * Checks whether the user is connected or not
     *
     * @return boolean - whether this object has an non-expired session token.
     */
    public boolean isConnected();

    /**
     * Checks whether the user's friends is loaded or not.
     *
     * @return boolean
     */
    public boolean isLoginCompleted();

    /**
     * Set the user's friends loaded
     *
     * @param contactsLoaded
     */
    public void setLoginCompleted(boolean contactsLoaded);

    /**
     * Gets the string representing the access token.
     *
     * @return the string representing the access token.
     */
    public String getAccessToken(Context context);

    public String getAccountName(Context context);


    /**
     * On login callback listener.
     */
    public interface LoginCallBack {

        void onLoggedIn();

        void onCancel();

        void onException(String msg, Exception e);

        void doWhileLogin();

    }

    /**
     * On logout callback listener.
     */
    public interface LogoutCallBack {

        void onLoggedOut();

        void onException(String msg, Exception e);

        void doWhileLogout();

    }

    /**
     * On request profile listener.
     */
    public interface ProfileRequestCallback {

        void onException(String msg, Exception e);

        void onFail();

        void onGettingProfile();

        void onComplete(Contact contact);

    }

    /**
     * On request Message listener.
     */
    public interface MessagesRequestCallback {

        void onException(String msg, Exception e);

        void onFail(String msg);

        void onGettingMessages();

        void onComplete(Map<String, ContentValues[]> messages);

    }

    public interface OnReopenSessionListener {

        void onSuccess();

        void onNotAcceptingPermissions();
    }

    public interface FriendsRequestCallBack {

        void onGettingFriends();

        void onException(String msg, Exception e);

        void onFail(String msg);

        void onComplete(List<Contact> friends);
    }

    public interface UsersRequestCallBack {

        void onGettingUsers();

        void onException(String msg, Exception e);

        void onFail(String msg);

        void onComplete(List<SearchResult> users);
    }

    public interface PublishRequestCallBack {

        void setPlatform(Platform platform);

        void whilePublishing();

        void onException(Exception e);

        void onFail(String msg);

        void onComplete(String postId);
    }

    public interface PostsRequestCallback {

        void onGettingPosts();

        void onException(String msg, Exception e);

        void onFail(String msg);

        void onComplete(SocialWindowPost post);

        void onComplete(List<SocialWindowPost> posts);
    }

    void getPosts(PostsRequestCallback postsRequestCallBack,
                  String userId);

}