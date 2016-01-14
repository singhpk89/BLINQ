package com.blinq.ui.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.models.Platform;
import com.blinq.authentication.Authenticator.PublishRequestCallBack;
import com.blinq.authentication.impl.Twitter.TwitterAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.facebook.Mappers.FeedMapper;
import com.blinq.authentication.impl.Google.GooglePlusAuthenticator;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

/**
 * Custom dialog builder that offer a platform set to user to select from.
 *
 * @author Johan Hansson.
 */
public class ShareDialogBuilder extends Builder implements OnClickListener {

    private static final String TAG = ShareDialogBuilder.class.getSimpleName();

    private Activity activity;
    /**
     * The dialog layout.
     */
    private View dialogView;

    /**
     * Dialog title.
     */
    private TextView title;

    /**
     * Dialog message.
     */
    private TextView message;

    private Button facebookButton;
    private Button googlePlusButton;
    private Button twitterButton;
    private Button instagramButton;
    private String clickedText;

    AlertDialog alertDialog;

    private BlinqAnalytics analyticsManager;

    /**
     * clickedText is used for analytics
     */
    public ShareDialogBuilder(Activity activity, String clickedText) {
        super(activity);

        this.activity = activity;
        this.clickedText = clickedText;

        analyticsManager = new BlinqAnalytics(
                activity.getApplicationContext());
        // Build the layout of the dialog.
        dialogView = View.inflate(activity, R.layout.spread_the_love_dialog,
                null);
        setView(dialogView);

        title = (TextView) dialogView.findViewById(R.id.textViewForDialogTitle);
        message = (TextView) dialogView
                .findViewById(R.id.textViewForDialogMessage);
        facebookButton = (Button) dialogView
                .findViewById(R.id.facebook_platform);
        googlePlusButton = (Button) dialogView
                .findViewById(R.id.googleplus_platform);
        twitterButton = (Button) dialogView.findViewById(R.id.twitter_platform);
        instagramButton = (Button) dialogView
                .findViewById(R.id.instagram_platform);
        facebookButton.setOnClickListener(this);
        googlePlusButton.setOnClickListener(this);
        twitterButton.setOnClickListener(this);
        instagramButton.setOnClickListener(this);
        instagramButton.setVisibility(View.GONE);
    }

    @Override
    public ShareDialogBuilder setTitle(CharSequence text) {
        title.setText(text);
        return this;
    }

    @Override
    public AlertDialog create() {
        alertDialog = super.create();
        return alertDialog;
    }

    /**
     * @param colorString color number in string format like "#FFAABB".
     */
    public ShareDialogBuilder setTitleColor(String colorString) {
        title.setTextColor(Color.parseColor(colorString));
        return this;
    }

    @Override
    public ShareDialogBuilder setMessage(CharSequence text) {
        message.setText(text);
        return this;
    }

    @Override
    public AlertDialog show() {

        // Check if there is no title, remove the title layout.
        if (title.getText().equals(""))
            dialogView.findViewById(R.id.textViewForDialogTitle).setVisibility(
                    View.GONE);
        return super.show();


    }

    @Override
    public void onClick(View v) {

        boolean connected = false;


        switch (v.getId()) {

            case R.id.facebook_platform:

                if (FacebookAuthenticator.getInstance(activity).isConnected()) {

                    final FeedMapper feed = new FeedMapper.Builder()
                            .setMessage("")
                            .setName(activity.getString(R.string.app_name))
                            .setCaption(activity.getString(R.string.share_link))
                            .setDescription(
                                    activity.getString(R.string.share_message))
                            .setPicture(activity.getString(R.string.share_picture))
                            .setLink(activity.getString(R.string.share_link))
                            .build();

                    connected = true;
                    onPublishListener.setPlatform(Platform.FACEBOOK);
                    FacebookAuthenticator.getInstance(activity).publishFeed(
                            onPublishListener, feed);
                }

                break;
            case R.id.googleplus_platform:

                if (GooglePlusAuthenticator.getInstance(activity).isConnected()) {

                    GooglePlusAuthenticator.getInstance(activity).shareStatus(
                            activity.getString(R.string.share_message));
                    connected = true;

                }

                break;
            case R.id.twitter_platform:

                onPublishListener.setPlatform(Platform.TWITTER);
                if (TwitterAuthenticator.getInstance(activity).isConnected()) {

                    TwitterAuthenticator.getInstance(activity).updateStatus(
                            onPublishListener,
                            activity.getString(R.string.share_message));
                    connected = true;

                }

                break;
            case R.id.instagram_platform:
                Log.d(TAG, "Instagram");
                break;
        }
        alertDialog.dismiss();

        if (!connected) {
            UIUtils.alertUser(activity,
                    activity.getString(R.string.you_are_not_logged_in));
        }
    }

    // listener for publishing action (For Facebook & Twitter)
    final PublishRequestCallBack onPublishListener = new PublishRequestCallBack() {

        private Platform platform;

        @Override
        public void onFail(String reason) {
            analyticsManager.sendEvent(
                    AnalyticsConstants.FAILED_TO_SHARE_EVENT,
                    AnalyticsConstants.REASON_PROPERTY, reason);

            if (platform != Platform.FACEBOOK)
                UIUtils.showMessage(activity, "oops. something went wrong: "
                        + reason);

            Log.w(TAG, "Failed to publish: " + reason);
        }

        @Override
        public void onException(Exception throwable) {
            String exceptionString = StringUtils.getStackTrace(throwable);
            analyticsManager.sendEvent(
                    AnalyticsConstants.FAILED_TO_SHARE_EXCEPTION_EVENT,
                    AnalyticsConstants.REASON_PROPERTY, exceptionString);

            UIUtils.showMessage(activity, "oops. something went wrong: "
                    + exceptionString);

            Log.w(TAG, "Failed to publish exception:" + exceptionString);
        }

        @Override
        public void onComplete(String postId) {

            Log.w(TAG, "publish completed. post id:" + postId);

            analyticsManager.sendEvent(
                    AnalyticsConstants.DRAWER_SPREAD_LOVE_SUCCESS_EVENT,
                    AnalyticsConstants.TYPE_PROPERTY,
                    platform.getName(), true,
                    AnalyticsConstants.ACTION_CATEGORY);

            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    alertDialog.dismiss();

                    UIUtils.alertUser(activity,
                            activity.getString(R.string.share_response));
                }
            });
        }

        @Override
        public void whilePublishing() {
            Log.w(TAG, "waiting...");
        }

        @Override
        public void setPlatform(Platform platform) {
            this.platform = platform;
        }
    };

}
