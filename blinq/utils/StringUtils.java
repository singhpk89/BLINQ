package com.blinq.utils;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;

import com.github.kevinsawicki.timeago.TimeAgo;
import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.blinq.R;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Character.UnicodeBlock;

/**
 * Hold an operations over Strings.
 */
public class StringUtils {

    public static final String FACEBOOK_CHAT_SUFFIX = "@chat.facebook.com";
    public static final String FACEBOOK_CHAT_PREFIX = "-";
    public static final String FACEBOOK_SUFFIX = "@facebook.com";
    public static final String GMAIL_SUFFIX = "@gmail.com";
    public static final String GOOGLE_SUFFIX = "@google.com";
    public static final String GOOGLE_PLUS_PHOTO_URL = "https://plus.google.com/s2/photos/profile/%s";
    public static final String EXALT_SUFFIX = "@exalt.ps";
    public static final String TWITTER_SUFFUX = "@twitter.com";
    public static final String INSTAGRAM_SUFFIX = "@instagram.com";
    public static final String SKYPE_SUFFIX = "@skype";
    public static final String WHATSAPP_SUFFIX = "whatsapp";
    public static final String GOOGLE_TALK_SUFFIX = "@public.talk.google.com";
    public static final String NEW_LINE = System.getProperty("line.separator");

    /**
     * To use when we have a contact without display name.
     */
    public static final String UNKNOWN_PERSON = "Unknown Person";
    public static final String UNKNOWN_CONTACT = "Unknown";
    public static final String PRIVATE = "Private Number";
    public static final String UNKNOWN_NUMBER = "-1";
    public static final String PRIVATE_NUMBER = "-2";

    public static final String EMPTY_STRING = "";
    public static final String SPACE = " ";
    public static final String ON = "On";
    public static final String OFF = "Off";

    public static final String ACTIVITY_LEAVING_SUFFIX = "_activity_leaving";
    public static final String ACTIVITY_COME_FROM_BACKGROUND_SUFFIX = "_activity_come_from_background";
    public static final String NEW_MESSAGE_RECEIVED = "New message received";
    public static final String HANGOUTS = "Hangouts";

    /**
     * Convert an integers array to string separated with commas
     *
     * @param numbers - integers
     */
    public static String convertToString(Integer[] numbers) {
        final String stringRep = Arrays.toString(numbers);
        final String[] out = stringRep.substring(1, stringRep.length() - 1)
                .split("\\s*,\\s*");
        String string = TextUtils.join(",", out);
        return string;
    }

    public static String convertToString(Long[] numbers) {
        final String stringRep = Arrays.toString(numbers);
        final String[] out = stringRep.substring(1, stringRep.length() - 1)
                .split("\\s*,\\s*");
        String string = TextUtils.join(",", out);
        return string;
    }

    /**
     * Convert an strings array to string separated with commas
     */
    public static String convertToString(String[] strings) {
        if (strings == null) {
            return EMPTY_STRING;
        }

        //check if all of the strings are null
        boolean isAllNull = true;
        for(String s : strings) {
            if(s != null) {
                isAllNull = false;
                break;
            }
        }

        if (isAllNull) { return EMPTY_STRING; }

        return Joiner.on(",").join(strings);
    }

    /**
     * Takes array of strings and returns a string contains these strings
     * surrounded by '' and separated by commas. Example: return
     * "'John','Smith'"
     */
    public static String convertStringsForINQuery(String[] strings) {

        String[] temp = new String[strings.length];
        int index = 0;
        for (String string : strings) {
            temp[index++] = "'" + string + "'";
        }
        return Joiner.on(",").join(temp);
    }

    /**
     * Takes list of strings and returns a string contains these strings
     * surrounded by '' and separated by commas. Example: return
     * "'John','Smith'"
     */
    public static String convertStringsForINQuery(List<String> strings) {

        String[] temp = new String[strings.size()];
        int index = 0;
        for (String string : strings) {
            temp[index++] = "'" + string + "'";
        }
        return Joiner.on(",").join(temp);
    }

