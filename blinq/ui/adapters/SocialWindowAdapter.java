package com.blinq.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blinq.ImageLoaderManager;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.models.Contact;
import com.blinq.models.Platform;
import com.blinq.models.social.window.FacebookPost;
import com.blinq.models.social.window.InstagramPost;
import com.blinq.models.social.window.MeCard;
import com.blinq.models.social.window.PostTypeTag;
import com.blinq.models.social.window.SocialWindowCard;
import com.blinq.models.social.window.SocialWindowItem;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.models.social.window.StatusContent;
import com.blinq.models.social.window.TwitterPost;
import com.blinq.utils.ImageUtils;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;
import com.meetme.android.horizontallistview.HorizontalListView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by Johan Hansson on 9/17/2014.
 * <p/>
 * Used to display list of posts in unified social window list-view.
 */
public class SocialWindowAdapter extends BaseAdapter implements View.OnClickListener {

    private static final int SOCIAL_WINDOW_ROUND_RADIUS_PX = 15;
    /**
     * String that should be pre appended to the post title.
     */
    private final String TAG = SocialWindowAdapter.class.getSimpleName();

    private Context context;
    private List<SocialWindowItem> posts;
    private PreferencesManager preferencesManager;

    private LayoutInflater layoutInflater;

    private ImageLoaderManager imageLoaderManager;

    private OnCardItemButtonClicked onCardItemButtonClicked;

    private boolean isCoverImageLoadingEnabled = true;
    private DisplayImageOptions displayImageOptions;
    private DisplayImageOptions displayRoundedImageOptions;

    private MeCardProfileAdapter meCardProfileAdapter;
    private MeCardMutualFriendsAdapter meCardMutualFriendsAdapter;

    public static boolean profileClicked;

    private boolean shouldApplyMeCard;

    public SocialWindowAdapter(Context context, List<SocialWindowItem> posts, OnCardItemButtonClicked onCardItemButtonClicked) {

        this.context = context;
        this.posts = posts;
        this.onCardItemButtonClicked = onCardItemButtonClicked;
        this.preferencesManager = new PreferencesManager(context);
        layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.shouldApplyMeCard = true;
        initializeImageLoader();
    }


    /**
     * Initialize image loader with two kinds of configuration as needed for the
     * application.
     */
    private void initializeImageLoader() {

        imageLoaderManager = new ImageLoaderManager(context);

        displayImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisk(true)
                .cacheInMemory(false)
                .showImageOnLoading(R.drawable.white_bitmap)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();

        // Found out that when using rounded corners - the image get stretched. make sure to use only on perfect square images
        displayRoundedImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisk(true)
                .cacheInMemory(false)
                .displayer(new RoundedBitmapDisplayer(SOCIAL_WINDOW_ROUND_RADIUS_PX))
                .showImageOnLoading(R.drawable.white_bitmap)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();

        imageLoaderManager.setDisplayImageOptions(displayImageOptions);
    }

    @Override
    public int getCount() {
        if (posts == null) {
            return 0;
        }
        return posts.size();
    }

    @Override
    public Object getItem(int position) {
        return posts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        SocialWindowItem socialWindowItem = posts.get(position);

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.social_window_list_item, parent, false);
            viewHolder = new ViewHolder();

            viewHolder.itemView = convertView;

            // For post item.
            viewHolder.postItemContainer = convertView.findViewById(R.id.socialWindowPostItemContainer);
            viewHolder.coverImageView = (ImageView) convertView.findViewById(R.id.imageViewForSocialWindowCover);
            viewHolder.coverImageLayout = convertView.findViewById(R.id.coverImageLayout);
            viewHolder.socialWindowContentAboveCoverImage = convertView.findViewById(R.id.relativeLayoutForSocialWindowItemContentsAboveCover);
            viewHolder.statusAndVideoIconContainer = convertView.findViewById(R.id.linearLayoutForPostStatusAndVideoIcon);
            viewHolder.playVideoIconImageView = (ImageView) convertView.findViewById(R.id.imageViewForPlayVideoIcon);
            viewHolder.friendImageView = (ImageView) convertView.findViewById(R.id.imageViewForFriendPhoto);
            viewHolder.statusTextView = (TextView) convertView.findViewById(R.id.textViewForSocialWindowStatus);
            viewHolder.statusHeaderTextView = (TextView) convertView.findViewById(R.id.textViewForSocialWindowStatusHeader);
            viewHolder.ribbonLayout = convertView.findViewById(R.id.relativeLayoutForSocialWindowRibbon);
            viewHolder.messageTimeTextView = (TextView) convertView.findViewById(R.id.textViewForSocialWindowMessageTime);
            viewHolder.platformIconImageView = (ImageView) convertView.findViewById(R.id.imageViewForSocialWindowPlatformIcon);
            viewHolder.postBottomBar = convertView.findViewById(R.id.relativeLayoutForSocialWindowBottomBar);
            viewHolder.numberOfLikesIconImageView = (ImageView) convertView.findViewById(R.id.imageViewForSocialWindowNumberOfLikesIcon);
            viewHolder.numberOfLikesTextView = (TextView) convertView.findViewById(R.id.textViewForSocialWindowNumberOfLikes);
            viewHolder.numberOfCommentsIconImageView = (ImageView) convertView.findViewById(R.id.me_card_bio_image);
            viewHolder.numberOfCommentsTextView = (TextView) convertView.findViewById(R.id.textViewForSocialWindowNumberOfComments);
            viewHolder.locationIconImageView = (ImageView) convertView.findViewById(R.id.imageViewForSocialWindowLocationIcon);
            viewHolder.locationTextView = (TextView) convertView.findViewById(R.id.textViewForSocialWindowLocation);

            viewHolder.eventLocationAndDateView = convertView.findViewById(R.id.relativeLayoutForEventLocationAndTime);
            viewHolder.eventDateImageView = (ImageView) convertView.findViewById(R.id.imageViewForSocialWindowEventTime);
            viewHolder.eventLocationImageView = (ImageView) convertView.findViewById(R.id.imageViewForSocialWindowEventLocation);
            viewHolder.eventDateTextView = (TextView) convertView.findViewById(R.id.textViewForSocialWindowEventTime);
            viewHolder.eventLocationTextView = (TextView) convertView.findViewById(R.id.textViewForSocialWindowEventLocation);

