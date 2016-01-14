package com.blinq.authentication.impl.provider;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;

import com.blinq.MeCardHolder;
import com.blinq.R;
import com.blinq.authentication.impl.Instagram.InstagramAuthenticator;
import com.blinq.authentication.impl.Twitter.TwitterAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.models.Contact;
import com.blinq.models.Location;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.models.social.window.FacebookPost;
import com.blinq.models.social.window.InstagramPost;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.models.social.window.TwitterPost;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.service.notification.HeadboxNotificationManager;
import com.blinq.sorters.SocialWindowSorter;
import com.blinq.utils.ImageUtils;
import com.blinq.utils.Log;
import com.blinq.utils.ServerUtils;
import com.blinq.utils.StringUtils;
import com.crashlytics.android.Crashlytics;

import org.codeandmagic.deferredobject.DeferredObject;
import org.codeandmagic.deferredobject.ProgressCallback;
import org.codeandmagic.deferredobject.Promise;
import org.codeandmagic.deferredobject.RejectCallback;
import org.codeandmagic.deferredobject.ResolveCallback;
import org.codeandmagic.deferredobject.android.DeferredAsyncTask;
import org.codeandmagic.deferredobject.merge.MergedPromiseProgress;
import org.codeandmagic.deferredobject.merge.MergedPromiseReject;
import org.codeandmagic.deferredobject.merge.MergedPromiseResult4;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible to get, map, merge,and sort platform posts and to populate the user unified social window
 * by his social updates from different platforms.
 * This Provider will directly works with the {@link com.blinq.ui.fragments.SocialListFragment}
 * fragment.
 * <p/>
 * Created by Exalt technology on 9/18/2014.
 */
public class SocialWindowProvider {

    private static String TAG = SocialWindowProvider.class.getSimpleName();

    private static SocialWindowSorter sorter = new SocialWindowSorter();

    /**
     * Number of pages to be returned per request from the APIs.
     */
    private static final int PAGES = 1;
    /**
     * Number of results to be returned per page.
     */
    private static int POSTS_PER_PAGE = 50;
    private static int EVENTS_TO_GET = 20;

    private static NewPostsListener newPostsListener;
    private SocialWindowPostsListener socialWindowPostsListener;
    private boolean isDataReady;
    private boolean isThereSomethingNew;
    private List<SocialWindowPost> posts;

    private static SocialWindowProvider instance;

    private long feedId;
    private Context context;
    private MeCardHolder meCardHolder;
    private Map<Platform, List<MemberContact>> contacts;
    private boolean notifyNewPostsOnCompletion;

    public static Map<Long, Date> feedMostRecentPostDate = new HashMap<Long, Date>();

    private boolean meCardOnly;

    private enum FacebookPostType {POST, EVENT};

    /**
     * Steps:
     * 1-Get Facebook posts.
     * 2-Get Twitter posts.
     * 3-Get Instagram posts.
     * 4-Merge Facebook,Twitter,Instagram.[sort by date].
     * 5-Notify the {@link com.headbox.blinq.ui.fragments.SocialListFragment} when data ready.
     */

    /**
     * The targeted activity or fragment should implement this listener in order to
     * control displaying the user posts.
     *
     * @author Johan Hansson.
     */
    public interface SocialWindowPostsListener {

        public void onComplete(List<SocialWindowPost> response, boolean isNew, boolean meCardOnly);

        public void onException(Throwable throwable);

        public void onFail(String reason);

        public void onLoading();

    }

    public interface NewPostsListener {
        public void onNewPosts();
    }

    public static SocialWindowProvider getNewInstance(Context context, long feedId) {
        instance = new SocialWindowProvider(context, feedId);
        return instance;
    }

    private SocialWindowProvider(Context context, long feedId) {
        this.feedId = feedId;
        this.context = context;
        this.meCardHolder = MeCardHolder.getInstance();
    }

    public static SocialWindowProvider getInstance() { return instance; }

    public void fetchSocialPosts(boolean notifyNewPostsOnCompletion, boolean getServerInformation) {
        if (feedId != 1 || (feedId == 1 && notifyNewPostsOnCompletion)) {
            getMergedPosts(FeedProviderImpl.getInstance(context).getContacts(feedId),
                    notifyNewPostsOnCompletion, getServerInformation);
        } else {
            getWelcomeFeed();
        }
    }

    private void getWelcomeFeed() {
        new WelcomeFeedFactory().createFeed();
        isDataReady = true;
        meCardOnly = true;
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(HeadboxNotificationManager.WELCOME_NOTIFICATION_ID);
        onGetMergedPostsComplete();
    }

