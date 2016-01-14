package com.blinq.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;

import com.blinq.R;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.ui.views.CustomDialogBuilder;

/**
 * @author Johan Hansson
 *         <p/>
 *         Holds Dialog utilities.
 */
public final class DialogUtils {

    private static final String TAG = DialogUtils.class.getSimpleName();
    public static final int COPY_MESSAGE_INDEX = 0;
    public static final int DELETE_MESSAGE_INDEX = 1;

    public enum DialogType {
        START, REPORT, CUSTOM, DEFAULT
    }

    ;

    private DialogUtils() {
    }

    /**
     * Create default waiting dialog dialog.
     */
    private static ProgressDialog createDefaultWaitingDialog(Activity activity) {
        ProgressDialog waitingDialog = new ProgressDialog(activity);
        waitingDialog.setMessage(activity.getResources().getString(
                R.string.Loading));
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(false);

        return waitingDialog;
    }

    /**
     * Create custom dialog.
     */
    public static AlertDialog createCustomDialog(Activity activity,
                                                 DialogType dialogType) {

        String title = "", message = "";
        boolean showIcon = true;
        int loadingIconResourse = R.drawable.loading_icon;

        message = AppUtils.getRandomString(activity.getApplicationContext(),
                R.array.waiting_dialogs_messages);
        switch (dialogType) {
            case CUSTOM:
                title = activity.getString(R.string.loading_dialog_title);
                break;
            case START:
                title = activity.getString(R.string.loading_dialog_title);
                break;
            case REPORT:
                title = activity.getString(R.string.copy_database_log_dialog_title);
                message = activity
                        .getString(R.string.copy_database_log_dialog_body);
                break;

            case DEFAULT:
                return createDefaultWaitingDialog(activity);
            default:
                return null;

        }

        CustomDialogBuilder dialogBuilder = new CustomDialogBuilder(activity);
        dialogBuilder.setTitle(title).showIcon(showIcon).setMessage(message)
                .setCancelable(false).setIcon(loadingIconResourse);

        return dialogBuilder.create();

    }

    /**
     * Show the dialog in UI thread.
     */
    public static void showDialog(final Activity activity,
                                  final AlertDialog waitingDialog) {

        if (activity == null || waitingDialog == null
                || !AppUtils.isActivityActive(activity))
            return;

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (waitingDialog != null)
                        waitingDialog.show();
                } catch (Exception e) {

                    Log.d(TAG, "Unable to show Waiting dialog in activity:"
                            + activity.getClass().getSimpleName() + " error:"
                            + e.toString());
                }
            }
        });
    }

    /**
     * Hide the dialog in UI thread.
     */
    public static void hideDialog(final Activity activity,
                                  final AlertDialog waitingDialog) {

        if (activity == null || waitingDialog == null
                || !AppUtils.isActivityActive(activity))
            return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (waitingDialog != null && waitingDialog.isShowing()) {
                    try {
                        waitingDialog.dismiss();
                    } catch (Exception e) {

                        Log.d(TAG,
                                "Unable to dismiss Waiting dialog  activity:"
                                        + activity.getClass().getSimpleName()
                                        + " error:" + e.toString()
                        );
                    }

                }

            }
        });
    }

    /**
     * Create edit message dialog.
     *
     * @param activity current activity.
     * @param listener dialog onClick listener.
     * @param message  clicked message.
     * @return alert dialog for given message.
     */
    public static AlertDialog createEditMessageDialog(Activity activity,
                                                      OnItemClickListener listener, HeadboxMessage message) {

        boolean includeDeleteOption;
        CharSequence[] dialogItems;
        String deleteItem, copyItem;

        CustomDialogBuilder builder = new CustomDialogBuilder(activity);

        CharSequence title = activity
                .getString(R.string.edit_message_dialog_title);
        builder.setTitle(title);
        builder.setTitleColor(activity
                .getString(R.color.list_dialog_title_color));
        builder.showIcon(false);
        builder.showMessage(false);

        includeDeleteOption = !StringUtils.isBlank(message.getSourceId())
                || message.getPlatform() == Platform.HANGOUTS || message.getPlatform() == Platform.SKYPE || message.getPlatform() == Platform.WHATSAPP;
        deleteItem = activity.getString(R.string.delete_message);

        if (message.getPlatform() == Platform.CALL) {
            copyItem = activity.getString(R.string.copy_call_message) + " "
                    + message.getContact().getIdentifier();
        } else {
            copyItem = activity.getString(R.string.copy_text_message);
        }

        if (includeDeleteOption) {
            dialogItems = new String[]{copyItem, deleteItem};

        } else {
            dialogItems = new String[]{copyItem};
        }

        builder.setItemListView(Arrays.asList(dialogItems), listener);
        return builder.create();
    }

    /**
     * Build custom dialog to select phone-number from.
     *
     * @param activity current activity.
     * @param listener dialog onClick listener.
     * @return alert dialog.
     */
    public static AlertDialog createChooseContactDialog(Activity activity,
                                                        String title, OnItemClickListener listener,
                                                        List<MemberContact> contacts) {

        CustomDialogBuilder builder = new CustomDialogBuilder(activity);

        builder.setTitle(title);
        builder.setTitleColor(activity
                .getString(R.color.list_dialog_title_color));
        builder.showIcon(false);
        builder.showMessage(false);

        List<CharSequence> numbers = new ArrayList<CharSequence>();
        for (MemberContact contact : contacts) {
            numbers.add(contact.getIdentifier());
        }

        builder.setItemListView(numbers, listener);

        return builder.create();
    }

    /**
     * Open phone/mobile number selecter dialog.
     *
     * @param listener - Respond to the dialog item selection change.
     * @param contacts - list of mobile/phone numbers to build the dialog from.
     * @param platform - Platform to build dialog to.
     */
    public static void openChooseNumberDialog(Activity activity,
                                              OnItemClickListener listener, List<MemberContact> contacts,
                                              final Platform platform) {

        AlertDialog chooseNumberDialog = null;

        String dialogTitle = "";
        if (platform == Platform.EMAIL) {
            dialogTitle = activity.getString(R.string.email_dialog_title);
        } else if (platform == Platform.SMS || platform == Platform.CALL) {
            dialogTitle = activity.getString(R.string.call_dialog_title);
        }

        chooseNumberDialog = createChooseContactDialog(activity, dialogTitle,
                listener, contacts);
        showDialog(activity, chooseNumberDialog);

    }


    public static AlertDialog createConfirmAlertDialog(Activity activity,
                                                       String title, String message,
                                                       OnClickListener positiveClickListener, String positiveButtonText,
                                                       String negativeButtonText, OnClickListener negativeClickListener) {

        CustomDialogBuilder dialogBuilder = new CustomDialogBuilder(activity);
        dialogBuilder
                .setTitle(title)
                .setMessage(message)
                .showIcon(false)
                .setPositiveButton(positiveButtonText, positiveClickListener)
                .setNegativeButton(negativeButtonText,
                        negativeClickListener);

        dialogBuilder.setCancelable(false);
        return dialogBuilder.create();

    }

    public static AlertDialog createConfirmOnlyAlertDialog(Activity activity,
                                                       String title, String message,
                                                       OnClickListener positiveClickListener,
                                                       String positiveButtonText) {

        CustomDialogBuilder dialogBuilder = new CustomDialogBuilder(activity);
        dialogBuilder
                .setTitle(title)
                .setMessage(message)
                .showIcon(false)
                .setPositiveButton(positiveButtonText, positiveClickListener);

        dialogBuilder.setCancelable(false);
        return dialogBuilder.create();

    }

}
