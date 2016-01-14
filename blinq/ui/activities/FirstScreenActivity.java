package com.blinq.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.blinq.R;
import com.blinq.ui.views.CustomDialogBuilder;
import com.blinq.utils.EmailUtils;
import com.blinq.utils.FileUtils;

/**
 * Created by Roi on 12/17/14.
 */
public class FirstScreenActivity extends HeadboxBaseActivity {

    private static int NUMBER_OF_CLICKS_TO_SHOW_DIALOG = 5;

    private CustomDialogBuilder dialogBuilder;
    private AlertDialog sendDatabaseLogDialog;

    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_screen);

        activity = this;
        initializeSendLogsButton();
    }

    private void initializeSendLogsButton() {
        Button button = (Button) findViewById(R.id.send_log_button);
        button.setOnClickListener(new View.OnClickListener() {

            int count;

            @Override
            public void onClick(View view) {
                if (++count == NUMBER_OF_CLICKS_TO_SHOW_DIALOG) {
                    buildSendDatabaseLogDialog();
                    count = 0;
                }
            }
        });
    }

    private void buildSendDatabaseLogDialog() {

        dialogBuilder = new CustomDialogBuilder(this);

        dialogBuilder
                .setTitle(getString(R.string.send_database_log_dialog_title))
                .setMessage(getString(R.string.send_database_log_dialog_body))
                .showIcon(false)
                .setPositiveButton(
                        R.string.send_database_log_dialog_yes_button,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {

                                sendDatabaseLogDialog.dismiss();
                                new CopyFilesInBackground().execute(null, null,
                                        null);

                            }
                        }
                )
                .setNegativeButton(R.string.send_database_log_dialog_no_button,
                        null);

        dialogBuilder.setCancelable(false);

        sendDatabaseLogDialog = dialogBuilder.create();
        sendDatabaseLogDialog.show();
    }

    private class CopyFilesInBackground extends
            AsyncTask<String, String, String> {

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

    @Override
    public void onBackPressed() {
        finish();
    }

}
