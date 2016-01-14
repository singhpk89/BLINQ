package com.blinq.utils;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.blinq.R;

import java.util.Calendar;

/**
 * @author Johan Hansson
 *         <p/>
 *         Holds GUI utilities.
 */
public final class UIUtils {

    private static final String TAG = UIUtils.class.getSimpleName();

    /**
     * View indices related to it's parent.
     */
    private static final int MESSAGE_CONTENT_INDEX = 1;
    private static final int SENDER_NAME_INDEX = 0;

    /**
     * manual Reference for test fairy views hiding.
     */
    private static int refID = 0x30000000;
    public static final long SHOW_KEYBOARD_DELAY = 200;

    private UIUtils() {
    }

    /**
     * Alert User.
     *
     * @param context - the Activity to use to open the Alert.
     * @param msg     - message to be shown.
     */
    public static void alertUser(Context context, String msg) {
        Toast t = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        t.setGravity(Gravity.BOTTOM, 0, (int) getPx(50, context));
        t.show();
    }

    /**
     * This method is a hook for background threads and async tasks that need to
     * update the UI. It does this by launching a runnable under the UI thread.
     */
    public static void showMessage(final Activity activity, final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertUser(activity, message);
            }
        });
    }

    /**
     * This method show keyboard on certain text view.
     */
    public static void showKeyboardOnTextView(final Activity activity,
                                              final TextView textView) {

        if (textView != null) {
            textView.postDelayed(new Runnable() {

                @Override
                public void run() {

                    textView.requestFocus();
                    if (activity != null)
                        AppUtils.showKeyboard(activity, textView);

                }
            }, UIUtils.SHOW_KEYBOARD_DELAY);
        }
    }

    /**
     * This function hide action bar from given activity.
     *
     * @param context activity to hide action bar from.
     */
    public static void hideActionbar(Context context) {
        ActionBar actionBar = ((Activity) context).getActionBar();

        try {
            actionBar
                    .getClass()
                    .getDeclaredMethod("setShowHideAnimationEnabled",
                            boolean.class).invoke(actionBar, false);
        } catch (Exception exception) {
            Log.d(TAG, "Unable to remove action bar hiding animation");
        }

        actionBar.hide();
    }

    /**
     * This function show action bar from given activity.
     *
     * @param context activity to show action bar from.
     */
    public static void showActionbar(Context context) {
        ActionBar actionBar = ((Activity) context).getActionBar();
        actionBar.show();
    }

    /**
     * Expands the activity layout to be shown on all screen size without action
     * bar and notification bar.
     */
    public static void fillScreen(Activity activity) {

        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * This function get the Typeface object for given font file.
     *
     * @param context  activity to get assets from.
     * @param fontFile file for specific font.
     * @return Typeface object for given font file
     */
    public static Typeface getFontTypeface(Context context, String fontFile) {

        return Typeface.createFromAsset(context.getAssets(), fontFile);
    }

    public interface Fonts {
        public static final String ROBOTO_LIGHT = "fonts/Roboto-Light.ttf";
        public static final String ROBOTO = "fonts/Roboto-Regular.ttf";
        public static final String ROBOTO_BOLD = "fonts/RobotoCondensed-Bold.ttf";
        public static final String ROBOTO_CONDENSED = "fonts/RobotoCondensed-Regular.ttf";

    }

    /**
     * Check the device time & update the application mode flag in shared
     * preferences.
     */
    public static void updateDayNightMode(Context context) {

        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();

        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR);
        int am_pm = calendar.get(Calendar.AM_PM);

        boolean isItNightTime = isItNightTime(hour, am_pm);
        editor.putBoolean(Constants.IS_DAY_MODE, isItNightTime);

        editor.commit();
    }

    private static boolean isItNightTime(int hour, int am_pm) {
        return (am_pm == 0 && hour >= Constants.START_HOUR_OF_AM_DAY_MODE && hour < Constants.END_HOUR_OF_AM_DAY_MODE)
                || (am_pm == 1 && hour >= Constants.START_HOUR_OF_PM_DAY_MODE && hour < Constants.END_HOUR_OF_PM_DAY_MODE);
    }

    /**
     * Return the PX value of given DP value.
     *
     * @param dp      value to convert in DP.
     * @param context application context.
     * @return PX value of given DP.
     */
    public static float getPx(float dp, Context context) {

        Resources r = context.getResources();

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                r.getDisplayMetrics());
    }

    /**
     * Get random dummy avatar.
     *
     * @param seed used to generate index for dummy avatar.
     * @return drawable id for specific avatar.
     */
    public static final int getDummyAvatar(int seed) {

        return DUMMY_AVATARS[0];
    }


    /**
     * Get screen width in pixels.
     *
     * @param activity activity to get screen width from.
     * @return screen width in pixels.
     */
    public static int getScreenWidth(Activity activity) {

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.widthPixels;
    }


    /**
     * Get the activity height in pixels.
     *
     * @param activity activity to get height for.
     * @return height of the activity in pixels.
     */
    public static int getScreenHeight(Activity activity) {

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.heightPixels;
    }

    /**
     * Get the height of the activity without action & status bars in pixels.
     *
     * @param activity activity to get height for.
     * @return height of the activity without action & status bars in pixels.
     */
    public static int getScreenHeightWithoutUpperBars(Activity activity) {

        return getScreenHeight(activity) - UIUtils.getStatusBarHeight(activity) - activity.getActionBar().getHeight();
    }

    /**
     * Get the height of the activity with action bar & and without status bars in pixels.
     *
     * @param activity activity to get height for.
     * @return height of the activity without action & status bars in pixels.
     */
    public static int getScreenHeightWithActionBar(Activity activity) {

        return getScreenHeight(activity) - UIUtils.getStatusBarHeight(activity);
    }

    public static int getDPI(int size, Activity activity) {

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (size * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
    }

    /**
     * Get the height of status bar in pixels.
     *
     * @param activity to get status bar height for.
     * @return height of the given activity status bar in pixels.
     */
    public static int getStatusBarHeight(Activity activity) {

        Rect rectangle = new Rect();
        Window window = activity.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);

        return rectangle.top;
    }

    /**
     * Return solid color drawable with rounded corners.
     *
     * @param color         drawable color.
     * @param cornersRadius corners radius.
     * @return rounded color drawable.
     */
    public static GradientDrawable getRoundedBackground(int color, float cornersRadius) {

        GradientDrawable roundedBackground = new GradientDrawable();
        roundedBackground.setColor(color);
        roundedBackground.setCornerRadius(cornersRadius);

        return roundedBackground;
    }

    /**
     * Hold reference to all dummy avatars.
     */
    private static final int DUMMY_AVATARS[] = new int[]{
            R.drawable.core_avatar };


    /**
     * Return the view of list item at specific position.
     *
     * @param itemPosition position of the item to get it's view.
     * @return view of the given item position.
     */
    public static View getTheViewOfListItemAtSpecificPosition(int itemPosition, ListView listView) {

        int firstDisplayedItemPosition = listView.getFirstVisiblePosition() - listView.getHeaderViewsCount();
        int targetItemPosition = itemPosition - firstDisplayedItemPosition;

        if (targetItemPosition < 0 || targetItemPosition >= listView.getChildCount()) {
            Log.w(TAG, "Unable to get view for desired position, because it's not being displayed on screen.");
            return null;
        }

        return listView.getChildAt(targetItemPosition);
    }
}
