package com.blinq.ui.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blinq.R;
import com.blinq.ImageLoaderManager;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.models.FeedModel;
import com.blinq.models.Platform;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.authentication.impl.AuthUtils.AuthAction;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.ui.activities.instantmessage.InstantMessageActivity;
import com.blinq.utils.AppUtils;
import com.blinq.utils.ImageUtils;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

/**
 * Manages the process of displaying cover page and status for specific
 * platform.
 *
 * @author Johan Hansson
 */
public class PlatformCoverListAdapter extends BaseAdapter {

    private final String TAG = PlatformCoverListAdapter.class.getSimpleName();

    public static final int DEFAULT_PLATFORM_ID = Platform.NOTHING.getId();

    private static final int FADE_IN_DURATION = 100;

    public static final String INTIAL_POST = "intial post";

    private LayoutInflater inflater;

    private ImageLoaderManager imageLoaderManager;

    private List<SocialWindowPost> platformCoverList;

    private boolean showStatus;

    private SparseIntArray platformIconSparseArray = new SparseIntArray() {

        {
            put(Platform.NOTHING.getId(), R.drawable.empty);
            put(Platform.FACEBOOK.getId(), R.drawable.conversation_outbound_fb);
            put(Platform.TWITTER.getId(), R.drawable.conversation_outbound_twitter);
            put(Platform.INSTAGRAM.getId(), R.drawable.conversation_outbound_instagram);

        }
    };

    private FragmentActivity activity;
    private Context context;

    private AuthAction authAction;

    private int feedId;

    /**
     * @param inflater        layout inflater for instant message main activity.
     * @param coverPageStatus array of CoverPageStatus object that contains cover page and
     *                        status for specific platform.
     * @param showStatus      specify whether to show status bar or not.
     */
    public PlatformCoverListAdapter(LayoutInflater inflater,
                                    List<SocialWindowPost> platformCoverList, boolean showStatus,
                                    FragmentActivity activity, Context context, int feedId,
                                    AuthAction authAction) {
        this.inflater = inflater;
        this.platformCoverList = platformCoverList;
        this.showStatus = showStatus;
        this.activity = activity;
        this.context = context;
        this.feedId = feedId;
        this.authAction = authAction;

        initializeImageLoader();
    }

    /**
     * Initialize image loader with specific configuration as needed for our
     * application.
     */
    private void initializeImageLoader() {

        imageLoaderManager = new ImageLoaderManager(context);



        DisplayImageOptions displayImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisk(true)
                .cacheInMemory(false)
                .showImageOnLoading(R.color.dark_gray)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();

        imageLoaderManager.setDisplayImageOptions(displayImageOptions);
    }

    @Override
    public int getCount() {
        return platformCoverList.size();
    }

    /**
     * This function return source ID of platform image for given position.
     *
     * @param position the selected platform position.
     * @return the source ID of platform image.
     */
    public int getPlatformImage(int position) {

        int platformIconMapKey = platformCoverList.get(position)
                .getCoverPageStatusPlatform().getId();

        if (platformIconSparseArray.get(platformIconMapKey, -1) != -1) {
            return platformIconSparseArray.get(platformIconMapKey);
        }

        return (platformIconSparseArray.get(DEFAULT_PLATFORM_ID));
    }

