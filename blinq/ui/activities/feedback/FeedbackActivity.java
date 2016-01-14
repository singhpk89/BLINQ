package com.blinq.ui.activities.feedback;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.ui.activities.HeadboxBaseActivity;
import com.blinq.ui.views.CustomDialogBuilder;
import com.blinq.utils.AppUtils;
import com.blinq.utils.DialogUtils;
import com.blinq.utils.EmailUtils;
import com.blinq.utils.ExternalAppsUtils;
import com.blinq.utils.FileUtils;
import com.blinq.utils.Log;
import com.blinq.utils.UIUtils;

public class FeedbackActivity extends HeadboxBaseActivity implements OnClickListener {

    private static final String TAG = FeedbackActivity.class.getSimpleName();

    private Button reportBugButton, requestFeatureButton, sendLogButton;
    private CustomDialogBuilder dialogBuilder;
    private AlertDialog copyingDialog;
    private AlertDialog sendDatabaseLogDialog;

    private final String HeadBox_COMMUNITY_ID = "114349608214033630692/";
    private final String BLINQ_COMMUNITY_ID = "101894883006546057410/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        AppUtils.unRegisterActivityGoingIntoBackground(this);
        hideActionBar();
        init();
        sendEvent(AnalyticsConstants.OPENED_FEEDBACK_SCREEN_EVENT, false, AnalyticsConstants.ACTION_CATEGORY);

    }

    @Override
    protected void onResume() {
        checkIdleState();
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        AppUtils.registerActivityGoingIntoBackground(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        complete();
        DialogUtils.hideDialog(FeedbackActivity.this, copyingDialog);
        super.onDestroy();
    }

    /**
     * Initialize required data for the activity.
     */
    private void init() {

        Typeface typeface = UIUtils.getFontTypeface(getApplicationContext(),
                UIUtils.Fonts.ROBOTO_CONDENSED);

        reportBugButton = (Button) findViewById(R.id.buttonReportBug);
        reportBugButton.setTypeface(typeface);
        reportBugButton.setOnClickListener(this);

        requestFeatureButton = (Button) findViewById(R.id.buttonJoinOurCommunity);
        requestFeatureButton.setTypeface(typeface);
        requestFeatureButton.setOnClickListener(this);

        sendLogButton = (Button) findViewById(R.id.buttonSendYourLog);
        sendLogButton.setTypeface(typeface);
        sendLogButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.buttonReportBug:

                EmailUtils.sendEmail(
                        new String[]{getString(R.string.headbox_support_email)},
                        EmailUtils.REPORT_BUG_TITLE, AppUtils.getDeviceInfo(this),
                        this);

                finish();
                break;

            case R.id.buttonJoinOurCommunity:
                ExternalAppsUtils.openGoogleCommunityPage(this, BLINQ_COMMUNITY_ID);
                finish();
                break;

            case R.id.buttonSendYourLog:
                sendBugReportEmailWithLogs();
                break;

        }

    }

    /**
     * Build send database-log dialog.
     */
    private void sendBugReportEmailWithLogs() {

        new CopyFilesInBackground(FeedbackActivity.this).execute(null, null,
                null);

    }

    private class CopyFilesInBackground extends
            AsyncTask<String, String, String> {

        Activity activity;

        public CopyFilesInBackground(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected String doInBackground(String... params) {

            FileUtils.prepareDatabaseLogToSend(activity, false);

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            EmailUtils.sendDatabaseLog(activity);
        }

    }
    /**
     * Check if the Feedback activity was idle to return it to home screen.
     */
    private void checkIdleState() {
        if (AppUtils.isActivityIdle(this)) {
            Log.d(TAG, "Detect feedback idle state.");
            finish();

        }

    }

}