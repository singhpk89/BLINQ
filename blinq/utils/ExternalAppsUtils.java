package com.blinq.utils;

import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

import com.blinq.models.Platform;
import com.blinq.models.social.window.PostTypeTag;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.models.social.window.StatusContent;
import com.blinq.provider.ContactsManager;

public class ExternalAppsUtils {

    private static final String TAG = ExternalAppsUtils.class.getSimpleName();

    public static final String FACEBOOK_ANDROID_APP_PACKAGE_NAME = "com.facebook.katana";
    public static final String FACEBOOK_MESSENGER_APP_PACKAGE_NAME = "com.facebook.orca";
    public static final String FACEBOOK_POSTS_URL = "fb://post/%s";
    public static final String FACEBOOK_EVENT_URL = "fb://event/%s";

    public static final String SKYPE_APP_PACKAGE_NAME = "com.skype.raider";
    public static final String SKYPE_APP_MAIN_CLASS = "com.skype.raider.Main";
    public static final String SKYPE_APP_MARKET_LINK = "market://details?id=com.skype.raider&referrer=com.headbox";
    public static final String SKYPE_CHAT_URI = "skype:%s?chat";
    public static final String TWITTER_ANDROID_APP_PACKAGE_NAME = "com.twitter.android";
    public static final String TWITTER_POSTS_URL = "twitter://status?status_id=%s";

    public static final String INSTAGRAM_ANDROID_APP_PACKAGE_NAME = "com.instagram.android";
    public static final String INSTAGRAM_URL_HANDLER_ACTIVITY = "com.instagram.android.activity.UrlHandlerActivity";

    public static final String YOUTUBE_ANDROID_APP_PACKAGE_NAME = "vnd.youtube:%s";
    public static final String YOUTUBE_VIDEO_ID_PARAMETER = "v";

    public static final String GOOGLE_PLUS_ANDROID_APP_PACKAGE_NAME = "com.google.android.apps.plus";
    public static final String GOOGLE_PLUS_COMMUNITY_ACTIVITY = "com.google.android.apps.plus.phone.UrlGatewayActivity";
    public static final String GOOGLE_PLUS_CUSTOM_APP_URL = "customAppUri";
    public static final String GOOGLE_PLUS_URL = "https://plus.google.com/";
    public static final String GOOGLE_PLUS_COMMUNITIES = "communities/";
    public static final String HANGOUTS_PACKAGE_NAME = "com.google.android.talk";
    public static final Object GMAIL_PACKAGE_NAME = "com.google.android.gm";

    public static final String WHATSAPP_PACKAGE_NAME = "com.whatsapp";
    public static final String WHATSAPP_APP_MARKET_LINK = "market://details?id=com.whatsapp&referrer=com.headbox";

    public static final String DEFAULT_SMS_APP_PACKAGE = "com.android.mms";
    public static final String DEFAULT_CALL_APP_PACKAGE = "com.android.phone";

    public static final String EMAIL_APP_PACKAGE_NAME = "com.android.email";
    public static final String GOOGLE_INBOX_APP_PACKAGE_NAME = "com.google.android.apps.inbox";

    public static final int OPEN_EXTERNAL_APP_REQUEST_CODE = 9543;

    /**
     * Checks if the platform's android app installed on user's device.
     *
     * @param platform
     * @param context
     * @return
     */
    public static boolean isAppInstalledFor(Platform platform, Context context) {

        switch (platform) {

            case FACEBOOK:
                return isFacebookAppInstalled(context);
            case INSTAGRAM:
                return isInstagramAppInstalled(context);
            case TWITTER:
                return isTwitterAppInstalled(context);
            case HANGOUTS:
                return isGooglePlusAppInstalled(context);
            case SKYPE:
                return isSkypeClientInstalled(context);
            default:
                return false;

        }

    }

    /**
     * Checks if the Facebook android app installed on user's device.
     *
     * @param context
     */
    public static boolean isFacebookAppInstalled(Context context) {

        return AppUtils.isPackageExist(context,
                FACEBOOK_ANDROID_APP_PACKAGE_NAME);

    }

    /**
     * Checks if the Facebook messanger android app installed on user's device.
     *
     * @param context
     */
    public static boolean isFacebookMessengerAppInstalled(Context context) {

        return AppUtils.isPackageExist(context,
                FACEBOOK_MESSENGER_APP_PACKAGE_NAME);

    }

    /**
     * Checks if the Twitter android app installed on user's device.
     *
     * @param context
     */
    public static boolean isTwitterAppInstalled(Context context) {

        return AppUtils.isPackageExist(context,
                TWITTER_ANDROID_APP_PACKAGE_NAME);

    }