    /**
     * Get list of merged and sorted social updates for a given user platforms.
     *
     * @param contacts - contacts to load the social updates for.
     */
    private void getMergedPosts(final Map<Platform, List<MemberContact>> contacts,
                                final boolean notifyNewPostsOnCompletion,
                                final boolean getServerInformation) {

        if (contacts == null || contacts.isEmpty()) {
            Log.d(TAG, "no contacts");
            return;
        }
        this.posts = new ArrayList<SocialWindowPost>();
        this.contacts = contacts;
        this.notifyNewPostsOnCompletion = notifyNewPostsOnCompletion;
        if (getServerInformation) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ServerUtils.getInstance(context).getContactInformation(contacts, feedId, listener);
                }
            }).start();
        } else {
            notifyOnMeCardReady();
        }
    }

    final ServerUtils.OnGetContactInformationListener listener = new ServerUtils.OnGetContactInformationListener() {
        @Override
        public void onGetContactInformation(boolean success) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyOnMeCardReady();
                }
            });
        }
    };

    private void notifyOnMeCardReady() {
        isDataReady = true;
        meCardOnly = true;
        onGetMergedPostsComplete();
        if (feedId != 1) {
            getSocialPosts();
        }
    }


    private void getSocialPosts() {
        final long startTime = System.currentTimeMillis();
        final Promise<List<SocialWindowPost>, List<SocialWindowPost>, Void> facebookPostsAction = new DeferredAsyncTask<List<SocialWindowPost>, List<SocialWindowPost>, Void>() {
            @Override
            protected List<SocialWindowPost> doInBackground() {

                List<SocialWindowPost> facebookPosts = getFacebookPostsByType(contacts.get(Platform.FACEBOOK), FacebookPostType.POST);

                return facebookPosts == null ? new ArrayList<SocialWindowPost>() : facebookPosts;
            }

        };

        final Promise<List<SocialWindowPost>, List<SocialWindowPost>, Void> facebookEventsAction = new DeferredAsyncTask<List<SocialWindowPost>, List<SocialWindowPost>, Void>() {
            @Override
            protected List<SocialWindowPost> doInBackground() {

                List<SocialWindowPost> facebookEvents = getFacebookPostsByType(contacts.get(Platform.FACEBOOK), FacebookPostType.EVENT);

                return facebookEvents == null ? new ArrayList<SocialWindowPost>() : facebookEvents;
            }

        };

        final Promise<List<SocialWindowPost>, List<SocialWindowPost>, Void> twitterAction = new DeferredAsyncTask<List<SocialWindowPost>, List<SocialWindowPost>, Void>() {
            @Override
            protected List<SocialWindowPost> doInBackground() {

                List<SocialWindowPost> twitterPosts = getPostsByPlatform(contacts, Platform.TWITTER);

                return twitterPosts == null ? new ArrayList<SocialWindowPost>() : twitterPosts;
            }

        };

        final Promise<List<SocialWindowPost>, List<SocialWindowPost>, Void> instagramAction = new DeferredAsyncTask<List<SocialWindowPost>, List<SocialWindowPost>, Void>() {
            @Override
            protected List<SocialWindowPost> doInBackground() {

                List<SocialWindowPost> instagramPosts = getPostsByPlatform(contacts, Platform.INSTAGRAM);

                return instagramPosts == null ? new ArrayList<SocialWindowPost>() : instagramPosts;
            }

        };

        //getLikesCount(contacts.get(Platform.FACEBOOK));

        DeferredObject.when(facebookPostsAction, facebookEventsAction, twitterAction, instagramAction).done(new ResolveCallback<MergedPromiseResult4<List<SocialWindowPost>,List<SocialWindowPost>, List<SocialWindowPost>, List<SocialWindowPost>>>() {
            @Override
            public void onResolve(MergedPromiseResult4<List<SocialWindowPost>,
                    List<SocialWindowPost>,
                    List<SocialWindowPost>,
                    List<SocialWindowPost>> MergedResults) {

                List<SocialWindowPost> facebookPosts = MergedResults.first();
                List<SocialWindowPost> facebookEvents = MergedResults.second();
                List<SocialWindowPost> twitterPosts = MergedResults.third();
                List<SocialWindowPost> instagramPosts = MergedResults.forth();
                posts = sorter.sort(facebookPosts, facebookEvents, twitterPosts, instagramPosts);


                long endTime = System.currentTimeMillis();
                Log.i("d", "Time to get posts, merge and sort: " + Long.toString(endTime - startTime));

                isDataReady = true;
                meCardOnly = false;
                updateMostRecentPostDate(notifyNewPostsOnCompletion);
                onGetMergedPostsComplete();

            }
        }).fail(new RejectCallback<MergedPromiseReject>() {
            @Override
            public void onReject(MergedPromiseReject mergedPromiseReject) {
            }
        }).progress(new ProgressCallback<MergedPromiseProgress>() {
            @Override
            public void onProgress(MergedPromiseProgress mergedPromiseProgress) {
                if (socialWindowPostsListener != null) {
                    socialWindowPostsListener.onLoading();
                }
            }
        });
    }

    private void getLikesCount(final List<MemberContact> contact) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (contact == null || contact.isEmpty()) {
                    return;
                }
                String id = contact.get(0).getId();
                String likesCount = FacebookAuthenticator.getInstance(context).getLikesCount(id, 100);
                meCardHolder.setLikesCount(likesCount);
            }
        }).start();
    }

    private List<SocialWindowPost> getFacebookPostsByType(List<MemberContact> memberContact, FacebookPostType type) {
        try {
            if (memberContact.isEmpty()) {
                return new ArrayList<SocialWindowPost>();
            }
            String id = memberContact.get(0).getId();
            String name = memberContact.get(0).getContactName();
            if (StringUtils.isBlank(name)) {
                name = meCardHolder.getName();
            }

            if (type == FacebookPostType.POST) {
                return FacebookAuthenticator.getInstance(context).getPosts(null, id, name, POSTS_PER_PAGE);
            } else {
                return FacebookAuthenticator.getInstance(context).getEvents(null, id, EVENTS_TO_GET);
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
            Log.e(TAG, "Failed to get facebook " + type + ": " + e.getMessage());
        }
        return new ArrayList<SocialWindowPost>();
    }

    private void updateMostRecentPostDate(boolean notifyNewPosts) {
        if (!posts.isEmpty()) {
            Date mostRecentPostData = posts.get(0).getPublishTime();
            if (!feedMostRecentPostDate.containsKey(feedId) ||
                    mostRecentPostData.compareTo(feedMostRecentPostDate.get(feedId)) > 0) {
                feedMostRecentPostDate.put(Long.valueOf(feedId), mostRecentPostData);
                isThereSomethingNew = true;
                if (newPostsListener != null && notifyNewPosts) {
                    newPostsListener.onNewPosts();
                }
            }
        }
    }

    /**
     * To fetch user posts given a platform.
     */
    private List<SocialWindowPost> getPostsByPlatform(Map<Platform, List<MemberContact>> contacts, Platform platform) {

        try {
            String id;
            if (contacts.containsKey(platform)) {
                id = contacts.get(platform).get(0).getId();
            } else {
                id = meCardHolder.getSocialProfile(platform);
            }
            if (!StringUtils.isBlank(id)) {

                switch (platform) {

                    case TWITTER:
                        return TwitterAuthenticator.getInstance(context).getPosts(null, id, PAGES, POSTS_PER_PAGE);
                    case INSTAGRAM:
                        return InstagramAuthenticator.getInstance(context).getPosts(null, id, POSTS_PER_PAGE);
                }
            }
        } catch (Exception e ) {
            Crashlytics.logException(e);
            Log.e(TAG, "Failed to get posts for platform " + platform.getName() + ": " + e.getMessage());
        }

        return new ArrayList<SocialWindowPost>();
    }

    private void onGetMergedPostsComplete() {
        if (socialWindowPostsListener != null) {
            notifySocialWindowPostsListener();
        }
    }

    public SocialWindowProvider setListener(SocialWindowPostsListener listener) {
        socialWindowPostsListener = listener;
        if (isDataReady) {
            notifySocialWindowPostsListener();
        }
        return this;
    }

    public static void setNewPostsListener(NewPostsListener listener) {
        newPostsListener = listener;
    }

    private void notifySocialWindowPostsListener() {
        socialWindowPostsListener.onComplete(posts, isThereSomethingNew, meCardOnly);
    }


    private class WelcomeFeedFactory {
        private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        private int FACEBOOK_LIKES_COUNT = 34;
        private int FACEBOOK_COMMENTS_COUNT = 20;

        private int TWITTER_FAVORITE_COUNT = 22;
        private int TWITTER_RETWEETS_COUNT = 28;
        private final String TWITTER_LOCATION = "500 Startups, CA";

        private int INSTAGRAM_LIKES_COUNT = 33;
        private int INSTAGRAM_COMMENTS_COUNT = 21;

        private final String ME_CARD_TITLE = "Product Manager";
        private final String ME_CARD_ORGANIZATION = "Blinq";
        private final String ME_CARD_NAME = "Tony T";
        private final String GOOGLE_PLUS_USER = "communities/101894883006546057410";
        private final String FACEBOOK_USER = "gotblinq";
        private final String TWITTER_USER = "GotblinQ";
        private final String INSTAGRAM_USER = "gotblinq";

        public void createFeed() {
            createMeCardContent();
            createPosts();

        }

        private void createMeCardContent() {
            meCardHolder.clearData();

            meCardHolder.setTitle(ME_CARD_TITLE);
            meCardHolder.setOrganization(ME_CARD_ORGANIZATION);
            meCardHolder.setName(ME_CARD_NAME);
            meCardHolder.setImagePath(ImageUtils.DRAWABLE_PATH + R.drawable.welcome_feed_profile);
            meCardHolder.setSocialProfile(Platform.FACEBOOK, FACEBOOK_USER);
            meCardHolder.setSocialProfile(Platform.TWITTER, TWITTER_USER);
            meCardHolder.setSocialProfile(Platform.INSTAGRAM, INSTAGRAM_USER);
            meCardHolder.setSocialProfile(Platform.GOOGLEPLUS, GOOGLE_PLUS_USER);
        }

        private void createPosts() {
            posts = new ArrayList<SocialWindowPost>();
            posts.add(createFacebookPost());
            posts.add(createTwitterPost());
            posts.add(createInstagramPost());
        }

        private SocialWindowPost createFacebookPost() {
            FacebookPost post = new FacebookPost();
            setPostContent(post, Platform.FACEBOOK, R.string.welcome_feed_facebook_text);
            post.setStory("");
            post.setLikesCount(FACEBOOK_LIKES_COUNT);
            post.setCommentsCount(FACEBOOK_COMMENTS_COUNT);
            post.setHasPicture(true);
            post.setPictureUrl(ImageUtils.DRAWABLE_PATH + R.drawable.blackhole);
            return post;
        }

        private SocialWindowPost createTwitterPost() {
            TwitterPost post = new TwitterPost();
            setPostContent(post, Platform.TWITTER, R.string.welcome_feed_twitter_text);
            post.setRetweetsCount(TWITTER_RETWEETS_COUNT);
            post.setFavoritesCount(TWITTER_FAVORITE_COUNT);
            setLocation(post, TWITTER_LOCATION);
            setContactPicture(post, R.drawable.ic_launcher);
            return post;
        }

        private SocialWindowPost createInstagramPost() {
            InstagramPost post = new InstagramPost();
            setPostContent(post, Platform.INSTAGRAM, R.string.welcome_feed_instagram_text);
            post.setLikesCount(INSTAGRAM_LIKES_COUNT);
            post.setCommentsCount(INSTAGRAM_COMMENTS_COUNT);
            setPicture(post, R.drawable.team_image);
            return post;
        }

        private void setPostContent(SocialWindowPost post, Platform platform,
                                    int statusBody) {
            post.setCoverPageStatusPlatform(platform);
            post.setStatusBody(context.getString(statusBody));
            post.setPublishTime(getPublishDateFromString());
        }

        private Date getPublishDateFromString() {
            Date date = null;
            long TWO_DAYS = 2L * 24 * 60 * 60 * 1000;
            Date PUBLISHED_TIME = new Date(new Date().getTime() - TWO_DAYS);
            Calendar c = Calendar.getInstance();
            c.setTime(PUBLISHED_TIME);
            try {
                String d = String.format("%d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
                Log.d(TAG, d);
                date = dateFormat.parse(d);
            } catch (ParseException e) {
                Log.e(TAG, "Failed to parse date");
            }
            return date;
        }

        private void setPicture(SocialWindowPost post, int drawable) {
            post.setHasPicture(true);
            post.setPictureUrl(ImageUtils.DRAWABLE_PATH + drawable);
        }

        private void setLocation(SocialWindowPost post, String city) {
            Location location = new Location();
            location.setCity(city);
            location.setName(city);
            post.setHasLocation(true);
            post.setLocation(location);
        }

        private void setContactPicture(SocialWindowPost post, int drawable) {
            Contact contact = new Contact();
            contact.setHasPhoto(true);
            contact.setPhotoUri(Uri.parse(ImageUtils.DRAWABLE_PATH + drawable));
            post.setUser(contact);
        }
    }
}