            // For card item.
            viewHolder.cardItemContainer = convertView.findViewById(R.id.socialWindowCardItemContainer);
            viewHolder.cardTitleTextView = (TextView) convertView.findViewById(R.id.textViewForCardTitle);
            viewHolder.cardMessageTextView = (TextView) convertView.findViewById(R.id.textViewForCardMessage);
            viewHolder.cardButtonView = convertView.findViewById(R.id.linearLayoutForCardButton);
            viewHolder.cardButtonShadowView = convertView.findViewById(R.id.linearLayoutForCardButtonShadow);
            viewHolder.mergeCardButtonPlatformIconImageView = (ImageView) convertView.findViewById(R.id.imageViewForCardButtonPlatformIcon);
            viewHolder.mergeCardVerticalLineView = convertView.findViewById(R.id.textViewForMergeButtonVerticalLine);
            viewHolder.mergeCardButtonTextTextView = (TextView) convertView.findViewById(R.id.textViewForMergeButtonText);

            // For loading item
            viewHolder.loadingItemContainer = convertView.findViewById(R.id.socialWindowLoadingItemContainer);

            // For me card item
            viewHolder.meCardItemContainer = convertView.findViewById(R.id.socialWindowMeCardItemContainer);
            viewHolder.meCardInnerContainer = convertView.findViewById(R.id.me_card_inner_container);
            viewHolder.meCardTopContainer = convertView.findViewById(R.id.socialWindowMeCardItemContainer);
            viewHolder.meCardPicture = (ImageView) convertView.findViewById(R.id.me_card_picture);
            viewHolder.meCardName = (TextView) convertView.findViewById(R.id.me_card_name);
            viewHolder.meCardTitleContainer = convertView.findViewById(R.id.me_card_title_container);
            viewHolder.meCardTitleText = (TextView) convertView.findViewById(R.id.me_card_title_text);
            viewHolder.meCardBioLayout = convertView.findViewById(R.id.me_card_bio_layout);
            viewHolder.meCardBioText = (TextView) convertView.findViewById(R.id.me_card_bio_text);
            viewHolder.meCardLastSeenLayout = convertView.findViewById(R.id.me_card_last_seen_layout);
            viewHolder.meCardLastSeenText = (TextView) convertView.findViewById(R.id.me_card_last_seen_text);
            viewHolder.meCardSocialProfilesLayout = convertView.findViewById(R.id.me_card_social_profiles_layout);
            viewHolder.meCardSocialProfilesList = (HorizontalListView) convertView.findViewById(R.id.social_profiles_list);
            viewHolder.meCardLikesCount = (TextView) convertView.findViewById(R.id.me_card_likes_count_txt);
            viewHolder.meCardLikesContainer = convertView.findViewById(R.id.me_card_likes_count_layout);
            viewHolder.meCardMutualFriendsListLayout = convertView.findViewById(R.id.me_card_mutual_friends);
            viewHolder.meCardMutualFriendsList = (HorizontalListView) convertView.findViewById(R.id.mutual_friends_list);
            viewHolder.meCardSeparatorBioMutualFriends = convertView.findViewById(R.id.me_card_separator_bio_and_mutual_friends);
            viewHolder.meCardSeparatorMutualFriendsLastSeen = convertView.findViewById(R.id.me_card_separator_mutual_friends_and_last_seen);
            viewHolder.meCardSeparatorLastSeenSocialProfiles = convertView.findViewById(R.id.me_card_separator_last_seen_and_social_profiles);

            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        switch (socialWindowItem.getItemType()) {
            case LOADING:
                applyLoadingItemFunctionality(viewHolder);
                break;
            case POST:
                applyPostItemFunctionality(viewHolder, (SocialWindowPost) socialWindowItem);
                viewHolder.cardButtonView.setOnClickListener(null);
                break;
            case CARD:
                applyCardItemFunctionality(viewHolder, (SocialWindowCard) socialWindowItem);
                viewHolder.cardButtonView.setTag(position);
                viewHolder.cardButtonView.setOnClickListener(this);
                break;
            case ME_CARD:
                if (shouldApplyMeCard)
                    applyMeCardFunctionality(viewHolder, (MeCard) socialWindowItem);
                return convertView;
            default:
                break;
        }