    /**
     * Checks if the Google+ android app installed on user's device.
     *
     * @param context
     */
    public static boolean isGooglePlusAppInstalled(Context context) {

        return AppUtils.isPackageExist(context,
                GOOGLE_PLUS_ANDROID_APP_PACKAGE_NAME);

    }

    /**
     * Checks if the Instagram android app installed on user's device.
     *
     * @param context
     */
    public static boolean isInstagramAppInstalled(Context context) {

        return AppUtils.isPackageExist(context,
                INSTAGRAM_ANDROID_APP_PACKAGE_NAME);

    }

    public static void launchPlatformPostIntent(SocialWindowPost post, Context context) {

        try {
            context.startActivity(ExternalAppsUtils.getPlatformPostIntent(post, context));
        } catch (android.content.ActivityNotFoundException e) {
            Log.e(TAG, "No Activity found to handle Intent.");
        }
    }

    /**
     * Get intent will open the platform's android application on a specific
     * post.
     *
     * @param socialWindowPost
     * @param context
     * @return
     */
    public static Intent getPlatformPostIntent(
            SocialWindowPost socialWindowPost, Context context) {

        switch (socialWindowPost.getCoverPageStatusPlatform()) {
            case FACEBOOK:
                if (socialWindowPost.getTag() == PostTypeTag.EVENT)
                    return getFacebookEventIntent(socialWindowPost.getId());
                else
                    return getFacebookPostIntent(socialWindowPost.getId());
            case INSTAGRAM:
                return getInstagramPostIntent(context, socialWindowPost.getLink());

            case TWITTER:
                return getTwitterPostIntent(socialWindowPost.getId());

            default:
                return null;

        }
    }

    /**
     * Get intent will open the facebook android application on a specific post.
     *
     * @param postId
     * @return
     */
    public static Intent getFacebookPostIntent(String postId) {
        Intent facebookPostIntent = new Intent(Intent.ACTION_VIEW);
        String postUrl = String.format(FACEBOOK_POSTS_URL, postId);
        facebookPostIntent.setData(Uri.parse(postUrl));
        facebookPostIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return facebookPostIntent;
    }

    public static Intent getFacebookEventIntent(String postId) {
        Intent facebookPostIntent = new Intent(Intent.ACTION_VIEW);
        String postUrl = String.format(FACEBOOK_EVENT_URL, postId);
        facebookPostIntent.setData(Uri.parse(postUrl));
        facebookPostIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return facebookPostIntent;
    }

    /**
     * Get intent will open the Twitter android application on a specific
     * Status.
     *
     * @param postId
     * @return
     */
    public static Intent getTwitterPostIntent(String postId) {
        Intent twitterPostIntent = new Intent(Intent.ACTION_VIEW);
        String postUrl = String.format(TWITTER_POSTS_URL, postId);
        twitterPostIntent.setData(Uri.parse(postUrl));
        twitterPostIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return twitterPostIntent;
    }

    /**
     * Get intent will open the Instagram android application on a specific
     * post.
     *
     * @param postId
     * @return
     */
    public static Intent getInstagramPostIntent(Context context, String postLink) {
        Intent instagramPostsIntent = context.getPackageManager()
                .getLaunchIntentForPackage(INSTAGRAM_ANDROID_APP_PACKAGE_NAME);
        instagramPostsIntent.setComponent(new android.content.ComponentName(
                INSTAGRAM_ANDROID_APP_PACKAGE_NAME,
                INSTAGRAM_URL_HANDLER_ACTIVITY));
        instagramPostsIntent.setData(Uri.parse(postLink));
        instagramPostsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return instagramPostsIntent;
    }