    @Override
    public Object getItem(int arg0) {
        return platformCoverList.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {

        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {

            holder = new ViewHolder();
            convertView = inflater.inflate(
                    R.layout.cover_status_pager_adapter_layout, null);
            holder.coverImage = (ImageView) convertView
                    .findViewById(R.id.account_cover_page);
            holder.accountStatusLayout = (LinearLayout) convertView
                    .findViewById(R.id.account_status_layout);
            holder.accountStatusImage = (ImageView) convertView
                    .findViewById(R.id.account_status_image);
            holder.accountStatustext = (TextView) convertView
                    .findViewById(R.id.account_status_text);

            holder.accountStatustext.setTextDirection(TextView.TEXT_DIRECTION_ANY_RTL);

            holder.loginLayout = (LinearLayout) convertView
                    .findViewById(R.id.loginLayout);
            holder.accountStatusImage
                    .setImageResource(getPlatformImage(position));

            convertView.setTag(holder);
        } else {

            holder = (ViewHolder) convertView.getTag();
        }

        SocialWindowPost currentPost = platformCoverList.get(position);
        Platform postPlatform = currentPost.getCoverPageStatusPlatform();
        holder.loginLayout.setVisibility(View.GONE);
        if (showStatus) {
            holder.accountStatusLayout.setVisibility(View.VISIBLE);

        } else {
            holder.accountStatusLayout.setVisibility(View.GONE);
        }
        holder.accountStatusImage.setVisibility(View.VISIBLE);

        if (isInitialPost(currentPost)) {

            holder.accountStatusImage.setVisibility(View.GONE);
            holder.accountStatusLayout.setVisibility(View.GONE);
            holder.loginButton = (ImageButton) convertView
                    .findViewById(R.id.LoginMergeButton);
            holder.loginCaption = (TextView) convertView
                    .findViewById(R.id.LoginMergeCaption);
            holder.loginCaptionDescription = (TextView) convertView
                    .findViewById(R.id.LoginMergeCaptionDescription);
            holder.coverImage
                    .setImageResource(getInitialPostBackground(postPlatform));
            holder.loginButton.setBackgroundResource(setupSocialWindowInfo(
                    postPlatform, holder.loginCaption, holder.loginCaptionDescription));
            holder.loginButton
                    .setOnClickListener(getLoginClickListener(postPlatform));

            holder.loginLayout.setVisibility(View.VISIBLE);

        } else {
            if (currentPost.hasPicture()) {

                String pictureUrl = currentPost.getPictureUrl();
                loadImage(holder, convertView.getContext(), pictureUrl);

            } else {

                if (platformCoverList.get(0).hasPicture()) {

                    String pictureUrl = platformCoverList.get(0)
                            .getPictureUrl();

                    loadImage(holder, convertView.getContext(), pictureUrl);

                } else {
                    holder.coverImage.setImageResource(R.color.gray);
                }
            }
        }

        if (currentPost.hasMessage()) {
            holder.accountStatustext.setText(currentPost.getStatusBody());
        } else {
            holder.accountStatustext.setText(StringUtils.EMPTY_STRING);
        }

        return convertView;
    }

    /**
     * Check the give image URL if it's locally, load the image directly. If not
     * load it using universal image loader.
     *
     * @param holder   view holder.
     * @param context  application context.
     * @param imageUrl image URL to be loaded.
     */
    private void loadImage(ViewHolder holder, Context context, String imageUrl) {

        if (imageUrl.contains(ImageUtils.DRAWABLE_PATH)) {

            holder.coverImage.setImageDrawable(context.getResources()
                    .getDrawable(getImageIdFromDrawablePictureUri(imageUrl)));

        } else {

            imageLoaderManager
                    .loadImage(holder.coverImage, Uri.parse(imageUrl));
        }

    }

    /**
     * Get the image id from local image URI. Used to load image directly using
     * it's id without using image loader.
     *
     * @param imageUri URI to the local image.
     * @return image id.
     */
    private int getImageIdFromDrawablePictureUri(String imageUri) {

        return Integer.parseInt(imageUri.substring(ImageUtils.DRAWABLE_PATH
                .length()));

    }

    /**
     * Get click listener for the (login/merge) Image button.
     */
    private OnClickListener getLoginClickListener(final Platform postPlatform) {
        OnClickListener platformButtonClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (postPlatform == Platform.NOTHING) {

                    try {

                        FeedModel friendFeed = FeedProviderImpl.getInstance(
                                context).getFeed(feedId);
                        AppUtils.openAddContactIntent(activity,
                                AppUtils.CONTACT_SAVE_INTENT_REQUEST,
                                friendFeed.getContact().getIdentifier());

                    } catch (android.content.ActivityNotFoundException e) {

                        UIUtils.alertUser(context,
                                context.getString(R.string.unknown_error_));
                    }
                } else {
                    Authenticator authenticator = AuthUtils.getAuthInstance(
                            postPlatform, activity);
                    connect(authenticator, postPlatform);
                }

            }
        };

