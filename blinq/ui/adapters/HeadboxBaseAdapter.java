package com.blinq.ui.adapters;

import android.widget.BaseAdapter;

import com.blinq.R;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;

/**
 * Work as base adapter for feeds/search list-views to centralize code between them.
 * <p/>
 * Created by Johan Hansson.
 */
public abstract class HeadboxBaseAdapter extends BaseAdapter {

    /**
     * Return the icon id from resources for given platform.
     *
     * @param platform     platform to get icon for.
     * @param lastCallType last call type in feed.
     * @return platform icon id from resources.
     */
    protected int getPlatformIcon(Platform platform, MessageType lastCallType) {

        switch (platform) {

            case CALL:

                if (lastCallType == MessageType.MISSED) {
                    return R.drawable.ic_missedcall;
                } else {
                    return R.drawable.ic_call;
                }

            case FACEBOOK:
                return R.drawable.feed_fb_unread;
            case HANGOUTS:
                return R.drawable.feed_hangouts_unread;
            case SMS:
                return R.drawable.feed_sms_unread;
            case MMS:
                return R.drawable.feed_sms_unread;
            case WHATSAPP:
                return R.drawable.feed_whatsapp_unread;
            case EMAIL:
                return R.drawable.feed_email_incoming_unread;
            case SKYPE:
                return R.drawable.feed_incoming_skype_unread;
            default:
                return 0;
        }
    }
}
