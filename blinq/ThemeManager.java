package com.blinq;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

import com.blinq.models.FeedDesign;
import com.blinq.utils.Constants;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

/**
 * Handle changing the application theme.
 *
 * @author Johan Hansson.
 */
public class ThemeManager {

    /**
     * Set the application theme to day mode.
     *
     * @param context application context.
     */
    public static void setDayModeDesign(Context context) {

        FeedDesign modeDesign = FeedDesign.getInstance();
        Resources resources = context.getResources();

        // Feed activity.
        modeDesign.setFeedActivityBackgroundColor(resources
                .getColor(R.color.feed_activity_background_day));

        // No contact photo.
        modeDesign.setSenderImageId(R.drawable.hb_default_avatar_day);

        // Separator.
        modeDesign.setFeedSeparatorColor(resources
                .getColor(R.color.feed_separator_day));

        // Feed item.
        modeDesign.setFeedBodySelector(R.drawable.feed_item_selector_day);
        modeDesign
                .setFeedBodySelectorUnread(R.drawable.feed_item_selector_day_unread);
        modeDesign.setFeedLongPressBackground(resources
                .getColor(R.color.feed_longpress_background_color_day));

        // Contact Name.
        modeDesign
                .setContactNameTextSelectorRead(R.drawable.feed_sender_name_selector_day_read);
        modeDesign.setContactNameTextSelectorUnread(R.drawable.feed_sender_name_selector_day_unread);

        // Message.
        modeDesign
                .setMessageTextSelectorRead(R.drawable.feed_message_selector_read);
        modeDesign.setMessageTextSelectorUnread(R.drawable.feed_message_selector_unread);

        // Message time ago
        modeDesign.setMessageTimeAgoSelector(R.drawable.feed_message_time_selector);

        // Contact text typeface.
        Typeface typeface = UIUtils.getFontTypeface(context,
                context.getString(R.string.roboto_light));
        modeDesign.setSenderTypefaceRead(typeface);

        typeface = UIUtils.getFontTypeface(context, context.getString(R.string.roboto_meduim));
        modeDesign.setSenderTypefaceUnread(typeface);

        // Merge view.
        modeDesign.setMergeViewBackgroundColor(resources
                .getColor(R.color.merge_view_background_day));

        modeDesign.setMergeViewSuggestionTitleColor(resources
                .getColor(R.color.merge_suggestion_title_day));

        modeDesign.setMergeViewSuggestionHintColor(resources
                .getColor(R.color.merge_suggestion_hint_day));

        modeDesign.setMergeViewSuggestionSeparatorColor(resources
                .getColor(R.color.merge_suggestion_separator_day));

    }

    /**
     * Set the application theme to night mode.
     *
     * @param context application context.
     */
    public static void setNightModeDesign(Context context) {

        FeedDesign modeDesign = FeedDesign.getInstance();
        Resources resources = context.getResources();

        // Feed activity.
        modeDesign.setFeedActivityBackgroundColor(resources
                .getColor(R.color.feed_activity_background_night));

        // No contact photo.
        modeDesign.setSenderImageId(R.drawable.hb_default_avatar_night);

        // Separator.
        modeDesign.setFeedSeparatorColor(resources
                .getColor(R.color.feed_separator_night));

        // Feed item.
        modeDesign.setFeedBodySelector(R.drawable.feed_item_selector_night);
        modeDesign
                .setFeedBodySelectorUnread(R.drawable.feed_item_selector_night_unread);
        modeDesign.setFeedLongPressBackground(resources.getColor(R.color.feed_longpress_background_color_night));

        // Contact Name.
        modeDesign
                .setContactNameTextSelectorRead(R.drawable.feed_sender_name_selector_night_read);
        modeDesign.setContactNameTextSelectorUnread(R.drawable.feed_sender_name_selector_night_unread);

        // Message.
        modeDesign
                .setMessageTextSelectorRead(R.drawable.feed_message_selector_night_read);
        modeDesign.setMessageTextSelectorUnread(R.drawable.feed_message_selector_night_unread);

        // Message time ago
        modeDesign.setMessageTimeAgoSelector(R.drawable.feed_message_time_selector_night);

        // Contact text typeface.
        Typeface typeface = UIUtils.getFontTypeface(context,
                context.getString(R.string.roboto_light));
        modeDesign.setSenderTypefaceRead(typeface);

        typeface = UIUtils.getFontTypeface(context, context.getString(R.string.roboto_meduim));
        modeDesign.setSenderTypefaceUnread(typeface);

        // Merge view.
        modeDesign.setMergeViewBackgroundColor(resources
                .getColor(R.color.merge_view_background_night));

        modeDesign.setMergeViewSuggestionTitleColor(resources
                .getColor(R.color.merge_suggestion_title_night));

        modeDesign.setMergeViewSuggestionHintColor(resources
                .getColor(R.color.merge_suggestion_hint_night));

        modeDesign.setMergeViewSuggestionSeparatorColor(resources
                .getColor(R.color.merge_suggestion_separator_night));
    }

    /**
     * Update theme data depends current application configuration.
     *
     * @param context application context.
     */
    public static void updateThemeData(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        SettingsManager settingsManager = new SettingsManager(context);

        String theme = settingsManager.getTheme();
        // Initialize default them
        if (StringUtils.isBlank(theme)) {
            ThemeManager.setDayModeDesign(context);
            return;
        }

        if (theme.equals(context.getResources().getString(
                R.string.settings_theme_automatic))) {

            UIUtils.updateDayNightMode(context);

            if (sharedPreferences.getBoolean(Constants.IS_DAY_MODE, false)) {

                ThemeManager.setDayModeDesign(context);

            } else {

                ThemeManager.setNightModeDesign(context);
            }

        } else {

            // Theme is changed.
            if (theme.equals(context.getString(R.string.settings_theme_day))) {

                ThemeManager.setDayModeDesign(context);

            } else if (theme.equals(context.getResources().getString(
                    R.string.settings_theme_night))) {

                ThemeManager.setNightModeDesign(context);
            }

        }

    }
}