        return platformButtonClickListener;
    }

    /**
     * Check if the social window post is initial (not real platform post)
     */
    public static boolean isInitialPost(SocialWindowPost currentPost) {
        return (currentPost.HasLink() && currentPost.getLink().equals(
                INTIAL_POST));

    }

    /**
     * Get social window background based on platform.
     */
    private int getInitialPostBackground(Platform coverPageStatusPlatform) {

        int resourceId;
        switch (coverPageStatusPlatform) {

            case FACEBOOK:

                resourceId = R.drawable.white_bitmap;

                break;
            case INSTAGRAM:

                resourceId = R.drawable.white_bitmap;

                break;
            case TWITTER:

                resourceId = R.drawable.white_bitmap;

                break;
            default:
                resourceId = R.drawable.white_bitmap;
                break;

        }
        return resourceId;

    }

    /**
     * Get information for the social window
     * Check if user logged in to platform - if not show login
     * Check if merged - if not suggest merge
     * Check if new friend - if is show new friend button
     */
    private int setupSocialWindowInfo(Platform coverPageStatusPlatform,
                                      TextView loginCaption,
                                      TextView loginCaptionDescription) {

        boolean isPlatformConnected = false;
        if (activity != null) {
            Authenticator authenticator = AuthUtils.getAuthInstance(
                    coverPageStatusPlatform, activity);
            if (authenticator != null)
                isPlatformConnected = authenticator.isConnected()
                        && authenticator.isLoginCompleted();
        }
        int resourceId;
        loginCaptionDescription.setVisibility(View.VISIBLE);
        setupLoginCaption(coverPageStatusPlatform, isPlatformConnected,
                loginCaption, loginCaptionDescription);
        switch (coverPageStatusPlatform) {

            case FACEBOOK:
                if (isPlatformConnected) {
                    resourceId = R.drawable.link_facebook;
                } else {
                    resourceId = R.drawable.login_fb;
                }

                break;
            case HANGOUTS:
                if (isPlatformConnected) {
                    resourceId = R.drawable.link_google;
                } else {
                    resourceId = R.drawable.login_gplus;
                }
                break;
            case INSTAGRAM:
                if (isPlatformConnected) {
                    resourceId = R.drawable.link_instagram;
                } else {
                    resourceId = R.drawable.instagram_btn;
                }
                break;
            case TWITTER:
                if (isPlatformConnected) {
                    resourceId = R.drawable.link_twitter;
                } else {
                    resourceId = R.drawable.login_twitter;
                }
                break;
            case NOTHING:
                resourceId = R.drawable.new_contact;
                loginCaption.setText(context
                        .getString(R.string.new_contact_caption));
                loginCaptionDescription.setVisibility(View.GONE);
                break;
            default:
                resourceId = R.drawable.login_fb;
                break;

        }
        return resourceId;

    }

    /**
     * Setup the text above login\link button
     */
    private void setupLoginCaption(Platform platform,
                                   boolean isPlatformConnected, TextView loginCaption,
                                   TextView loginCaptionDescription) {
        if (isPlatformConnected) {
            FeedModel friendFeed = FeedProviderImpl.getInstance(context)
                    .getFeed(feedId);

            String contactName = "";
            if (friendFeed != null && friendFeed.getContact() != null)
                contactName = friendFeed.getContact().getFirstName();

            String linkCaption = String.format(
                    context.getString(R.string.link_person_caption),
                    contactName.toUpperCase(), platform.getName().toUpperCase());

            loginCaption.setText(linkCaption);

            String linkCaptionDescription = String.format(
                    context.getString(R.string.link_person_caption_description),
                    contactName, platform.getName());

            loginCaptionDescription.setText(linkCaptionDescription);

        } else {
            loginCaption.setText(context.getString(R.string.login_caption));
            loginCaptionDescription.setText(String.format(context.getString(
                    R.string.login_caption_description), platform.getName()));
        }

    }

    public void setShowStatus(boolean showStatus) {
        this.showStatus = showStatus;
    }

    /**
     * Login to specific Platform or display merge screen .
     *
     * @param authenticator - authentication instance.
     * @param platform      - Authentication platform.
     */
    private void connect(Authenticator authenticator, Platform platform) {

        if (activity == null)
            return;

        if (authenticator.isConnected() && !authenticator.isLoginCompleted()) {
            AuthUtils.LoadContacts(platform, this.authAction, activity, true,
                    AnalyticsConstants.LOGIN_FROM_SOCIAL_WINDOW);
            return;
        }
        if (!authenticator.isConnected()) {

            AuthUtils.LoginCallBack loginCallCallback = new AuthUtils().new LoginCallBack(
                    platform, activity, this.authAction, true,
                    AnalyticsConstants.LOGIN_FROM_SOCIAL_WINDOW);
            authenticator.login(loginCallCallback);

        } else {

//            InstantMessageActivity.displayMergeView(activity, feedId, platform,
//                    SearchHandler.MERGE_SEARCH_HANDLER);

            return;
        }

        if (activity instanceof InstantMessageActivity)
            ((InstantMessageActivity) activity).setRedirectToHomeEnabled(false);
    }

    /**
     * Used to hold the UI of single view to avoid duplicating view for each
     * list element.
     */
    public static class ViewHolder {

        public ImageView coverImage;
        public ImageView accountStatusImage;
        TextView accountStatustext;
        LinearLayout accountStatusLayout;
        ImageButton loginButton;
        TextView loginCaption;
        TextView loginCaptionDescription;
        LinearLayout loginLayout;

    }

}
