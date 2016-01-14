package com.blinq.ui.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinq.ImageLoaderManager;
import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.provider.CallsManager;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.SMSManager;
import com.blinq.ui.activities.webpage.WebPageActivity;
import com.blinq.ui.animations.SlideAnimation;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.DialogUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Takes a list of messages and manage the process of displaying them.
 *
 * @author Johan Hansson
 */
public class MessageListAdapter extends BaseAdapter {


    private static final int INCOMING_MESSAGE_TYPE = 0;
    private static final int OUTGOING_MESSAGE_TYPE = 1;
    private static final int MESSAGE_TYPE_COUNT = 2;
    private static final int GROUPING_PERIOD_IN_MINUTES = 10;

    private final int LAST_ITEM_SLIDE_IN_ANIMATION_DURATION = 300;

    private LayoutInflater inflater;
    private List<HeadboxMessage> messages;
    private int feedId;
    private Activity activity;
    private HashMap<Integer, Boolean> selectedRows = new HashMap<Integer, Boolean>();
    private ImageLoaderManager imageLoaderManager;

    /**
     * No animation applied to last item (No new messages).
     */
    public static final int NORMAL_MODE = 1;

    /**
     * Slide in animation applied to last item (New message added).
     */
    public static final int ANIMATION_MODE = 2;

    private int displayMode = NORMAL_MODE;

    private BlinqAnalytics analyticsManager;

    /**
     * @param activity activity that hold messages ListView.
     * @param messages List of message object.
     * @param feedId   friend feed ID.
     */
    public MessageListAdapter(Activity activity, List<HeadboxMessage> messages,
                              int feedId) {

        this.messages = messages;
        this.feedId = feedId;
        this.activity = activity;
        inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        imageLoaderManager = new ImageLoaderManager(
                activity.getApplicationContext());
        analyticsManager = new BlinqAnalytics(activity.getApplicationContext());

    }

    @Override
    public int getItemViewType(int position) {

        MessageType messageType = messages.get(position).getType();
        if (messageType == MessageType.INCOMING
                || messageType == MessageType.MISSED) {
            return INCOMING_MESSAGE_TYPE;
        }
        return OUTGOING_MESSAGE_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        return MESSAGE_TYPE_COUNT;
    }

    @Override
    public int getCount() {

        return messages.size();
    }