    /**
     * Returns the appropriate string correspond to given Date.
     *
     * @param context application context
     * @param date    the date we want to display.
     * @return String that have the shape "Hours:Minutes" if its in the current
     * Day, and return String that have shape
     * "Month Day  Hours:Minutes AM/PM" otherwise.
     */
    public static String normalizeDate(Context context, Date date) {

        SimpleDateFormat simpleDateFormat;

        String stringDate;

        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTimeInMillis(date.getTime());

        Calendar currentDateCalendar = Calendar.getInstance();
        currentDateCalendar.setTimeInMillis(new Date().getTime());

        int day = dateCalendar.get(Calendar.DAY_OF_YEAR);
        int currentDay = currentDateCalendar.get(Calendar.DAY_OF_YEAR);

        int year = dateCalendar.get(Calendar.YEAR);
        int currentYear = currentDateCalendar.get(Calendar.YEAR);

        simpleDateFormat = new SimpleDateFormat("MMM d");
        stringDate = simpleDateFormat.format(date).toString();
        if ((year == currentYear) && ((day + 1) == currentDay)) {
            stringDate = context.getResources().getString(R.string.yesterday);
        }
        return stringDate;
    }

    /**
     * Check if a string is an email address or not
     */
    public static boolean validateEmailAddress(String email) {
        if (isBlank(email)) {
            return false;
        }
        Pattern pattern = Pattern
                .compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
        Matcher mat = pattern.matcher(email);

        if (mat.matches()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if a string is an youtube link or not
     */
    public static boolean validateYoutubeUrl(String url) {
        if (isBlank(url)) {
            return false;
        }
        Pattern pattern = Pattern
                .compile("(((http://)|(https://))?)(www\\.)?((youtube\\.com/)|(youtu\\.be)|(youtube)).+");
        Matcher mat = pattern.matcher(url);

        if (mat.matches()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the appropriate string that display difference between current
     * Date and given Date.
     *
     * @param date the date we want to display.
     * @return String that display difference between current Date and given
     * Date.
     */
    public static String normalizeDifferenceDate(Date date) {

        TimeAgo time = new TimeAgo();
        String timeString = "";

        if (date != null && date.getTime() > 0) {
            timeString = time.timeAgo(date.getTime());
        }

        return timeString;
    }

    /**
     * Returns the appropriate string that display difference between current
     * Date and given Date.
     *
     * @param date the date we want to display.
     * @return String that display difference between current Date and given
     * Date.
     */
    public static String normalizeDifferenceDateFuture(Date date) {

        TimeAgo time = new TimeAgo();
        String timeString = "";

        if (date != null && date.getTime() > 0) {
            timeString = time.timeUntil(date.getTime());
        }

        return timeString;
    }



    /**
     * Cut a given string from start index to last index.
     *
     * @param context    context where String visible.
     * @param startIndex start cutting index.
     * @param endIndex   end cutting index.
     * @param text       original text to take sub of it.
     * @return sub text of given text.
     */
    public static String subString(Context context, String text,
                                   int startIndex, int endIndex) {

        // If text content consists of more than one line with the first line
        // length < end index, take only the first line.
        if (text.contains("\n") && text.indexOf("\n") <= endIndex) {
            endIndex = text.indexOf("\n");
        }

        return text.substring(startIndex, endIndex)
                + context.getString(R.string.sub_string_suspension_marks);
    }


    /**
     * Return whether given string represent valid phone number or not.
     *
     * @param phoneNumber string represent phone number.
     * @return true if given string represent valid phone number, false
     * otherwise.
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {

        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }

        Pattern phoneNumberPattern = Patterns.PHONE;
        Matcher matcher = phoneNumberPattern.matcher(phoneNumber);

        return matcher.matches();
    }

    /**
     * Check if a given string is empty or null.
     */
    public static boolean isBlank(CharSequence text) {
        if (text == null || text.equals("") || text.equals("null"))
            return true;
        return false;
    }

    /**
     * Check if a given string is empty or null.
     */
    public static boolean isBlank(String string) {
        if (string == null || string.equals("") || string.equals("null"))
            return true;
        return false;
    }

    /**
     * Return substring after specific delimiter
     */
    public static String getStringAfter(String string, String delimiter) {
        return string.subSequence(string.lastIndexOf(delimiter) + 1,
                string.length()).toString();
    }

    /**
     * Takes a string - split it by white space and return the first part of it.
     * For: "John Smith" it would return "John"
     */
    public static String getFirstName(String fullName) {
        String[] splited = fullName.split("\\s+");
        return splited[0];
    }

    /**
     * Takes a string - split it by white space and return the second part of it
     * For: "John Smith" it would return "Smith"
     * Handles people with 3 names:"Gal Mojah Bracha" return "Mojah Bracha"
     * Handles people with 4 names:"Gal Mojah James Bracha" returns "Mojah James Bracha"
     */
    public static String getLastName(String fullName) {
        String[] splited = fullName.split("\\s+");
        if(splited.length > 3) {
            return splited[1] + " " + splited[2] + " " + splited[3];
        }
        if(splited.length > 2) {
            return splited[1] + " " + splited[2];
        }
        return (splited.length > 1) ? splited[1] : splited[0];
    }

    /**
     * Takes a boolean - and convert the true value to 'On' and the false value
     * to 'Off'.
     */
    public static String convertBooleanToOnOff(boolean flag) {
        if (flag)
            return ON;

        return OFF;
    }

    /**
     * Take the first name from given name depends on the first space.
     *
     * @param contactName full name.
     * @return first name.
     */
    public static String getFirstNameFromContactName(String contactName) {

        int stopIndex = contactName.indexOf(" ");

        if (stopIndex > 0) {

            return contactName.substring(0, stopIndex);
        }

        return contactName;
    }

    /**
     * Setup clickable span in certain SpannableString.
     */
    public static SpannableString setupClickableSpan(
            SpannableString spannableString, ClickableSpan clickableSpan,
            String allContent, String urlText, int foregroundColor,
            int backgroundColor) {
        spannableString.setSpan(clickableSpan, allContent.indexOf(urlText),
                allContent.indexOf(urlText) + urlText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ForegroundColorSpan(foregroundColor),
                allContent.indexOf(urlText), allContent.indexOf(urlText)
                        + urlText.length(), 0
        );
        spannableString.setSpan(new BackgroundColorSpan(backgroundColor),
                allContent.indexOf(urlText), allContent.indexOf(urlText)
                        + urlText.length(), 0
        );

        return spannableString;
    }

    /**
     * Get useful info from exception
     */
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static List union(final List list1, final List list2) {

        if (list1 == null && list2 == null)
            return null;

        if (list1 == null)
            return list2;

        if (list2 == null)
            return list1;

        final ArrayList result = new ArrayList(list1);
        result.addAll(list2);
        return result;
    }


    /**
     * Join multiple string with the given char between
     */
    public static String join(char c, String... strings) {
        return Joiner.on(",").join(strings);
    }


    /**
     * Capitalize (convert first character to upper case and others to lower case) given text.
     *
     * @param text text to be capitalized.
     * @return capitalized version of given text.
     */
    public static String capitalize(String text) {

        if (StringUtils.isBlank(text)) {
            return "";
        }

        String textLowerCase = text.toLowerCase();

        return String.valueOf(textLowerCase.charAt(0)).toUpperCase() + textLowerCase.substring(1, textLowerCase.length());
    }

    /**
     * Takes list of strings contains numbers and returns a string contains these numbers
     * separated by commas. Example: return
     * "3,5,64"
     */
    public static String convertNumbersForINQuery(List<String> strings) {

        String[] temp = new String[strings.size()];
        int index = 0;
        for (String string : strings) {
            temp[index++] = string;
        }
        return Joiner.on(",").join(temp);
    }

    /**
     * Remove unicode punctuation unicode characters from string
     * Sometimes in non latin alphabet string, formatting unicode characters
     * are added to the string.
     */
    public static String removeGeneralPunctuation(CharSequence str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (UnicodeBlock.of(c) != UnicodeBlock.GENERAL_PUNCTUATION)
                sb.append(c);
        }
        return sb.toString();
    }

}