        // We scrolled beyond meCard. so next time we hit it - need to apply.
        shouldApplyMeCard = true;
        return convertView;
    }

    private void applyMeCardFunctionality(ViewHolder viewHolder, MeCard meCard) {
        viewHolder.meCardItemContainer.setVisibility(View.VISIBLE);
        viewHolder.postItemContainer.setVisibility(View.GONE);
        viewHolder.cardItemContainer.setVisibility(View.GONE);
        viewHolder.loadingItemContainer.setVisibility(View.GONE);
        viewHolder.meCardLikesContainer.setVisibility(View.GONE);

        viewHolder.meCardName.setText(meCard.getName());

        viewHolder.meCardTitleText.setText(meCard.getTitle());
        viewHolder.meCardTitleContainer.setVisibility(StringUtils.isBlank(meCard.getTitle()) ? View.GONE : View.VISIBLE);

        viewHolder.meCardBioText.setText(meCard.getBio());
        viewHolder.meCardBioLayout.setVisibility(StringUtils.isBlank(meCard.getBio()) ? View.GONE : View.VISIBLE);

        imageLoaderManager.setDisplayImageOptions(displayImageOptions);

        viewHolder.meCardPicture.setVisibility(View.VISIBLE);

        viewHolder.meCardLastSeenLayout.setVisibility(StringUtils.isBlank(meCard.getLastLocation()) ? View.GONE : View.VISIBLE);
        if(!StringUtils.isBlank(meCard.getLastLocation())) {
            viewHolder.meCardLastSeenText.setText(meCard.getLastLocation());
        }

        if(!StringUtils.isBlank(meCard.getLastLocation())) {
            viewHolder.meCardLastSeenText.setText(meCard.getLastLocation());
        }

        //TODO: is this the right place to put those? should I init the adapter every time?

        meCardProfileAdapter = new MeCardProfileAdapter(context, (java.util.ArrayList<MeCard.SocialProfile>)
                meCard.getSocialProfiles());

        viewHolder.meCardSocialProfilesLayout.setVisibility(meCard.getSocialProfiles() == null ||
                meCard.getSocialProfiles().isEmpty() ? View.GONE : View.VISIBLE);

        viewHolder.meCardSocialProfilesList.setAdapter(meCardProfileAdapter);
        viewHolder.meCardSocialProfilesList.setOnItemClickListener(onSocialProfileClick());

        viewHolder.meCardLikesContainer.setVisibility(StringUtils.isBlank(meCard.getLikesCount()) ?
                View.GONE : View.VISIBLE);
        viewHolder.meCardLikesCount.setText(
                String.format(context.getString(R.string.me_card_likes), meCard.getLikesCount()));

        viewHolder.meCardMutualFriendsListLayout.setVisibility(meCard.getMutualFriends() == null ||
                        meCard.getMutualFriends().isEmpty() ? View.GONE : View.VISIBLE);

        meCardMutualFriendsAdapter = new MeCardMutualFriendsAdapter(context, (ArrayList<MeCard.MutualFriend>)meCard.getMutualFriends());

        viewHolder.meCardMutualFriendsList.setAdapter(meCardMutualFriendsAdapter);
        viewHolder.meCardMutualFriendsList.setOnItemClickListener(onMutualFriendClick());

        //viewHolder.meCardPicture.setVisibility(StringUtils.isBlank(meCard.getPictureUrl())? View.GONE : View.VISIBLE);
        if (meCard.getPictureUrl() != null && !StringUtils.isBlank(meCard.getPictureUrl())) {
            loadImage(viewHolder.meCardPicture, meCard.getPictureUrl());
        }

        viewHolder.meCardSeparatorBioMutualFriends.setVisibility(viewHolder.meCardBioLayout.getVisibility());
        viewHolder.meCardSeparatorMutualFriendsLastSeen.setVisibility(viewHolder.meCardMutualFriendsListLayout.getVisibility());
        viewHolder.meCardSeparatorLastSeenSocialProfiles.setVisibility(viewHolder.meCardLastSeenLayout.getVisibility());

        changeLoadingAndCardSocialWindowHeight(viewHolder.itemView, viewHolder.meCardItemContainer.getHeight());

        shouldApplyMeCard = false;
    }


    private AdapterView.OnItemClickListener onSocialProfileClick() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                profileClicked = true;
                MeCard.SocialProfile profile = (MeCard.SocialProfile) adapterView.getAdapter().getItem(i);
                openWebBrowser(profile.getUrl());

            }
        };
    }

    private AdapterView.OnItemClickListener onMutualFriendClick() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                profileClicked = true;
                String id = ((MeCard.MutualFriend)adapterView.getAdapter().getItem(i)).getId();
                Intent facebookPostIntent = new Intent(Intent.ACTION_VIEW);
                facebookPostIntent.setData(Uri.parse("fb://profile/" + id));
                facebookPostIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(facebookPostIntent);
            }
        };
    }

    private void openWebBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(browserIntent);
    }


    /**
     * Apply needed functionality when it's loading item.
     *
     * @param viewHolder holder for views in social item layout.
     */
    private void applyLoadingItemFunctionality(ViewHolder viewHolder) {

        viewHolder.loadingItemContainer.setVisibility(View.VISIBLE);
        viewHolder.postItemContainer.setVisibility(View.GONE);
        viewHolder.cardItemContainer.setVisibility(View.GONE);
        viewHolder.meCardItemContainer.setVisibility(View.GONE);

        changeLoadingAndCardSocialWindowHeight(viewHolder.itemView, (int) context.getResources().getDimension(R.dimen.social_window_text_only_item_height));
    }


    /**
     * Apply needed functionality when it's post item.
     *
     * @param viewHolder holder for views in social item layout.
     * @param post social item.
     */
    private void applyPostItemFunctionality(final ViewHolder viewHolder, SocialWindowPost post) {

        viewHolder.postItemContainer.setVisibility(View.VISIBLE);
        viewHolder.cardItemContainer.setVisibility(View.GONE);
        viewHolder.loadingItemContainer.setVisibility(View.GONE);
        viewHolder.meCardItemContainer.setVisibility(View.GONE);

        viewHolder.statusTextView.setText(post.getStatusBody());
        viewHolder.messageTimeTextView.setText(post.getCreatedDate());
        viewHolder.coverImageView.setBackgroundResource(0);

        changeLoadingAndCardSocialWindowHeight(viewHolder.itemView, (int) AbsListView.LayoutParams.WRAP_CONTENT);

        // Handle different posts types (Facebook, Twitter, Instagram).
        switch (post.getCoverPageStatusPlatform()) {

            case FACEBOOK:

                applyFacebookTheme(viewHolder, post);
                break;

            case TWITTER:

                applyTwitterTheme(viewHolder, post);
                break;

            case INSTAGRAM:

                applyInstagramTheme(viewHolder, post);
                break;
        }


        // Handle if post location not provided.
        if (post.hasLocation()) {

            viewHolder.locationIconImageView.setVisibility(View.VISIBLE);
            viewHolder.locationTextView.setText("" + post.getLocation().getCity());

        } else {

            viewHolder.locationIconImageView.setVisibility(View.INVISIBLE);
            viewHolder.locationTextView.setText("");
        }

        // Load post cover.
        if (post.hasPicture()) {

            applyTextWithImageTheme(viewHolder);

            if (isCoverImageLoadingEnabled) {

                if (post.getCoverPageStatusPlatform() == Platform.INSTAGRAM) {
                    imageLoaderManager.setDisplayImageOptions(displayImageOptions);
                } else if (post.getContentType() == StatusContent.VIDEO || post.getContentType() == StatusContent.PHOTO || post.getTag() == PostTypeTag.EVENT) {
                    imageLoaderManager.setDisplayImageOptions(displayImageOptions);
                } else {
                    imageLoaderManager.setDisplayImageOptions(displayImageOptions);
                }
                loadImage(viewHolder.coverImageView, post.getPictureUrl());

            }

        } else {

            applyTextOnlyTheme(viewHolder);

            viewHolder.coverImageView.setImageResource(R.color.gray);
        }

        if (post.getUser() != null) {

            imageLoaderManager.setDisplayImageOptions(displayRoundedImageOptions);
            if (post.getUser().getPhotoUri() != null)
                loadImage(viewHolder.friendImageView, post.getUser().getPhotoUri().toString());

        } else if (post.getTag() == PostTypeTag.TAGGED_IN) {

            FacebookPost fbPost = ((FacebookPost) post);
            String userId = fbPost.getTaggedUsers().get(fbPost.getTaggedUsers().size() - 1).getId();

            FacebookAuthenticator.getInstance(context).getProfile(userId, new FacebookTagUserLoader(viewHolder, post));
        }

        // Apply changes on post video type.
        if (post.getContentType() == StatusContent.VIDEO) {

            applyVideoTheme(viewHolder);

        } else {

            removeVideoThemeChanges(viewHolder);
        }
    }



    /**
     * Apply changes on view when the social item text only.
     *
     * @param viewHolder holder for views in social item layout.
     */
    private void applyTextOnlyTheme(ViewHolder viewHolder) {

        viewHolder.coverImageView.setVisibility(View.GONE);

        viewHolder.statusTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_status_color));
        viewHolder.statusHeaderTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_status_color));

        viewHolder.messageTimeTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_message_time_text_color));
        //viewHolder.lineSeparatorTextView.setBackgroundResource(R.color.social_window_text_only_item_horizontal_line_color);
        viewHolder.socialWindowContentAboveCoverImage.setBackgroundResource(0);

        // Align parent to top of item.
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.statusAndVideoIconContainer.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.relativeLayoutForSocialWindowRibbon);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        viewHolder.statusAndVideoIconContainer.setLayoutParams(layoutParams);
    }


    /**
     * Apply changes on view when social item is text with image.
     *
     * @param viewHolder holder for views in social item layout.
     */
    private void applyTextWithImageTheme(ViewHolder viewHolder) {

        viewHolder.coverImageView.setVisibility(View.VISIBLE);

        viewHolder.statusTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_status_color));
        viewHolder.statusHeaderTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_status_color));

        viewHolder.messageTimeTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_message_time_text_color));
        //viewHolder.lineSeparatorTextView.setBackgroundResource(R.color.social_window_text_only_item_horizontal_line_color);
        viewHolder.socialWindowContentAboveCoverImage.setBackgroundResource(0);

        // Align parent to top of item.
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.statusAndVideoIconContainer.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.relativeLayoutForSocialWindowRibbon);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        viewHolder.statusAndVideoIconContainer.setLayoutParams(layoutParams);

    }


    /**
     * Apply changes on social window components when the post type is video.
     *
     * @param viewHolder holder for views in social item layout.
     */
    private void applyVideoTheme(ViewHolder viewHolder) {

        // Align parent to top of item.
        viewHolder.statusTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_status_color));
        viewHolder.statusHeaderTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_status_color));

        viewHolder.messageTimeTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_message_time_text_color));
        //viewHolder.lineSeparatorTextView.setBackgroundResource(R.color.social_window_text_only_item_horizontal_line_color);
        viewHolder.socialWindowContentAboveCoverImage.setBackgroundResource(0);

        // Align parent to top of item.
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.statusAndVideoIconContainer.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.relativeLayoutForSocialWindowRibbon);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        viewHolder.statusAndVideoIconContainer.setLayoutParams(layoutParams);

        // Show video icon.
        viewHolder.playVideoIconImageView.setVisibility(View.VISIBLE);
    }


    /**
     * Remove video changes when social window type not video.
     *
     * @param viewHolder holder for views in social item layout.
     */
    private void removeVideoThemeChanges(ViewHolder viewHolder) {

        // Align parent to top of item.
        viewHolder.statusTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_status_color));
        viewHolder.statusHeaderTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_status_color));

        viewHolder.messageTimeTextView.setTextColor(context.getResources().getColor(R.color.social_window_text_only_item_message_time_text_color));
        //viewHolder.lineSeparatorTextView.setBackgroundResource(R.color.social_window_text_only_item_horizontal_line_color);
        viewHolder.socialWindowContentAboveCoverImage.setBackgroundResource(0);

        // Align parent to top of item.
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.statusAndVideoIconContainer.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.relativeLayoutForSocialWindowRibbon);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        viewHolder.statusAndVideoIconContainer.setLayoutParams(layoutParams);

        // Remove video icon.
        viewHolder.playVideoIconImageView.setVisibility(View.GONE);
    }


    /**
     * Apply changes on social item view when the post from Facebook.
     *
     * @param viewHolder holder for views in social item layout.
     * @param post       social post.
     */
    private void applyFacebookTheme(ViewHolder viewHolder, SocialWindowPost post) {

        viewHolder.ribbonLayout.setBackgroundResource(android.R.color.transparent);
        viewHolder.platformIconImageView.setImageResource(R.drawable.sw_icon_fb);

        FacebookPost facebookPost = (FacebookPost) post;

        Spanned statusHeader = Html.fromHtml(facebookPost.getStory().replaceAll("\\u0000", ""));
        Spanned status = Html.fromHtml(facebookPost.getStatusBody());

        viewHolder.statusHeaderTextView.setText(statusHeader);
        viewHolder.statusTextView.setText(status);

        if (facebookPost.getTag() == PostTypeTag.EVENT || facebookPost.getContentType() == StatusContent.VIDEO || facebookPost.getContentType() == StatusContent.PHOTO) {
            viewHolder.coverImageView.setVisibility(View.VISIBLE);
            viewHolder.statusTextView.setVisibility(View.VISIBLE);

            changeSocialWindowHeight(viewHolder.coverImageLayout,
                    (int) context.getResources().getDimension(R.dimen.social_window_text_with_image_item_height));

            int imageViewMarginLeft = (int) context.getResources().getDimension(R.dimen.social_window_post_image_margin_left);
            int imageViewMarginRight = (int) context.getResources().getDimension(R.dimen.social_window_post_image_margin_right);
            int imageViewMarginTop = (int) context.getResources().getDimension(R.dimen.social_window_post_image_margin_top);
            int imageViewMarginBottom = (int) context.getResources().getDimension(R.dimen.social_window_post_image_margin_bottom);
            if (facebookPost.getTag() == PostTypeTag.EVENT) {
                imageViewMarginBottom = 0;
            }
            changeLayoutMargin(viewHolder.coverImageLayout, imageViewMarginLeft, imageViewMarginRight, imageViewMarginTop, imageViewMarginBottom);

            if (facebookPost.getTag() == PostTypeTag.EVENT) {

                viewHolder.eventLocationAndDateView.setVisibility(View.VISIBLE);
                viewHolder.coverImageView.setBackgroundResource(R.drawable.social_window_event_border);
                viewHolder.coverImageView.setPadding(
                        (int) context.getResources().getDimension(R.dimen.social_window_facebook_event_border_width),
                        (int) context.getResources().getDimension(R.dimen.social_window_facebook_event_border_width),
                        (int) context.getResources().getDimension(R.dimen.social_window_facebook_event_border_width),
                        (int) context.getResources().getDimension(R.dimen.social_window_facebook_event_border_width));
                changeSocialWindowHeight(viewHolder.coverImageLayout, (int) context.getResources().getDimension(
                                R.dimen.social_window_facebook_event_image_height));

                if (facebookPost.hasLocation()) {

                    viewHolder.eventLocationImageView.setVisibility(View.VISIBLE);
                    viewHolder.eventLocationTextView.setText(facebookPost.getLocation().getName());
                } else {

                    viewHolder.eventLocationImageView.setVisibility(View.GONE);
                    viewHolder.locationTextView.setText("");
                }
                viewHolder.eventDateTextView.setText(getFormattedDateForFacebookEvents(facebookPost.getPublishTime()));
            } else {
                viewHolder.eventLocationAndDateView.setVisibility(View.GONE);
            }
            viewHolder.statusHeaderTextView.setVisibility(View.VISIBLE);
            viewHolder.friendImageView.setVisibility(View.GONE);
            if (post.getTag() == PostTypeTag.TAGGED_IN) {
                viewHolder.friendImageView.setVisibility(View.VISIBLE);
            }

        } else {
            viewHolder.statusHeaderTextView.setVisibility(View.VISIBLE);
            viewHolder.eventLocationAndDateView.setVisibility(View.GONE);

            if (post.getTag() == PostTypeTag.TAGGED_IN || post.hasPicture()) {

                if (post.getTag() == PostTypeTag.TAGGED_IN) {
                    viewHolder.statusTextView.setVisibility(View.VISIBLE);
                    viewHolder.statusHeaderTextView.setVisibility(View.VISIBLE);
                    viewHolder.friendImageView.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.friendImageView.setVisibility(View.GONE);
                }

                viewHolder.coverImageView.setVisibility(View.VISIBLE);
                changeSocialWindowHeight(viewHolder.coverImageLayout, (int) context.getResources().getDimension(R.dimen.social_window_text_with_image_item_height));
                changeLayoutMargin(viewHolder.coverImageLayout, 0, 0, 0, 0);

            } else {
                viewHolder.statusTextView.setVisibility(View.VISIBLE);
                viewHolder.friendImageView.setVisibility(View.GONE);
                viewHolder.coverImageView.setVisibility(View.GONE);
                viewHolder.playVideoIconImageView.setVisibility(View.GONE);
                changeSocialWindowHeight(viewHolder.coverImageLayout, ViewGroup.LayoutParams.WRAP_CONTENT);
                changeLayoutMargin(viewHolder.coverImageLayout, 0, 0, 0, 0);
            }
        }

        viewHolder.postBottomBar.setVisibility(View.GONE);
        if (post.getTag() == PostTypeTag.LIKES_LINK) {
            changeSocialWindowHeight(viewHolder.coverImageLayout, (int) context.getResources().getDimension(R.dimen.social_window_like_image_height));
            changeLayoutMargin(viewHolder.coverImageLayout, 0, 0, 0, 5);
        } else if (facebookPost.getTag() != PostTypeTag.EVENT && (facebookPost.getLikesCount() > 0 || facebookPost.getCommentsCount() > 0 || facebookPost.hasLocation())) {
            viewHolder.postBottomBar.setVisibility(View.VISIBLE);
            viewHolder.numberOfLikesTextView.setText("" + facebookPost.getLikesCount());
            viewHolder.numberOfCommentsTextView.setText("" + facebookPost.getCommentsCount());
            viewHolder.numberOfLikesIconImageView.setImageResource(R.drawable.sw_icon_fbpost_like);
            viewHolder.numberOfCommentsIconImageView.setImageResource(R.drawable.sw_icon_fbpost_comment);
        }

        if (StringUtils.isBlank(status)) {
            viewHolder.statusTextView.setVisibility(View.GONE);
        }
        if (StringUtils.isBlank(statusHeader)) {
            viewHolder.statusHeaderTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Return a date string like: Sat,Nov 22 at 9:00 PM.
     */
    private String getFormattedDateForFacebookEvents(Date publishTime) {

        String patternDate = "EEE,MMM dd";
        String patternTime = "hh:mm aa";
        SimpleDateFormat formatDate = new SimpleDateFormat(patternDate);
        SimpleDateFormat formatTime = new SimpleDateFormat(patternTime);
        String formattedDate = formatDate.format(publishTime.getTime());
        String formattedTime = formatTime.format(publishTime.getTime());
        return formattedDate + " at " + formattedTime;
    }


    /**
     * Apply changes on social item view when the post from Twitter.
     *
     * @param viewHolder holder for view in social item layout.
     * @param post       social post.
     */

    private void applyTwitterTheme(ViewHolder viewHolder, SocialWindowPost post) {

        //viewHolder.ribbonLayout.setBackgroundResource(R.color.social_window_twitter_text_only_item_ribbon_background_color);
        viewHolder.ribbonLayout.setBackgroundResource(android.R.color.transparent);

        viewHolder.platformIconImageView.setImageResource(R.drawable.sw_icon_twitter);

        TwitterPost tweet = (TwitterPost) post;

        //Enable the Like - comments view.
        viewHolder.postBottomBar.setVisibility(View.VISIBLE);
        viewHolder.numberOfLikesTextView.setText("" + tweet.getFavoritesCount());
        viewHolder.numberOfCommentsTextView.setText("" + tweet.getRetweetsCount());
        viewHolder.numberOfLikesIconImageView.setImageResource(R.drawable.sw_icon_twitterpost_retweet);
        viewHolder.numberOfCommentsIconImageView.setImageResource(R.drawable.sw_icon_twitterpost_fav);

        //For facebook only.
        viewHolder.statusHeaderTextView.setVisibility(View.GONE);
        viewHolder.eventLocationAndDateView.setVisibility(View.GONE);

        viewHolder.statusTextView.setVisibility(View.VISIBLE);

        //Enable the original writer photo if its a retweet.
        if (post.getUser() != null) {
            viewHolder.friendImageView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.friendImageView.setVisibility(View.GONE);
        }

        // Change the height for social window depends on the photo status.
        if (post.hasPicture()) {

            viewHolder.coverImageView.setVisibility(View.VISIBLE);

            changeSocialWindowHeight(viewHolder.coverImageLayout, (int) context.getResources().getDimension(R.dimen.social_window_text_with_image_item_height));

            int imageViewMarginLeft = (int) context.getResources().getDimension(R.dimen.social_window_post_image_margin_left);
            int imageViewMarginRight = (int) context.getResources().getDimension(R.dimen.social_window_post_image_margin_right);
            int imageViewMarginTop = (int) context.getResources().getDimension(R.dimen.social_window_post_image_margin_top);
            int imageViewMarginBottom = (int) context.getResources().getDimension(R.dimen.social_window_post_image_margin_bottom);
            changeLayoutMargin(viewHolder.coverImageLayout, imageViewMarginLeft, imageViewMarginRight, imageViewMarginTop, imageViewMarginBottom);

        } else {

            viewHolder.coverImageView.setVisibility(View.GONE);
            viewHolder.playVideoIconImageView.setVisibility(View.GONE);
            changeSocialWindowHeight(viewHolder.coverImageLayout, ViewGroup.LayoutParams.WRAP_CONTENT);
            changeLayoutMargin(viewHolder.coverImageLayout, 0, 0, 0, 0);

        }
    }


    /**
     * Apply changes on social item view when the post from Instagram.
     *
     * @param viewHolder holder for view in social item layout.
     * @param post       social post.
     */
    private void applyInstagramTheme(ViewHolder viewHolder, SocialWindowPost post) {

        viewHolder.platformIconImageView.setImageResource(R.drawable.sw_icon_instagram);

        viewHolder.ribbonLayout.setBackgroundResource(android.R.color.transparent);

        InstagramPost status = (InstagramPost) post;

        //Always visible for instagram posts.
        viewHolder.coverImageView.setVisibility(View.VISIBLE);
        viewHolder.statusTextView.setVisibility(View.VISIBLE);

        //Use with facebook only.
        viewHolder.statusHeaderTextView.setVisibility(View.GONE);
        viewHolder.eventLocationAndDateView.setVisibility(View.GONE);
        viewHolder.friendImageView.setVisibility(View.GONE);

        // Use the comments & likes Facebook icons for Instagram.
        viewHolder.postBottomBar.setVisibility(View.VISIBLE);
        viewHolder.numberOfLikesTextView.setText("" + status.getLikesCount());
        viewHolder.numberOfCommentsTextView.setText("" + status.getCommentsCount());
        viewHolder.numberOfLikesIconImageView.setImageResource(R.drawable.sw_icon_fbpost_like);
        viewHolder.numberOfCommentsIconImageView.setImageResource(R.drawable.sw_icon_fbpost_comment);

        changeSocialWindowHeight(viewHolder.coverImageLayout, (int) context.getResources().getDimension(R.dimen.social_window_instagram_item_height));
        changeLayoutMargin(viewHolder.coverImageLayout, 0, 0, 0, 0);

    }


    /**
     * Apply needed functionality when it's card item (merge/connect).
     *
     * @param viewHolder       holder for views in social item layout.
     * @param card             social card.
     */
    private void applyCardItemFunctionality(ViewHolder viewHolder, SocialWindowCard card) {
        viewHolder.cardItemContainer.setVisibility(View.VISIBLE);
        viewHolder.loadingItemContainer.setVisibility(View.GONE);
        viewHolder.postItemContainer.setVisibility(View.GONE);
        viewHolder.meCardItemContainer.setVisibility(View.GONE);

        switch (card.getType()) {

            // Connect card.
            case CONNECT:
                applyConnectCardTheme(card, viewHolder);
                break;

            // Merge card.
            case MERGE:
                applyMergeCardTheme(card, viewHolder);
                break;
        }

        changeLoadingAndCardSocialWindowHeight(viewHolder.itemView, (int) context.getResources().getDimension(R.dimen.social_window_text_only_item_height));
    }


    /**
     * Apply changes on card UI when card type is connect.
     *
     * @param card       card post item.
     * @param viewHolder holder for views in social item layout.
     */
    private void applyConnectCardTheme(SocialWindowCard card, ViewHolder viewHolder) {

        viewHolder.cardTitleTextView.setText(String.format(
                context.getResources().getString(R.string.social_window_connect_to_platform_card_title),
                card.getPlatform()).toUpperCase());

        viewHolder.cardMessageTextView.setText(String.format(
                context.getResources().getString(R.string.social_window_connect_to_platform_card_message),
                card.getPlatform(), card.getFriendName()));

        viewHolder.cardButtonView.setBackgroundResource(getConnectButtonImage(card.getPlatform()));

        // Set card button dimensions to wrap content.
        ViewGroup.LayoutParams cardButtonLayoutParams;
        cardButtonLayoutParams = viewHolder.cardButtonView.getLayoutParams();
        cardButtonLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        cardButtonLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        viewHolder.cardButtonView.setLayoutParams(cardButtonLayoutParams);

        // Remove shadow
        viewHolder.cardButtonShadowView.setBackgroundResource(0);

        // Hide find button views.
        viewHolder.mergeCardButtonPlatformIconImageView.setVisibility(View.GONE);
        viewHolder.mergeCardVerticalLineView.setVisibility(View.GONE);
        viewHolder.mergeCardButtonTextTextView.setVisibility(View.GONE);
    }


    /**
     * Apply changes on card UI when card type is merge.
     *
     * @param card       card post item.
     * @param viewHolder holder for views in social item layout.
     */
    private void applyMergeCardTheme(SocialWindowCard card, ViewHolder viewHolder) {

        FindButtonTheme findButtonTheme = getFindButtonTheme(card.getPlatform());
        String platformName = card.getPlatform().getName();

        viewHolder.cardTitleTextView.setText(String.format(
                context.getResources().getString(R.string.social_window_merge_platform_card_title), StringUtils.getFirstName(card.getFriendName()).toUpperCase(),
                platformName.toUpperCase()));

        viewHolder.cardMessageTextView.setText(String.format(
                context.getResources().getString(R.string.social_window_merge_platform_card_message),
                card.getFriendName(), StringUtils.capitalize(platformName)));

        viewHolder.mergeCardButtonPlatformIconImageView.setImageResource(findButtonTheme.platformIcon);
        // Change find button background color depends on the card platform.
        GradientDrawable cardButtonBackground = UIUtils.getRoundedBackground(findButtonTheme.buttonColor,
                context.getResources().getDimension(R.dimen.social_window_merge_card_corners_radius));
        viewHolder.cardButtonView.setBackground(cardButtonBackground);


        // Set static dimensions to card button.
        ViewGroup.LayoutParams cardButtonLayoutParams = viewHolder.cardButtonView.getLayoutParams();
        cardButtonLayoutParams.height = (int) context.getResources().getDimension(R.dimen.social_window_merge_card_button_height);
        cardButtonLayoutParams.width = (int) context.getResources().getDimension(R.dimen.social_window_merge_card_button_width);
        viewHolder.cardButtonView.setLayoutParams(cardButtonLayoutParams);


        // Set background for find button view container to work as shadow.
        GradientDrawable cardButtonViewBorder = UIUtils.getRoundedBackground(findButtonTheme.shadowColor,
                context.getResources().getDimension(R.dimen.social_window_merge_card_corners_radius));
        viewHolder.cardButtonShadowView.setBackground(cardButtonViewBorder);

        // Update find button text.
        viewHolder.mergeCardButtonTextTextView.setText(
                String.format(context.getResources().getString(R.string.social_window_merge_platform_card_find_button_text),
                        StringUtils.getFirstName(card.getFriendName()))
        );

        // Show find button views.
        viewHolder.mergeCardButtonPlatformIconImageView.setVisibility(View.VISIBLE);
        viewHolder.mergeCardVerticalLineView.setVisibility(View.VISIBLE);
        viewHolder.mergeCardButtonTextTextView.setVisibility(View.VISIBLE);
    }


    /**
     * Return connect image id for given platform.
     *
     * @param platform card platform.
     * @return connect image id.
     */
    private int getConnectButtonImage(Platform platform) {

        switch (platform) {

            case TWITTER:
                return R.drawable.connect_twitter;

            case INSTAGRAM:
                return R.drawable.connect_instagram;

            case HANGOUTS:
                return R.drawable.connect_hangouts;
        }

        return 0;
    }


    /**
     * Get theme object containing resources for merge card find button.
     *
     * @param platform merge card platform.
     * @return theme object for merge card find button.
     */
    private FindButtonTheme getFindButtonTheme(Platform platform) {

        FindButtonTheme findButtonTheme = new FindButtonTheme();

        switch (platform) {

            case FACEBOOK:
                findButtonTheme.buttonColor = context.getResources().getColor(R.color.social_window_card_item_find_button_facebook_color);
                findButtonTheme.shadowColor = context.getResources().getColor(R.color.social_window_card_item_find_button_shadow_line_facebook_color);
                findButtonTheme.platformIcon = R.drawable.find_fb_icon;
                break;

            case HANGOUTS:
                findButtonTheme.buttonColor = context.getResources().getColor(R.color.social_window_card_item_find_button_hangouts_color);
                findButtonTheme.shadowColor = context.getResources().getColor(R.color.social_window_card_item_find_button_shadow_line_hangouts_color);
                findButtonTheme.platformIcon = R.drawable.find_hangouts_icon;
                break;

            case TWITTER:
                findButtonTheme.buttonColor = context.getResources().getColor(R.color.social_window_card_item_find_button_twitter_color);
                findButtonTheme.shadowColor = context.getResources().getColor(R.color.social_window_card_item_find_button_shadow_line_twitter_color);
                findButtonTheme.platformIcon = R.drawable.find_twitter_icon;
                break;

            case INSTAGRAM:
                findButtonTheme.buttonColor = context.getResources().getColor(R.color.social_window_card_item_find_button_instagram_color);
                findButtonTheme.shadowColor = context.getResources().getColor(R.color.social_window_card_item_find_button_shadow_line_instagram_color);
                findButtonTheme.platformIcon = R.drawable.find_instagram_icon;
                break;
        }

        return findButtonTheme;
    }


    /**
     * Change the height for social window layout and the list item view.
     *
     * @param itemView social window list item view.
     * @param height   new height for social window.
     */
    private void changeSocialWindowHeight(View itemView, int height) {

        // Change social window list item height.
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) itemView.getLayoutParams();
        layoutParams.height = height;
        itemView.setLayoutParams(layoutParams);
    }

    private void changeLoadingAndCardSocialWindowHeight(View itemView, int height) {

        // Change social window list item height.
        AbsListView.LayoutParams layoutParams = (AbsListView.LayoutParams) itemView.getLayoutParams();
        layoutParams.height = height;
        itemView.setLayoutParams(layoutParams);
    }

    /**
     * Change the height for social window layout and the list item view.
     *
     * @param itemView social window list item view.
     */
    private void changeLayoutMargin(View itemView, int marginLeft, int marginRight, int marginTop, int marginBottom) {

        // Change social window list item height.
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) itemView.getLayoutParams();
        layoutParams.leftMargin = marginLeft;
        layoutParams.rightMargin = marginRight;
        layoutParams.topMargin = marginTop;
        layoutParams.bottomMargin = marginBottom;
        itemView.setLayoutParams(layoutParams);
    }


    /**
     * Check the give image URL if it's locally, load the image directly. If not
     * load it using universal image loader.
     *
     * @param imageView image view to load image in.
     * @param imageUrl  image URL to be loaded.
     */
    private void loadImage(ImageView imageView, String imageUrl) {
        if (imageUrl.contains(ImageUtils.DRAWABLE_PATH)) {
            imageView.setImageDrawable(context.getResources()
                    .getDrawable(getImageIdFromDrawablePictureUri(imageUrl)));
        } else {
            imageLoaderManager
                    .loadImage(imageView, Uri.parse(imageUrl));
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


    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            // Card button.
            case R.id.linearLayoutForCardButton:

                onCardItemButtonClicked.onCardItemButtonClicked((Integer) view.getTag());
                break;
        }
    }


    /**
     * Used to centralize updating resources for merge card find button depends
     * on the card platform.
     */
    private static class FindButtonTheme {

        public int buttonColor;
        public int shadowColor;
        public int platformIcon;
    }


    /**
     * Work as holder for unified social item view in order to decrease the time of creating
     * list-view items.
     */
    private static class ViewHolder {

        public View itemView;

        // For post item.
        public View postItemContainer;
        public View coverImageLayout;
        public ImageView coverImageView;
        public View socialWindowContentAboveCoverImage;
        public View statusAndVideoIconContainer;
        public ImageView playVideoIconImageView;
        public ImageView friendImageView;
        public TextView statusTextView;
        public TextView statusHeaderTextView;
        public View ribbonLayout;
        public TextView messageTimeTextView;
        public ImageView platformIconImageView;

        public View postBottomBar;
        public ImageView numberOfLikesIconImageView;
        public TextView numberOfLikesTextView;
        public ImageView numberOfCommentsIconImageView;
        public TextView numberOfCommentsTextView;
        public ImageView locationIconImageView;
        public TextView locationTextView;

        public View eventLocationAndDateView;
        public ImageView eventDateImageView;
        public TextView eventDateTextView;
        public ImageView eventLocationImageView;
        public TextView eventLocationTextView;

        // For card item
        public View cardItemContainer;
        public TextView cardTitleTextView;
        public TextView cardMessageTextView;
        public View cardButtonView;
        public View cardButtonShadowView;
        public ImageView mergeCardButtonPlatformIconImageView;
        public View mergeCardVerticalLineView;
        public TextView mergeCardButtonTextTextView;


        // For loading item
        public View loadingItemContainer;

        // For me card
        public View meCardItemContainer;
        public View meCardInnerContainer;
        public View meCardTopContainer;
        public ImageView meCardPicture;
        public TextView meCardName;
        public View meCardTitleContainer;
        public TextView meCardTitleText;
        public View meCardBioLayout;
        public TextView meCardBioText;
        public View meCardLastSeenLayout;
        public TextView meCardLastSeenText;
        public View meCardSocialProfilesLayout;
        public HorizontalListView meCardSocialProfilesList;
        public View meCardLikesContainer;
        public TextView meCardLikesCount;
        public View meCardMutualFriendsListLayout;
        public HorizontalListView meCardMutualFriendsList;

        public View meCardSeparatorBioMutualFriends;
        public View meCardSeparatorMutualFriendsLastSeen;
        public View meCardSeparatorLastSeenSocialProfiles;

    }


    /**
     * Used to send click event on card item button.
     */
    public interface OnCardItemButtonClicked {

        /**
         * Called when card item button clicked.
         *
         * @param position position of selected card in the list.
         */
        public void onCardItemButtonClicked(int position);
    }


    // ------------------------------ Getters & Setters -----------------------------

    /**
     * Used to Enable/Disable cover images loading.
     *
     * @param status true to enable cover image loading, false to disable cover image loading.
     */
    public void setCoverImageLoadingEnabled(boolean status) {
        this.isCoverImageLoadingEnabled = status;
    }


    private class FacebookTagUserLoader implements Authenticator.ProfileRequestCallback {

        private SocialWindowPost post;
        private ViewHolder viewHolder;

        public FacebookTagUserLoader(ViewHolder viewHolder, SocialWindowPost post) {
            this.post = post;
            this.viewHolder = viewHolder;
        }

        @Override
        public void onException(String msg, Exception e) {
            viewHolder.friendImageView.setImageResource(R.color.gray);
        }

        @Override
        public void onFail() {
            viewHolder.friendImageView.setImageResource(R.color.gray);
        }

        @Override
        public void onGettingProfile() {
            viewHolder.friendImageView.setImageResource(R.color.gray);
        }

        @Override
        public void onComplete(Contact user) {

            post.setUser(user);

            if (post.getUser().getPhotoUri() == null) {
                viewHolder.friendImageView.setImageResource(R.color.gray);
            } else {
                imageLoaderManager.setDisplayImageOptions(displayRoundedImageOptions);
                loadImage(viewHolder.friendImageView, user.getPhotoUri().toString());
            }

        }
    }


}