    @Override
    public Object getItem(int position) {

        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    public void updateItemView(HeadboxMessage updatedMessage) {

        // Get message position.
        int position = 0;
        for (HeadboxMessage message : messages) {
            if (message.getMessageId().equals(updatedMessage.getMessageId())) {
                position = messages.indexOf(message);
                break;
            }
        }
        messages.set(position, updatedMessage);
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {

        ViewHolder holder;
        int type = getItemViewType(position);
        if (convertView == null) {
            holder = new ViewHolder();

            switch (type) {
                case INCOMING_MESSAGE_TYPE:
                    convertView = inflater.inflate(R.layout.incoming_message_view,
                            parent, false);
                    holder = getHolder(convertView);
                    break;

                case OUTGOING_MESSAGE_TYPE:
                    convertView = inflater.inflate(R.layout.outgoing_message_view,
                            parent, false);
                    holder = getHolder(convertView);
                    break;
            }
            if (convertView != null)
                convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        setMessageDesign(holder, position);

        // Apply animation on last item if needed.
        if (position == messages.size() - 1 && displayMode == ANIMATION_MODE) {

            applyAnimationOnLastItem(position, convertView, parent);
        }

        holder.bubbleLayout.setBackgroundColor(activity.getResources().getColor(android.R.color.background_light)); //default color

        if (selectedRows.get(position) != null) {
            holder.bubbleLayout.setBackgroundColor(activity.getResources().getColor(R.color.messages_highlight_color));// this is a selected position so make it red
        } else {
            setupBubble(position, holder);
        }
        return convertView;

    }

    /**
     * Apply appropriate animation on last item in messages list.
     *
     * @param position list item position.
     * @param view     list item view.
     * @param parent   parent view of current list item.
     */
    private void applyAnimationOnLastItem(int position, View view,
                                          ViewGroup parent) {

        switch (messages.get(position).getType()) {

            // Left in animation.
            case INCOMING:

                SlideAnimation.slideHorizontally(view, 0 - parent.getWidth(), 0,
                        LAST_ITEM_SLIDE_IN_ANIMATION_DURATION);
                displayMode = NORMAL_MODE;

                break;

            // Right in animation.
            case OUTGOING:

                SlideAnimation.slideHorizontally(view, parent.getWidth(), 0,
                        LAST_ITEM_SLIDE_IN_ANIMATION_DURATION);
                displayMode = NORMAL_MODE;

                break;

            default:
                break;
        }

    }

    /**
     * Set the design for current message item.
     *
     * @param holder   view holder of current message
     * @param position the position of message inside the list
     */
    private void setMessageDesign(final ViewHolder holder, final int position) {

        final HeadboxMessage message = messages.get(position);

        configurePlatformIcon(holder, position);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm");
        holder.messageTextDate.setText(simpleDateFormat.format(message.getDate()).toString());
        holder.messageText.setText(message.getBody());
        holder.messageText.setGravity(getBodyGravity(position));
        if (isStartOfGroup(position)) {
            holder.messageDate.setText(StringUtils.normalizeDate(
                    activity.getApplicationContext(), message.getDate()));
            holder.messageDate.setVisibility(View.VISIBLE);
        } else {
            holder.messageDate.setVisibility(View.GONE);
        }

        // Try again layout.
        if (message.getType() == MessageType.FAILED) {

            holder.sendAgainLayout.setVisibility(View.GONE);
            holder.sendAgainLayout.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                }
            });

        } else if (message.getType() == MessageType.PENDING) {
            holder.sendingLayout.setVisibility(View.VISIBLE);
        } else {
            holder.sendAgainLayout.setVisibility(View.GONE);
        }

        // MMS.
        if (message.getPlatform() == Platform.MMS && holder.MMSGridView != null) {

            holder.MMSGridView.setVisibility(View.VISIBLE);

            if (message.getType() == MessageType.OUTGOING) {

                configureOutgoingMMSDesign(holder);

            } else {

                configureIncomingMMSDesign(holder);

            }

            final List<Uri> imagesUris = SMSManager.getMmsImages(
                    activity.getApplicationContext(), message.getSourceId());

            holder.MMSGridView
                    .setOnItemClickListener(new OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> arg0, View arg1,
                                                int arg2, long position) {

                            // displayImageInDialog(imagesUris.get((int)
                            // position));
                            AppUtils.openPhotoIntent(activity,
                                    imagesUris.get((int) position));
                        }
                    });

