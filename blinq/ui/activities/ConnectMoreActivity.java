package com.blinq.ui.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.animation.SetGoneWhenFinishedAnimationListener;
import com.blinq.authentication.impl.Google.GooglePlusAuthenticator;
import com.blinq.server.UserProfileCreator;
import com.blinq.ui.activities.feed.FeedActivity;
import com.blinq.ui.activities.notificationsetup.NotificationSetupActivity;
import com.blinq.ui.adapters.ConnectMoreAdapter;
import com.blinq.ui.views.CustomTypefaceTextView;
import com.blinq.utils.ActivityUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.DialogUtils;
import com.blinq.utils.ServerUtils;

import dreamers.graphics.RippleDrawable;

/**
 * Let headbox user connect with more platforms.
 */
public class ConnectMoreActivity extends HeadboxBaseActivity implements ConnectMoreAdapter.OnPlatformConnectedListener {


    private ListView platformsList;
    private ConnectMoreAdapter connectMoreAdapter;
    private CustomTypefaceTextView skipView;
    private ImageView continueView;
    private AlertDialog skipDialog;
    private AlertDialog syncContactsDialog;
    private boolean backPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fillScreen();

        setContentView(R.layout.activity_connect_more);

        initializeComponents();
        sendEvent(AnalyticsConstants.OPENED_CONNECT_MORE_SCREEN_EVENT,
                false, AnalyticsConstants.ONBOARDING_CATEGORY);

    }

    /**
     * Initialize and populate activity's components.
     */
    private void initializeComponents() {

        RelativeLayout socialNetworks = (RelativeLayout) findViewById(R.id.mainSocialNetworksLayout);
        socialNetworks.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                sendEvent(
                        AnalyticsConstants.CLICKED_RANDOMLY_SOCIAL_NETWORK_SCREEN_EVENT,
                        false,
                        AnalyticsConstants.ONBOARDING_CATEGORY);

            }
        });
        platformsList = (ListView) findViewById(R.id.connect_more_platforms_list);
        skipView = (CustomTypefaceTextView) findViewById(R.id.connect_more_platforms_skip);
        continueView = (ImageView) findViewById(R.id.connect_more_continue);
        RippleDrawable.createRipple(continueView, getResources().getColor(R.color.accept_button_ripple));


        skipDialog = buildSkipDialog();

        skipView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendEvent(AnalyticsConstants.SKIPPED_IN_SOCIAL_EVENT, true, AnalyticsConstants.ONBOARDING_CATEGORY);
                showSkipDialog();
            }
        });

        syncContactsDialog = buildSyncContactsDialog();

        continueView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSyncContactsDialog();
            }
        });

        connectMoreAdapter = new ConnectMoreAdapter(this, this);
        platformsList.setAdapter(connectMoreAdapter);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        GooglePlusAuthenticator.getInstance(this).onActivityResult(this,
                requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startFeedView() {

        finish();
        Intent intent = new Intent(this, FeedActivity.class);
        intent.putExtra(Constants.SINGLE_NOTIFICATION, false);
        intent.putExtra(Constants.FROM_NOTIFICATION, false);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

    }

    @Override
    protected void onDestroy() {
        unbindDrawables();
        ActivityUtils.unbindDrawables(findViewById(R.id.connect_more_main_layout));
        super.onDestroy();
    }

    /**
     * Build a custom skip dialog to show when pressing the skip button.
     */
    private AlertDialog buildSkipDialog() {

        String title = getString(R.string.connect_more_skip_dialog_title);
        String message = getString(R.string.connect_more_skip_dialog_message);
        String accept = getString(R.string.connect_more_skip_dialog_accept);
        String skip = getString(R.string.connect_more_skip_dialog_skip);


        OnClickListener positiveClickListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Do nothing..
                sendEvent(AnalyticsConstants.CONNECT_MORE_PLATFORMS_POPUP_OK_CLICKED, true, AnalyticsConstants.ONBOARDING_CATEGORY);
                dialogInterface.dismiss();
            }
        };
        OnClickListener negativeClickListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Open the next view.
                sendEvent(AnalyticsConstants.CONNECT_MORE_PLATFORMS_POPUP_SKIP_CLICKED, true, AnalyticsConstants.ONBOARDING_CATEGORY);
                dialogInterface.dismiss();
                showSyncContactsDialog();
            }
        };

        AlertDialog dialog = DialogUtils.createConfirmAlertDialog(this, title, message,
                positiveClickListener, accept, skip, negativeClickListener);

        return dialog;
    }

    private AlertDialog buildSyncContactsDialog() {
        String title = getString(R.string.connect_more_sync_contacts_title);
        String message = getString(R.string.connect_more_sync_contacts_message);
        String accept = getString(R.string.connect_more_sync_contacts_accept);
        String cancel = getString(R.string.connect_more_sync_contacts_cancel);

        OnClickListener positiveClickListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setPlatformsSyncStatus();
                preferencesManager.setContactsSyncApprovedByUser();
                ServerUtils.getInstance(ConnectMoreActivity.this).sendContactsDatabase();
                dialogInterface.dismiss();
                forwardToNextView();
            }
        };
        OnClickListener negativeClickListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                forwardToNextView();
            }
        };

        AlertDialog dialog = DialogUtils.createConfirmAlertDialog(this, title, message,
                positiveClickListener, accept, cancel, negativeClickListener);

        return dialog;

    }

    private void startNotificationSetup() {

        // Here to show notification setup screen only one time.
        boolean openedBefore = getPreferencesManager().getProperty(
                PreferencesManager.SETUP_SCREEN_OPENED, false);

        boolean isPreviouslyChecked = settingsManager.isDefaultNotificationApp();

        if (!openedBefore && !isPreviouslyChecked) {

            finish();
            Intent notificationSetupIntent = new Intent(this, NotificationSetupActivity.class);
            notificationSetupIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationSetupIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(notificationSetupIntent);
        } else {
            startFeedView();
        }
    }

    private void forwardToNextView() {

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            startNotificationSetup();
        } else {
            startFeedView();
        }
        createUserProfile();
    }

    private void setPlatformsSyncStatus() {
        if (preferencesManager.getTwitterContactsSyncStatus() != Constants.SYNC_STARTED
                && preferencesManager.getTwitterContactsSyncStatus() != Constants.SYNC_ENDED)
            preferencesManager.setTwitterContactsSyncStatus(Constants.SYNC_SKIPPED);
        if (preferencesManager.getInstagramContactsSyncStatus() != Constants.SYNC_STARTED
                && preferencesManager.getInstagramContactsSyncStatus() != Constants.SYNC_ENDED)
            preferencesManager.setInstagramContactsSyncStatus(Constants.SYNC_SKIPPED);
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        sendEvent(AnalyticsConstants.PRESS_BACK_SOCIAL_EVENT, false, AnalyticsConstants.ONBOARDING_CATEGORY);

        showSkipDialog();
    }

    private void showSkipDialog() {
        sendEvent(AnalyticsConstants.CONNECT_MORE_PLATFORMS_POPUP_OPENED, true, AnalyticsConstants.ONBOARDING_CATEGORY);
        skipDialog.show();
    }

    private void showSyncContactsDialog() {
        if (!backPressed) {
            syncContactsDialog.show();
        } else {
            forwardToNextView();
        }
        backPressed = true;
    }

    @Override
    public void onAllPlatformsConnected() {
        /* I just discovered that this method can be invoked more than once
        and that not all platforms have to be connected here, so beware.*/
        if (continueView.getVisibility() != View.VISIBLE) {
            continueView.setVisibility(View.VISIBLE);
            final Animation fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            fadeInAnimation.setStartOffset(900);
            continueView.startAnimation(fadeInAnimation);

            final Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            fadeOutAnimation.setStartOffset(500);
            fadeOutAnimation.setAnimationListener(new SetGoneWhenFinishedAnimationListener(skipView));
            skipView.startAnimation(fadeOutAnimation);
        }
    }

    private void unbindDrawables() {

//        ImageView view = (ImageView) findViewById(R.id.platform_image_view);
//        view.setImageDrawable(null);
//        view.setImageBitmap(null);
//        view.setImageResource(0);
    }

    private void createUserProfile() {
        UserProfileCreator.getInstance(this).createProfile();
    }


}