    /**
     * Get intent will open the Youtube android application on a specific video.
     *
     * @param context
     * @param videoLink
     * @return
     */
    public static Intent getYoutubeIntent(Context context, String videoLink) {
        String videoId = Uri.parse(videoLink).getQueryParameter(
                YOUTUBE_VIDEO_ID_PARAMETER);
        Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String
                .format(YOUTUBE_ANDROID_APP_PACKAGE_NAME, videoId)));
        List<ResolveInfo> packages = context.getPackageManager()
                .queryIntentActivities(youtubeIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (packages.size() == 0 || StringUtils.isBlank(videoId)) {
            // default youtube app not present
            youtubeIntent = new Intent(Intent.ACTION_VIEW);
            youtubeIntent.setData(Uri.parse(videoLink));
        }
        youtubeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return youtubeIntent;
    }

    /**
     * Open Google+ community page
     */

    public static void openGoogleCommunityPage(Activity activity,
                                               String communityId) {
        try {
            String communityPage = GOOGLE_PLUS_COMMUNITIES + communityId;
            Uri googlePLusUri = Uri.parse(GOOGLE_PLUS_URL + communityPage);
            if (isAppInstalledFor(Platform.HANGOUTS,
                    activity.getApplicationContext())) {
                Intent googlePlusIntent = new Intent(Intent.ACTION_VIEW);
                googlePlusIntent
                        .setPackage(GOOGLE_PLUS_ANDROID_APP_PACKAGE_NAME);
                googlePlusIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                googlePlusIntent.putExtra(GOOGLE_PLUS_CUSTOM_APP_URL,
                        communityPage);

                try {
                    activity.startActivity(googlePlusIntent);
                    return;

                } catch (ActivityNotFoundException activityNotFoundException) {

                    // No need for any action
                }

            }
            Intent googlePlusIntent = new Intent(Intent.ACTION_VIEW,
                    googlePLusUri);
            activity.startActivity(googlePlusIntent);

        } catch (Exception ex) {
            Log.e(TAG, "Open google plus community error:" + ex);
        }

    }

    /**
     * Determine whether the Skype for Android client is installed on this
     * device.
     */
    public static boolean isSkypeClientInstalled(Context context) {
        return AppUtils.isPackageExist(context, SKYPE_APP_PACKAGE_NAME);
    }

    /**
     * Determine whether the whatsapp application is installed on this device.
     */
    public static boolean isWhatsappInstalled(Context context) {
        return AppUtils.isPackageExist(context, WHATSAPP_PACKAGE_NAME);
    }

    /**
     * Install certain app through the market: URI scheme.
     *
     * @param uri - uri of the app.
     */
    public static void goToMarket(Context context, String uri) {
        Uri marketUri = Uri.parse(uri);
        Intent myIntent = new Intent(Intent.ACTION_VIEW, marketUri);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myIntent);
        return;
    }

    /**
     * Initiate the actions encoded in the specified URI.
     */
    public static void initiateSkypeUri(Context context, String skypeUri) {

        // Make sure the Skype for Android client is installed.
        if (!isSkypeClientInstalled(context)) {
            goToMarket(context, SKYPE_APP_MARKET_LINK);
            return;
        }

        // Create the Intent from our Skype URI.
        Uri uri = Uri.parse(skypeUri);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        // Restrict the Intent to being handled by the Skype for Android client
        // only.
        intent.setComponent(new ComponentName(SKYPE_APP_PACKAGE_NAME,
                SKYPE_APP_MAIN_CLASS));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Initiate the Intent. It should never fail because you've already
        // established the
        // presence of its handler (although there is an extremely minute window
        // where that
        // handler can go away).
        context.startActivity(intent);

        return;
    }

    /**
     * Open skype chat window with a contact.
     */
    public static void openSkypeChat(Context context, String userName) {

        if (org.apache.commons.lang3.StringUtils.isBlank(userName))
            return;

        String chatUri = String.format(SKYPE_CHAT_URI, userName);
        initiateSkypeUri(context, chatUri);
    }

    /**
     * Open a conversation with a certain whatsapp contact.
     */
    public static void openWhatsappConversation(Activity activity,
                                                String whatsappId) {

        if (org.apache.commons.lang3.StringUtils.isBlank(whatsappId))
            return;

        if (!isWhatsappInstalled(activity)) {
            goToMarket(activity, WHATSAPP_APP_MARKET_LINK);
            return;
        }

        final String[] projection = new String[]{Contacts.Data._ID};
        final String selection = ContactsContract.Data.DATA1 + "=?";
        final String[] selectionArgs = new String[]{whatsappId};

        Cursor cursor = activity.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI, projection, selection,
                selectionArgs, null);

        if (cursor == null || cursor.getCount() == 0) {
            return;
        }

        cursor.moveToFirst();
        String contact = String.format(ContactsManager.CONTACT_DATA_URI,
                cursor.getString(cursor.getColumnIndex(Contacts.Data._ID)));
        cursor.close();

        Log.d(TAG, "Whatspp id=" + whatsappId + ",Contact id =" + contact);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(contact));
        intent.setPackage(ExternalAppsUtils.WHATSAPP_PACKAGE_NAME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivityForResult(intent,
                ExternalAppsUtils.OPEN_EXTERNAL_APP_REQUEST_CODE);

    }
}