            holder.MMSGridView
                    .setOnItemLongClickListener(new OnItemLongClickListener() {

                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent,
                                                       View view, int p, long id) {
                            onMessageLongClick(position);
                            return false;
                        }
                    });

            configureMMSGridView(holder, imagesUris.size());

            MMSAdapter imageAdapter = new MMSAdapter(
                    activity.getApplicationContext(), imagesUris);
            holder.MMSGridView.setAdapter(imageAdapter);

        } else {
            holder.MMSGridView.setVisibility(View.GONE);
        }

    }

    private void configurePlatformIcon(ViewHolder holder, int position) {

        int platformIconResourceID = 0;

        switch ((messages.get(position).getPlatform())) {
            case FACEBOOK:
                platformIconResourceID = R.drawable.conversation_circle_fb;
                break;

            case SMS:
            case MMS:
                platformIconResourceID = R.drawable.conversation_circle_sms;
                break;
            case CALL:
                switch (messages.get(position).getType()) {
                    case MISSED:
                        platformIconResourceID = R.drawable.conversation_circle_missed;
                        break;
                    case OUTGOING:
                        platformIconResourceID = R.drawable.conversation_circle_call_out;
                        break;

                    default:
                        platformIconResourceID = R.drawable.conversation_circle_call_in;
                        break;
                }
                break;
            case WHATSAPP:
                platformIconResourceID = R.drawable.conversation_circle_whatsapp;
                break;
            case EMAIL:
                platformIconResourceID = R.drawable.conversation_circle_email;
                break;

            case SKYPE:
                platformIconResourceID = R.drawable.conversation_circle_skype;
                break;
            case HANGOUTS:
                platformIconResourceID = R.drawable.conversation_circle_hangouts;
                break;
            default:
                Log.e("Unknown Platform", "Message platfom type is unknown.");
                break;

        }

        holder.accountImage.setImageResource(platformIconResourceID);
    }

    /**
     * Configure MMS grid view (Width/Height) depends on the number of items.
     *
     * @param holder        item views holder.
     * @param numberOfItems number of items in the MMS message.
     */
    private void configureMMSGridView(ViewHolder holder, int numberOfItems) {

        float imageHeightPx = activity.getApplicationContext().getResources()
                .getDimension(R.dimen.mms_image_item_height);

        int numberOfRows = (int) Math.ceil((float) numberOfItems
                / holder.MMSGridView.getNumColumns());

        ViewGroup.LayoutParams layoutParams = holder.MMSGridView
                .getLayoutParams();
        layoutParams.height = (int) (numberOfRows * imageHeightPx);
        holder.MMSGridView.setLayoutParams(layoutParams);
    }

    /**
     * Set needed paddings for incoming MMS layout.
     *
     * @param holder view holder of message list item.
     */
    private void configureIncomingMMSDesign(ViewHolder holder) {

        Context context = activity.getApplicationContext();

        int messagePaddingTop = (int) context.getResources().getDimension(
                R.dimen.message_padding_top);

        int messagePaddingBottom = (int) context.getResources().getDimension(
                R.dimen.message_padding_bottom);

        int mmsImageLeftMargin = (int) context.getResources().getDimension(
                R.dimen.incoming_mms_image_margin_left);
        int mmsImageRightMargin = (int) context.getResources().getDimension(
                R.dimen.incoming_mms_image_margin_right);

        int noMMSMessageLeftPadding = (int) context.getResources()
                .getDimension(R.dimen.incoming_message_margin_left);
        int noMMSMessageRightPadding = (int) context.getResources()
                .getDimension(R.dimen.incoming_message_margin_right);

        holder.messageText.setPadding(mmsImageLeftMargin
                        - noMMSMessageLeftPadding, messagePaddingTop,
                mmsImageRightMargin - noMMSMessageRightPadding,
                messagePaddingBottom
        );

    }

    /**
     * Set needed paddings for outgoing MMS layout
     *
     * @param holder view holder of message list item
     */
    private void configureOutgoingMMSDesign(ViewHolder holder) {

        Context context = activity.getApplicationContext();

        int messagePaddingTop = (int) context.getResources().getDimension(
                R.dimen.message_padding_top);

        int messagePaddingBottom = (int) context.getResources().getDimension(
                R.dimen.message_padding_bottom);

        int mmsImageLeftMargin = (int) context.getResources().getDimension(
                R.dimen.outgoing_mms_image_margin_left);
        int mmsImageRightMargin = (int) context.getResources().getDimension(
                R.dimen.outgoing_mms_image_margin_right);

        int noMMSMessageLeftPadding = (int) context.getResources()
                .getDimension(R.dimen.outgoing_message_margin_left);
        int noMMSMessageRightPadding = (int) context.getResources()
                .getDimension(R.dimen.outgoing_message_margin_right);

        holder.messageText.setPadding(mmsImageLeftMargin
                        - noMMSMessageLeftPadding, messagePaddingTop,
                mmsImageRightMargin - noMMSMessageRightPadding,
                messagePaddingBottom
        );

    }

    /**
     * Returns gravity direction of body text view for given message position.
     *
     * @param position the position of selected message .
     * @return the gravity direction of body text view for given message
     * position.
     */
    public int getBodyGravity(int position) {

        int type = getItemViewType(position);

        if (messages.get(position).getPlatform() == Platform.CALL) {
            switch (type) {
                case INCOMING_MESSAGE_TYPE:
                    return Gravity.LEFT;
                case OUTGOING_MESSAGE_TYPE:
                    return Gravity.RIGHT;
            }
        }

        return Gravity.LEFT;
    }

    /**
     * Set the speech bubble icon  and the text cololr
     *
     * @param position the position of selected message .
     */
    public void setupBubble(int position, ViewHolder holder) {

        int type = getItemViewType(position);
        int textColor = R.color.conversation_tb_call_color;
        int bubbleResourceID = R.drawable.conversation_tb_call_incoming;

        if (messages.get(position).getPlatform() == Platform.CALL) {

            switch (messages.get(position).getType()) {
                case OUTGOING:
                    bubbleResourceID = R.drawable.conversation_tb_call_outgoing;
                    break;
                case MISSED:
                    textColor = R.color.conversation_tb_call_missed_color;
                    bubbleResourceID = R.drawable.conversation_tb_call_missed;
                    break;

            }

        } else {

            textColor = R.color.message_text_color;
            switch (type) {

                case INCOMING_MESSAGE_TYPE:
                    bubbleResourceID = R.drawable.conversation_tb_msg_in;

                    break;
                case OUTGOING_MESSAGE_TYPE:
                    bubbleResourceID = R.drawable.conversation_tb_msg_out; //change
                    break;
            }
        }

        holder.bubbleLayout.setBackgroundResource(bubbleResourceID);
        setMessageTextColor(holder, textColor);
    }

    private void setMessageTextColor(ViewHolder holder, int colorId) {
        holder.messageText.setTextColor(activity.getResources().getColor(colorId));
        holder.messageTextDate.setTextColor(activity.getResources().getColor(colorId));
    }

    /**
     * return boolean that will be true if the message is first message in the
     * group.
     *
     * @param position the position of message.
     * @return true if given message is the first message of a group, false
     * otherwise.
     */
    private boolean isStartOfGroup(int position) {

        if (position > 0) {

            Calendar currentMessageDate = Calendar.getInstance();
            currentMessageDate.setTimeInMillis(messages.get(position).getDate()
                    .getTime());

            Calendar previousMessageDate = Calendar.getInstance();
            previousMessageDate.setTimeInMillis(messages.get(position - 1)
                    .getDate().getTime());

            // Group messages per day.
            if (currentMessageDate.get(Calendar.YEAR) == previousMessageDate
                    .get(Calendar.YEAR) && currentMessageDate.get(Calendar.DAY_OF_YEAR) == previousMessageDate
                    .get(Calendar.DAY_OF_YEAR)) {

                return false;

            }

        }

        return true;
    }

    /**
     * Returns string with clickable phone number and URL.
     *
     * @param messageBody body of message.
     * @return string with clickable phone number and URL
     */
    private Spanned getSpannedString(final String messageBody) {

        SpannableString spannableString = new SpannableString(messageBody);

        int numOfLetters = 0;

        List<String> lines = Arrays.asList(messageBody.split("\n"));

        for (final String line : lines) {

            List<String> terms = Arrays.asList(line.split(" "));

            for (final String term : terms) {

                final boolean isValidNumber = StringUtils
                        .isValidPhoneNumber(term);
                final boolean isValidURL = URLUtil.isValidUrl(term);
                int startIndex = messageBody.indexOf(term, numOfLetters);

                if (isValidNumber || isValidURL) {
                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(View textView) {

                            if (textView.isPressed()) {
                                if (isValidURL) {

                                    Intent webPageIntent = new Intent(
                                            activity.getApplicationContext(),
                                            WebPageActivity.class);
                                    webPageIntent.putExtra(
                                            Constants.WEB_PAGE_LINK, term);
                                    activity.startActivity(webPageIntent);

                                } else if (isValidNumber) {
                                    CallsManager.makeADial(activity, term);
                                }
                            }
                        }
                    };
                    spannableString.setSpan(clickableSpan, startIndex,
                            startIndex + term.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannableString.setSpan(
                            new ForegroundColorSpan(activity.getResources()
                                    .getColor(R.color.url_phone_message_body)),
                            startIndex, startIndex + term.length(), 0
                    );
                }

                numOfLetters = numOfLetters + term.length() + 1;
            }
        }

        return spannableString;
    }

    /**
     * Returns the view holder of ListView Adapter.
     *
     * @param convertView view of data corresponding to specific position.
     * @return the view holder of ListView Adapter.
     */
    public ViewHolder getHolder(View convertView) {

        ViewHolder holder;

        holder = new ViewHolder();

        holder.messageText = (TextView) convertView
                .findViewById(R.id.message_text);
        holder.messageTextDate = (TextView) convertView
                .findViewById(R.id.message_text_date);
        Typeface messageTextTypeface = UIUtils.getFontTypeface(
                convertView.getContext(), UIUtils.Fonts.ROBOTO_CONDENSED);
        holder.messageText.setTypeface(messageTextTypeface);

        holder.accountImage = (ImageView) convertView
                .findViewById(R.id.account_image);

        holder.bubbleLayout = (FrameLayout) convertView
                .findViewById(R.id.bubble_layout);
        holder.messageDate = (TextView) convertView
                .findViewById(R.id.message_date);

        Typeface messageDateTypeface = UIUtils.getFontTypeface(
                convertView.getContext(), UIUtils.Fonts.ROBOTO_CONDENSED);
        holder.messageDate.setTypeface(messageDateTypeface);
        holder.sendAgainLayout = (FrameLayout) convertView
                .findViewById(R.id.send_again);
        holder.sendingLayout = (FrameLayout) convertView
                .findViewById(R.id.message_sending);

        holder.MMSGridView = (GridView) convertView
                .findViewById(R.id.gridviewForMMSData);

        return holder;
    }

    /**
     * Adds new message at the top of message list.
     *
     * @param message to be added at the top of message list.
     */
    public void addTop(HeadboxMessage message) {
        messages.add(0, message);
    }

    /**
     * Adds new message at the end of message list.
     *
     * @param message to be added at the end of message list.
     */
    public void addEnd(HeadboxMessage message) {
        messages.add(message);
    }

    /**
     * Remove given message from list.
     *
     * @param messageId ID of the message.
     */
    public void removeMessage(long messageId) {

        for (HeadboxMessage message : messages) {
            if (Long.valueOf(message.getMessageId()) == messageId) {
                messages.remove(messages.indexOf(message));
                break;
            }
        }
    }

    /**
     * Perform action on message long click.
     *
     * @param position the position of message inside adapter.
     */
    public void onMessageLongClick(final int position) {

        OnItemClickListener listener;
        AlertDialog editMessageDialog = null;

        final HeadboxMessage message = messages.get(position);

        listener = new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                switch (position) {

                    case DialogUtils.COPY_MESSAGE_INDEX:
                        copyMessage(message);
                        analyticsManager.sendEvent(
                                AnalyticsConstants.COPY_MESSAGE_EVENT, false,
                                AnalyticsConstants.ACTION_CATEGORY);
                        break;
                    case DialogUtils.DELETE_MESSAGE_INDEX:

                        deleteMessage(message, feedId);
                        analyticsManager.sendEvent(
                                AnalyticsConstants.DELETE_MESSAGE_EVENT, false,
                                AnalyticsConstants.ACTION_CATEGORY);
                        break;
                }
            }
        };

        editMessageDialog = DialogUtils.createEditMessageDialog(activity,
                listener, message);
        DialogUtils.showDialog(activity, editMessageDialog);
        analyticsManager.sendEvent(
                AnalyticsConstants.LONG_CLICK_ON_MESSAGE_EVENT, false,
                AnalyticsConstants.ACTION_CATEGORY);


    }

    /**
     * Copy the message body / call number to the android clipboard
     */
    protected void copyMessage(HeadboxMessage message) {
        String copiedMessage, messageType;
        if (message.getPlatform() == Platform.CALL) {
            copiedMessage = message.getContact().getIdentifier();

            messageType = activity.getString(R.string.copy_number);
        } else {
            copiedMessage = message.getBody();
            messageType = activity.getString(R.string.copy_text);
        }
        AppUtils.copyTextToClipboard(activity.getApplicationContext(),
                copiedMessage);

        UIUtils.alertUser(activity, String.format(
                activity.getString(R.string.copy_success_message), messageType));

    }

    /**
     * Delete the message from Headbox DB and displayed list
     */
    protected void deleteMessage(HeadboxMessage message
            , long feedId) {

        int deleteResult = 0;
        switch (message.getPlatform()) {
            case HANGOUTS:
            case SKYPE:
            case WHATSAPP:
                deleteResult = FeedProviderImpl.getInstance(
                        activity.getApplicationContext()).deleteMessage(feedId,
                        Long.valueOf(message.getMessageId()));
                break;

            default:
                if (!StringUtils.isBlank(message.getSourceId())) {
                    deleteResult = FeedProviderImpl.getInstance(
                            activity.getApplicationContext()).deleteMessage(
                            message.getSourceId(), message.getPlatform(), feedId);
                }
                break;
        }


        if (deleteResult > 0) {
            messages.remove(message);
            notifyDataSetChanged();
            if (messages.size() <= 0) {
                activity.finish();
            }

        }
    }

    public void deleteSelectedMessages() {

        ArrayList<HeadboxMessage> currentCheckedMessages = getCurrentCheckedMessages();
        if (currentCheckedMessages == null || currentCheckedMessages.size() <= 0) {
            return;
        }
        for (HeadboxMessage message : currentCheckedMessages) {

            deleteMessage(message, feedId);
        }

        UIUtils.alertUser(activity, activity.getString(R.string.delete_message));
    }


    public void copySelectedMessages() {

        ArrayList<HeadboxMessage> currentCheckedMessages = getCurrentCheckedMessages();
        if (currentCheckedMessages == null || currentCheckedMessages.size() <= 0) {
            return;
        }
        StringBuilder messageCopier = new StringBuilder();
        for (HeadboxMessage message : currentCheckedMessages) {

            messageCopier.append(getCopyMessageText(message) + StringUtils.NEW_LINE);
        }

        AppUtils.copyTextToClipboard(activity.getApplicationContext(),
                messageCopier.toString());

        UIUtils.alertUser(activity, activity.getString(R.string.copy_call_message));
    }


    /**
     * Used to hold the UI of single view to avoid duplicating view for each
     * list element.
     */
    public static class ViewHolder {

        public TextView messageText;
        public TextView messageTextDate;
        public FrameLayout bubbleLayout;
        public ImageView accountImage;
        public TextView messageDate;
        FrameLayout sendAgainLayout;
        FrameLayout sendingLayout;
        GridView MMSGridView;
    }

    private String getCopyMessageText(HeadboxMessage message) {
        String copiedMessage;
        if (message.getPlatform() == Platform.CALL) {
            copiedMessage = message.getContact().getIdentifier();

        } else {
            copiedMessage = message.getBody();

        }

        return copiedMessage;
    }

    /**
     * @return display mode (Animation, Normal) of the messages list view.
     */
    public int getDisplayMode() {
        return displayMode;
    }

    /**
     * @param displayMode display mode (Animation, Normal) of the list view.
     */
    public void setDisplayMode(int displayMode) {
        this.displayMode = displayMode;
    }


    public void setNewSelection(int position, boolean value) {
        selectedRows.put(position, value);
        notifyDataSetChanged();
    }

    public boolean isPositionChecked(int position) {

        Boolean result = selectedRows.get(position);
        return result == null ? false : result;
    }

    public ArrayList<HeadboxMessage> getCurrentCheckedMessages() {
        ArrayList<HeadboxMessage> selectedMessages = new ArrayList<HeadboxMessage>();
        for (int position : selectedRows.keySet()) {
            if (position < messages.size()) {
                selectedMessages.add(messages.get(position));
            }
        }

        return selectedMessages;
    }

    public void removeSelection(int position) {
        selectedRows.remove(position);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedRows = new HashMap<Integer, Boolean>();
        notifyDataSetChanged();
    }


}