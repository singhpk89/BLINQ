package com.blinq.ui.activities.notificationsetup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.blinq.BlinqApplication;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.ui.activities.HeadboxBaseActivity;
import com.blinq.ui.activities.feed.FeedActivity;
import com.blinq.utils.ActivityUtils;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.DialogUtils;
import com.blinq.utils.Log;

import java.lang.ref.WeakReference;

import dreamers.graphics.RippleDrawable;

public class NotificationSetupActivity extends HeadboxBaseActivity {

    public static final int NOTIFICATION_ACCESS_REQUEST = 1;
    public static final String NOTIFICATION_ACCESS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final String TAG = NotificationSetupActivity.class
            .getSimpleName();

    private AlertDialog skipDialog;
    private boolean backPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        fillScreen();
        Log.d(TAG, TAG);
        setContentView(R.layout.activity_notification_setup);
        initializeComponents();
        sendEvent(
                AnalyticsConstants.OPENED_STREAM_NOTIFICATIONS_SCREEN_EVENT, false, AnalyticsConstants.ONBOARDING_CATEGORY);
    }

    private void initializeComponents() {

        ImageView acceptNotification = (ImageView) findViewById(R.id.acceptManageNotification);
        View phone = findViewById(R.id.notification_image_view);
        RippleDrawable.createRipple(acceptNotification, getResources().getColor(R.color.accept_button_ripple));

        phone.setOnClickListener(clickOnStreamNotifications());
        acceptNotification.setOnClickListener(clickOnStreamNotifications());

        skipDialog = buildSkipDialog();

        RelativeLayout notificationStreamLayout = (RelativeLayout) findViewById(R.id.mainNotificationLayout);
        notificationStreamLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendEvent(AnalyticsConstants.CLICKED_RANDOMLY_NOTIFICATION_SCREEN_EVENT, false, AnalyticsConstants.ONBOARDING_CATEGORY);
            }
        });

        initializeAnimations();
    }

    private void initializeAnimations() {
        // checkbox circle animation
        final Animation circleAnimation = AnimationUtils.loadAnimation(this, R.anim.notification_setup_checkbox_circle);
        final View animatedCircleView = findViewById(R.id.animated_circle);
        circleAnimation.setStartOffset(500); // just to wait for the view to load
        // Unfortunately repeatCount doesn't work on AnimationSets.
        // http://stackoverflow.com/questions/4480652/android-animation-does-not-repeat
        // That is why an AnimationListener is used here.
        circleAnimation.setAnimationListener(new RepeatWhenFinishedAnimationListener(animatedCircleView));
        animatedCircleView.startAnimation(circleAnimation);

        // button animation
        final View turnOnBlinqButton = findViewById(R.id.acceptManageNotification);
        final Animation turnOnBlinqButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.notification_setup_turn_on_blinq);
        turnOnBlinqButton.startAnimation(turnOnBlinqButtonAnimation);
    }

    private static class RepeatWhenFinishedAnimationListener implements Animation.AnimationListener {

        private final WeakReference<View> viewWeakReference;

        public RepeatWhenFinishedAnimationListener(View view) {
            viewWeakReference = new WeakReference<View>(view);
        }

        @Override
        public void onAnimationStart(Animation animation) {
            // do nothing
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            final View view = viewWeakReference.get();
            if (view != null) {
                animation.setStartOffset(0);
                view.startAnimation(animation);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // do nothing
        }
    }

    private View.OnClickListener clickOnStreamNotifications() {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendEvent(AnalyticsConstants.CLICKED_ON_NOTIFICATION_STREAM_BUTTON_EVENT, true, AnalyticsConstants.ONBOARDING_CATEGORY);
                Intent intent = AppUtils.getNotificationsServiceIntent();
                startActivityForResult(intent, NOTIFICATION_ACCESS_REQUEST);
            }
        };
    }

    /**
     * Build a custom skip dialog to show when pressing the skip button.
     */
    private AlertDialog buildSkipDialog() {

        String title = getString(R.string.notification_skip_dialog_title);
        String message = getString(R.string.notification_skip_dialog_message);
        String accept = getString(R.string.notification_skip_dialog_accept);

        DialogInterface.OnClickListener positiveClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Do nothing..
                sendEvent(AnalyticsConstants.STREAM_NOTIFICATION_POPUP_OK_CLICKED, true, AnalyticsConstants.ONBOARDING_CATEGORY);
                dialogInterface.dismiss();
            }
        };

        AlertDialog dialog = DialogUtils.createConfirmOnlyAlertDialog(this, title, message,
                positiveClickListener, accept);

        return dialog;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NOTIFICATION_ACCESS_REQUEST) {

            sendEvent(
                    AnalyticsConstants.PRESSED_BACK_IN_STREAM_NOTIFICATION_EVENT,
                    true, AnalyticsConstants.ONBOARDING_CATEGORY);
        }

        if(BlinqApplication.settingsManager.isDefaultNotificationApp()){
            finish();
        }
    }

    private void showSkipDialog() {
        if (!backPressed) {
            sendEvent(AnalyticsConstants.SKIP_STREAM_NOTIFICATION_POPUP_OPENED,
                    true, AnalyticsConstants.ONBOARDING_CATEGORY);
        } else {
            sendEvent(AnalyticsConstants.SKIP_STREAM_NOTIFICATION_POPUP_OPENED_TWICE,
                    true, AnalyticsConstants.ONBOARDING_CATEGORY);
        }
        skipDialog.show();
        backPressed = true;
    }

    private void startFeedActivity() {

        getPreferencesManager().setProperty(
                PreferencesManager.SETUP_SCREEN_OPENED, true);
        finish();
        Intent intent = new Intent(this, FeedActivity.class);

        intent.putExtra(Constants.SINGLE_NOTIFICATION, false);
        intent.putExtra(Constants.FROM_NOTIFICATION, false);
        startActivity(intent);

    }

    @Override
    public void onBackPressed() {

        sendEvent(AnalyticsConstants.BACK_BUTTON_NOTIFICATION_CLICKED_PROPERTY);
        sendEvent(
                AnalyticsConstants.PRESSED_BACK_IN_STREAM_NOTIFICATION_SCREEN_EVENT,
                false, AnalyticsConstants.ONBOARDING_CATEGORY);
        showSkipDialog();
    }

    @Override
    protected void onDestroy() {
        ActivityUtils.unbindDrawables(findViewById(R.id.notification_setup_main_layout));
        super.onDestroy();
    }

}
