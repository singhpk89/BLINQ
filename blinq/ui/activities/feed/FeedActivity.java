package com.blinq.ui.activities.feed;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.blinq.BlinqApplication;
import com.blinq.BuildConfig;
import com.blinq.MeCardHolder;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.ThemeManager;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.authentication.impl.AuthUtils.AuthAction;
import com.blinq.authentication.impl.AuthUtils.RequestStatus;
import com.blinq.authentication.impl.Google.GooglePlusAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.authentication.impl.provider.SocialWindowProvider;
import com.blinq.models.Contact;
import com.blinq.models.FeedDesign;
import com.blinq.models.FeedModel;
import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.blinq.models.SearchResult;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;
import com.blinq.provider.SearchProvider;
import com.blinq.service.ConnectivityService;
import com.blinq.service.FloatingDotService;
import com.blinq.service.notification.HeadboxNotificationManager;
import com.blinq.ui.activities.HeadboxBaseActivity;
import com.blinq.ui.activities.PopupSocialWindow;
import com.blinq.ui.activities.feedback.FeedbackActivity;
import com.blinq.ui.activities.instantmessage.InstantMessageActivity;
import com.blinq.ui.activities.search.SearchHandler;
import com.blinq.ui.activities.settings.SettingsActivity;
import com.blinq.ui.adapters.DrawerPlatformAdapter;
import com.blinq.ui.fragments.BlinqSocialListFragment;
import com.blinq.ui.fragments.FeedsFragmentList;
import com.blinq.ui.fragments.FeedsFragmentList.OnFeedSelectedListener;
import com.blinq.ui.fragments.InstantMessageFragment;
import com.blinq.ui.recyclers.RecycleSearchHolder;
import com.blinq.ui.views.SearchListView;
import com.blinq.ui.views.SearchViewBuilder;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.EmailUtils;
import com.blinq.utils.Log;
import com.blinq.utils.ServerUtils;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import dreamers.graphics.RippleDrawable;

/**
 * @author Johan Hansson
 */
public class FeedActivity extends HeadboxBaseActivity implements
        OnFeedSelectedListener, OnClickListener, AuthAction {

    private final String TAG = FeedActivity.class.getSimpleName();

    protected static final long CHECK_READY_DELAY = 2000;
    private static final long FIRST_TIME_REFRESH_DELAY = 1000;
    private static final long REFRESH_DELAY = 60 * 1000;


    boolean isServiceBounded = false;

    // Defines callbacks for service binding, passed to bindService().
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            // We've bound to ConnectorService, cast the binder and get
            // ConnectivityService instance
            isServiceBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBounded = false;

        }
    };

    private LinearLayout feedContentLayout;
    private LinearLayout searchLayout;
    private SearchView searchView;
    private TextView cancelSearchTextView;
    private DrawerLayout drawerLayout;
    private LinearLayout drawerLinearLayout;
    private RelativeLayout settingsButton;
    private RelativeLayout feedbackButton;
    private RelativeLayout meCardGeneratorButton;
    private LinearLayout ratingBarLayout;
    private RatingBar ratingBar;
    private ListView drawerPlatforms;
    private ActionBarDrawerToggle drawerToggle;
    private List<Platform> platformsFilters;
    private List<FeedModel> feeds;
    private SearchHandler searchHandler;
    private ListView searchResultList;
    private Provider provider;
    private boolean isDataReady = false;
    private BroadcastReceiver refreshReceiver;
    private Timer selfRefreshTime;
    private boolean loadAll = true;

    // Flag to indicate that refresh receiver is registered.
    private boolean isRegistered = false;
    private DrawerPlatformAdapter drawerPlatformAdapter;

    private FeedsFragmentList feedsFragmentList;
    private boolean fromSearch;
    private int firstLoadItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_feed_drawer);

        initiateFirstLoad();

        AppUtils.unRegisterActivityGoingIntoBackground(this);

        AppUtils.convertActivityFromTranslucent(this);

        provider = FeedProviderImpl.getInstance(getApplicationContext());
        drawerPlatformAdapter = new DrawerPlatformAdapter(this);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        clearNotificationHistory();

        manageNotifications(getIntent());

        init();

        loadFeedListFragment();
        customizeActionBar();
        customizeDrawer();

        sendEvent(AnalyticsConstants.OPENED_FEED_SCREEN_EVENT,
                false, AnalyticsConstants.ACTION_CATEGORY);

        analyticsSender.sendDailyUsedEvent();

    }

    private void manageNotifications(Intent intent) {

        if (intent == null || intent.getExtras() == null)
            return;

        boolean isSingleNotification = intent.getBooleanExtra(
                Constants.SINGLE_NOTIFICATION, false);

        if (isSingleNotification) {

            Log.d(TAG, "Open feed from notification");

            Intent i = new Intent(getApplicationContext(), InstantMessageActivity.class);
            int feedId = intent.getIntExtra(Constants.FEED_ID, 0);
            Platform platform = Platform.fromId(intent.getIntExtra(Constants.PLATFORM, 0));
            i.putExtra(Constants.FEED_ID, feedId);
            i.putExtra(InstantMessageFragment.SHOW_KEYBOARD, true);

            if (intent.hasExtra(Constants.OPEN_SOCIAL_WINDOW)) {
                i.putExtra(Constants.OPEN_SOCIAL_WINDOW, true);
                //Send Socialize event.
                Log.d(TAG, "Send the notification socialize action clicked event.");
                analyticsSender.sendNotificationSocializeActionClicked(platform);
            }

            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra(Constants.FROM_NOTIFICATION, true);
            startActivity(i);
        }

    }

    private void initiateFirstLoad() {

        if (getPreferencesManager().isAppFirstUse()) {

            getPreferencesManager().setProperty(PreferencesManager.FIRST_APP_USE, false);

        }

        firstLoadItems = getPreferencesManager().getABFeedHistoryValue();
        loadAll = firstLoadItems > 0;


    }

    private void showMixpanelUpdates() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                // Display Mixpanel Survey and Notfications
                BlinqAnalytics.showAllMixpanelUpdates(FeedActivity.this);

            }
        });

    }

    @Override
    protected void onStart() {

        super.onStart();
        // Bind to ConnectivityService
        Intent intent = new Intent(this, ConnectivityService.class);
        startService(intent);

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {

        Log.d(TAG, "onStop");


        // Unbind from the ConnectivityService
        if (isServiceBounded) {

            unbindService(serviceConnection);
            isServiceBounded = false;
        }
        if (!BlinqSocialListFragment.socialWindowItemClicked) {
            closeTheDot();
        }

        super.onStop();
    }

    private void closeTheDot() {
        if (lastSelectedPlatform == null)
            return;
        Intent i = new Intent(this, FloatingDotService.class);
        i.setAction(FloatingDotService.CLOSE_DOT_ACTION);
        i.putExtra(FloatingDotService.PLATFORM_EXTRA_TAG, lastSelectedPlatform.getId());
        startService(i);
    }

    @Override
    public boolean onSearchRequested() {

        return false;
    }

    /**
     * Initiate required data for the Feed Activity
     */
    private void init() {

        feedContentLayout = (LinearLayout) findViewById(R.id.linearLayoutForFeedContentList);

        searchLayout = (LinearLayout) findViewById(R.id.linearLayoutForSearchView);
        searchLayout.setOnClickListener(this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLinearLayout = (LinearLayout) findViewById(R.id.left_drawer);
        settingsButton = (RelativeLayout) findViewById(R.id.drawer_settings_layout);
        feedbackButton = (RelativeLayout) findViewById(R.id.drawer_feedback_layout);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        meCardGeneratorButton = (RelativeLayout) findViewById(R.id.drawer_me_card_generator_layout);
        drawerPlatforms = (ListView) findViewById(R.id.drawer_platforms);
        ratingBarLayout = (LinearLayout) findViewById(R.id.ratingBarLayout);

        preferencesManager = new PreferencesManager(this);

        new FeedsLoader().execute(null, null, null);

        initSearchView();

        initRefreshReceiver();
    }

    /**
     * Load the feeds fragment list into feeds activity.
     */
    private void loadFeedListFragment() {

        feedsFragmentList = new FeedsFragmentList();
        feedsFragmentList.setFeeds(feeds);
        feedsFragmentList.setPlatforms(platformsFilters);
        feedsFragmentList.setSearchLayout(searchLayout);
        feedsFragmentList.setAllFeed(true);

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.linearLayoutForFeedContentList, feedsFragmentList).commit();
    }

    private void initRefreshReceiver() {
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isDataReady)
                    updateOnMessageReceive(Long.valueOf(intent
                            .getStringExtra(Constants.FEED_EXTRA)));
            }
        };
    }

    /**
     * Initiate search Layout views and listener.
     */
    private void initSearchView() {

        searchView = (SearchView) findViewById(R.id.searchView);
        cancelSearchTextView = (TextView) findViewById(R.id.textViewForCancelSearch);
        searchResultList = (ListView) findViewById(R.id.listViewForSearchResult);
        searchResultList.setRecyclerListener(new RecycleSearchHolder());
        customizeSearchView();
    }

    /**
     * Customizes search view edit text and cancel button.
     */
    private void customizeSearchView() {


        final View.OnClickListener onClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {

                // Remove search view when clicked on dim background.
                ((SearchListView) feedsFragmentList.getListView())
                        .removeSearchView();

                // Send search analytics
                sendEvent(AnalyticsConstants.CLICKED_CANCEL_SEARCH_EVENT, false,
                        AnalyticsConstants.ACTION_CATEGORY);
                sendEvent(AnalyticsConstants.CANCELED_SEARCH_EVENT, false,
                        AnalyticsConstants.ACTION_CATEGORY);
            }
        };

        cancelSearchTextView.setOnClickListener(onClickListener);

        SearchViewBuilder searchViewBuilder = new SearchViewBuilder()
                .setContext(this)
                .setSearchView(searchView)
                .setCancelImageViewResourceId(R.drawable.search_cancel)
                .setSearchPlateBackgroundResourceId(R.drawable.search_bar_background)
                .setSearchBarTextColorResourceId(R.color.search_bar_text)
                .setTextContentTypeFace(UIUtils.getFontTypeface(this, UIUtils.Fonts.ROBOTO_CONDENSED))
                .setSearchContentTextCursorResource(R.drawable.custom_cursor)
                .build();

        searchHandler = new SearchHandler(this, searchResultList, searchView, SearchHandler.SearchViewMode.GENERAL);

        connectToSearchAPI();

    }

    /**
     * Remove the Search view
     */

    public void removeSearchView() {

        // Remove search view when clicked on dim background.
        ((SearchListView) feedsFragmentList.getListView()).removeSearchView();
    }

    /**
     * Initializes the connection with search API.
     */
    private void connectToSearchAPI() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));

    }

    @Override
    protected void onPause() {
        super.onPause();

        AppUtils.registerActivityGoingIntoBackground(this);

        if (isRegistered) {
            unregisterReceiver(refreshReceiver);
            selfRefreshTime.cancel();
            isRegistered = false;
        }
    }

    @Override
    public void onBackPressed() {

        handleSearchListOnBackPressed();

    }

    /**
     * Hide search list view if it is currently visible when pressing back key.
     */
    private void handleSearchListOnBackPressed() {

        try {

            if (feedsFragmentList != null) {

                SearchListView searchListView = ((SearchListView) feedsFragmentList
                        .getListView());

                if (searchListView != null
                        && searchListView.isSearchViewVisible()) {

                    ((SearchListView) feedsFragmentList.getListView())
                            .removeSearchView();
                } else {
                    super.onBackPressed();
                }

            } else {
                super.onBackPressed();
            }

        } catch (Exception e) {
            Log.e(TAG,
                    "an exception happened while hiding search view = "
                            + e.getMessage()
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "On Resumes");


        if (fromSearch) {
            fromSearch = false;
            return;
        }

        refreshDrawer();
        buildModeDesign();

        setPageDesign();

        if (FacebookAuthenticator.getInstance(this).isConnected()
                && !getPreferencesManager().isFacebookProfileLoaded()) {
            AuthUtils.UpdateUserProfile(this,
                    FacebookAuthenticator.getInstance(this), this,
                    Platform.FACEBOOK);
        }

        searchHandler.refresh();

        // Check if something needs update occur to restart the view.
        refreshView();

        // Clear notification history.
        clearNotificationHistory();

        final Handler handler = new Handler();

        Runnable runnable = new Runnable() {
            public void run() {
                // Wait until loading all data on application start in order to
                // handle received messages while loading.
                if (isDataReady) {
                    if (searchHandler.isClicked()) {

                        if (((SearchListView) feedsFragmentList.getListView())
                                .isSearchViewVisible()) {

                            ((SearchListView) feedsFragmentList.getListView())
                                    .removeSearchView();
                        }
                        searchHandler.setClicked(false);
                    }
                    updateOnResume();

                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(Constants.ACTION_REFRESH_INSERT);
                    registerReceiver(refreshReceiver, intentFilter);

                    selfRefreshTime = new Timer();
                    selfRefreshTime.schedule(new TimerTask() {

                        public void run() {

                            runOnUiThread(new Runnable() {

                                public void run() {
                                    updateLastMessageTime();
                                }
                            });
                        }
                    }, FIRST_TIME_REFRESH_DELAY, REFRESH_DELAY);

                    isRegistered = true;

                } else {

                    handler.postDelayed(this, CHECK_READY_DELAY);
                }
            }
        };

        handler.post(runnable);

        if (getPreferencesManager().getUserRatedTheApp()) {
            ratingBarLayout.setVisibility(View.GONE);
        }

        showMixpanelUpdates();
        checkIdleState();

    }

    /**
     * Check if the feed view needs to be refreshed.
     */
    private void refreshView() {

        if (BlinqApplication.refresh) {
            BlinqApplication.refresh = false;
            restartActivity();
        }
    }

    /**
     * Update the drawer by notifying the drawer adapter.
     */
    private void refreshDrawer() {
        drawerPlatformAdapter.setActivity(this);
        drawerPlatformAdapter.notifyDataSetChanged();
    }

    /**
     * Cancel and clear headbox notifications from the notification status bar.
     */
    private void clearNotificationHistory() {

        HeadboxNotificationManager.cancelNotifications();
        HeadboxNotificationManager.clearNotificationHistory();
    }

    /**
     * Restart the activity.
     */
    private void restartActivity() {

        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }


    private void setPageDesign() {

        feedContentLayout.setBackgroundColor(FeedDesign.getInstance()
                .getFeedActivityBackgroundColor());
    }

    /**
     * Check the mode of the application and build the design object for it
     * depends on the application mode (day/night).
     */
    private void buildModeDesign() {

        ThemeManager.updateThemeData(getApplicationContext());
    }

    /**
     * Apply some customization to the contents of the action bar.
     */
    private void customizeActionBar() {

        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_background));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    /**
     * Apply some customization to the contents of the drawer.
     */
    private void customizeDrawer() {

        drawerPlatforms.setAdapter(drawerPlatformAdapter);

        initDrawerRateWidget();

        initDrawerListener();
    }

    private void initDrawerRateWidget() {
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                int rate = (int) v;
                ratingBar.setRating(0f);
                if (rate <= 3) {
                    EmailUtils.sendLowRateEmail(FeedActivity.this);
                    return;
                }
                getPreferencesManager().setUserRatedTheApp();
                openPlayStoreView();
            }

            private void openPlayStoreView() {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
                startActivity(intent);
            }
        });
    }


    /**
     * Initialize drawer related event listener.
     */
    private void initDrawerListener() {

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.drawer_icon, R.string.drawer_open,
                R.string.drawer_close) {

            public void onDrawerClosed(View view) {

           /*     spreadTheLoveButton.setText(AppUtils
                        .getRandomString(getApplicationContext(),
                                R.array.spread_the_love_titles));*/
                super.onDrawerClosed(view);
                invalidateOptionsMenu();

            }

            public void onDrawerOpened(View drawerView) {
                sendEvent(
                        AnalyticsConstants.DRAWER_OPENED_EVENT, false,
                        AnalyticsConstants.ACTION_CATEGORY);
                drawerPlatformAdapter.refresh();
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                View container = findViewById(R.id.content_frame);
                container.setTranslationX(slideOffset * drawerView.getWidth());
                super.onDrawerSlide(drawerView, slideOffset);
            }

        };

        drawerLayout.setDrawerListener(drawerToggle);
        initDrawerButtonsListener();

    }

    /**
     * Initialize drawer related click listener.
     */
    private void initDrawerButtonsListener() {

        drawerLinearLayout.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        RippleDrawable.createRipple(settingsButton,
                getResources().getColor(R.color.drawer_actions_background_pressed));
        settingsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                startSettingsActivity();
            }
        });

        RippleDrawable.createRipple(feedbackButton,
                getResources().getColor(R.color.drawer_actions_background_pressed));
        feedbackButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                // Go to feedback activity
                Intent feedbackIntent = new Intent(getApplicationContext(),
                        FeedbackActivity.class);
                feedbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(feedbackIntent);
            }
        });

        if (AppUtils.isDebug(this)) {
            meCardGeneratorButton.setVisibility(View.VISIBLE);
            RippleDrawable.createRipple(meCardGeneratorButton,
                    getResources().getColor(R.color.drawer_actions_background_pressed));
            meCardGeneratorButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    showMeCardGeneratorDialog();
                }
            });
        }

    }

    private void showMeCardGeneratorDialog() {
        final Activity context = this;
        LinearLayout linearLayout = new LinearLayout(FeedActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText email = new EditText(FeedActivity.this);
        final EditText facebookId = new EditText(FeedActivity.this);
        final EditText facebookName = new EditText(FeedActivity.this);
        email.setHint("Email address");
        email.setPadding(10, 20, 0, 20);
        facebookName.setHint("Facebook name");
        facebookId.setPadding(10, 20, 0, 20);
        facebookId.setHint("Facebook id");
        facebookId.setPadding(10, 20, 0, 20);
        linearLayout.addView(email, lp);
        linearLayout.addView(facebookId, lp);
        linearLayout.addView(facebookName, lp);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("MeCard generator")
                .setView(linearLayout)
                .setPositiveButton("Generate", new DialogInterface.OnClickListener() {

                    private ServerUtils.OnGetContactInformationListener listener = new ServerUtils.OnGetContactInformationListener() {
                        @Override
                        public void onGetContactInformation(boolean success) {
                            if (!success) {
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "Failed to get server information", Toast.LENGTH_LONG).show();
                                    }
                                });
                                return;
                            }
                            SocialWindowProvider.getNewInstance(context, 1).fetchSocialPosts(true, false);
                            Intent intent = new Intent(context, PopupSocialWindow.class);
                            intent.putExtra(Constants.FEED_ID, 1);
                            intent.putExtra(Constants.PLATFORM, Platform.NOTHING.getId());
                            intent.putExtra(Constants.FROM_FEED, true);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    };

                    public void onClick(DialogInterface dialog, int id) {
                        final Map<Platform, List<MemberContact>> params = new HashMap<Platform, List<MemberContact>>();
                        String fbId = facebookId.getText().toString();
                        String em = email.getText().toString();
                        String fbname = facebookName.getText().toString();
                        if (!StringUtils.isBlank(fbId)) {
                            MemberContact mc = new MemberContact(Platform.FACEBOOK, fbId, fbId);
                            params.put(Platform.FACEBOOK, Arrays.asList(mc));
                        }
                        if (!StringUtils.isBlank(em)) {
                            MemberContact mc = new MemberContact(Platform.EMAIL, em, em);
                            params.put(Platform.EMAIL, Arrays.asList(mc));
                        }
                        if (!StringUtils.isBlank(fbname)) {
                            MemberContact mc = new MemberContact(Platform.EMAIL, fbname, fbname);
                            params.put(Platform.FACEBOOK, Arrays.asList(mc));
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                MeCardHolder.getInstance().clearData();
                                ServerUtils.getInstance(context).getContactInformation(params, 1, listener);
                            }
                        }).start();

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            AppUtils.addContact(getApplicationContext(), data);
        }

        FacebookAuthenticator.getInstance(this).onActivityResult(this,
                requestCode, resultCode, data);

        GooglePlusAuthenticator.getInstance(this).onActivityResult(this,
                requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Check if the Feed view was idle to return it to home screen.
     */
    private void checkIdleState() {
        if (AppUtils.isActivityIdle(this)) {
            Log.d(TAG, "Detect feed idle state.");
            drawerLayout.closeDrawers();
            removeSearchView();

        } else {
            AppUtils.unRegisterActivityGoingIntoBackground(this);
        }
    }

    /**
     * Get feeds data from database.
     */
    private void getFeedsData() {

        platformsFilters = new ArrayList<Platform>();
        platformsFilters.add(Platform.CALL);
        platformsFilters.add(Platform.SMS);
        platformsFilters.add(Platform.FACEBOOK);
        platformsFilters.add(Platform.HANGOUTS);
        platformsFilters.add(Platform.MMS);
        platformsFilters.add(Platform.SKYPE);
        platformsFilters.add(Platform.WHATSAPP);
        platformsFilters.add(Platform.EMAIL);

        feeds = provider.getFeeds(0, Constants.NUMBER_OF_FEEDS_TO_LOAD_FIRST, platformsFilters);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feed_settings_menu, menu);

        if (firstLoadItems == 0) {
            //Set the icon for AB Feed History - None - Mode.
            menu.findItem(R.id.action_search).setIcon(R.drawable.core_new_message);
        }
        sendEvent(AnalyticsConstants.FEED_ANDROID_OPTION,
                false, AnalyticsConstants.SETTINGS_CATEGORY);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch (item.getItemId()) {

            case R.id.action_search:
                ((SearchListView) feedsFragmentList.getListView())
                        .showSearchView(AnalyticsConstants.ICON_VALUE);
                sendEvent(AnalyticsConstants.CLICK_ON_SEARCH_ICON,
                        false, AnalyticsConstants.ACTION_CATEGORY);
                return true;

            // settings
            case R.id.action_settings:
                startSettingsActivity();
                sendEvent(AnalyticsConstants.FEED_ANDROID_OPTION_SETTINGS,
                        false, AnalyticsConstants.SETTINGS_CATEGORY);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Start the Headbox settings activity.
     */
    private void startSettingsActivity() {
        Intent settingsActivity = new Intent(getApplicationContext(),
                SettingsActivity.class);
        startActivity(settingsActivity);

    }
    @Override
    public void onRestart() {
        super.onRestart();
    }

    /**
     * Called when activity start-up is complete
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    /* Called after calling invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerLinearLayout);
        // TODO: Uncomment if search is back
        //menu.findItem(R.id.action_search).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    private void startDotService(int feedId, Platform platform) {
        MeCardHolder.getInstance().clearData();
        SocialWindowProvider.getNewInstance(this, feedId).fetchSocialPosts(false, true);
        Intent intent = new Intent(this, FloatingDotService.class);
        intent.putExtra(FloatingDotService.FEED_ID_EXTRA_TAG, feedId);
        intent.putExtra(FloatingDotService.IS_OPEN_SOCIAL_WINDOW_TAG, true);
        intent.putExtra(FloatingDotService.PLATFORM_EXTRA_TAG, platform.getId());
        this.startService(intent);
    }

    private Platform lastSelectedPlatform;

    @Override
    public void onFeedSelected(Uri feedUri, Platform platform) {

        //Debug.delayExecution(10000);
        lastSelectedPlatform = platform;
        int selectedFeedId = Integer.parseInt(feedUri.toString());

        startDotService(selectedFeedId, platform);

        //You can remove the return to launch the following code that launches the Instant Message window


//        Log.d(TAG, "Open conversation id = " + selectedFeedId);
//
//        Intent instantMessageIntent = new Intent(getApplicationContext(),
//                InstantMessageActivity.class);
//        instantMessageIntent.putExtra(Constants.FEED_ID, selectedFeedId);
//        instantMessageIntent.putExtra(InstantMessageFragment.SHOW_KEYBOARD,
//                false);
//        instantMessageIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        // Apply animation on transition from feed to com-log activity.
//        Bundle activityTransitionBundleAnimation =
//                ActivityOptions.makeCustomAnimation(getApplicationContext(),
//                        R.anim.comlog_activity_start_animation,
//                        R.anim.feed_activity_close_animation).toBundle();
//        startActivity(instantMessageIntent, activityTransitionBundleAnimation);
//
//        provider.markFeedAsRead(getApplicationContext(), selectedFeedId);
//        updateOnRead(selectedFeedId);
//
//        sendEvent(AnalyticsConstants.PERSON_CLICKED_EVENT,
//                false, AnalyticsConstants.ACTION_CATEGORY);
//        sendEvent(AnalyticsConstants.CLICK_TO_OPEN_COMLOG_EVENT,
//                false, AnalyticsConstants.ACTION_CATEGORY);
    }

    /**
     * Move over each fragment in the view pager and updates it's content
     * according to received feed.
     */
    public void updateOnMessageReceive(Long feedId) {

        List<FeedModel> newFeeds = new ArrayList<FeedModel>();

        FeedModel updatedFeed = provider.getFeed(feedId,
                Platform.ALL);

        if (updatedFeed != null) {
            newFeeds.add(updatedFeed);
        }

        feedsFragmentList.updateFeeds(newFeeds);
    }

    /**
     * Move over each fragment in the view pager and updates last message time
     * for each feed.
     */
    public void updateLastMessageTime() {

        feedsFragmentList.updateFeedLastMessageTime();
    }

    /**
     * Move over each fragment in the view pager and updates modified feeds.
     */
    public void updateOnResume() {

        if (feedsFragmentList != null) {

            // Save all modified feed to mark them as unmodified later.
            List<FeedModel> modifiedFeeds = provider
                    .getModifiedFeeds();
            feedsFragmentList.updateFeeds(modifiedFeeds);
            provider.setFeedsAsUnmodified(modifiedFeeds);

        }

    }

    /**
     * Updates given feed as read in all fragments.
     *
     * @param selectedFeedId The ID of feed to update.
     */
    private void updateOnRead(int selectedFeedId) {

        feedsFragmentList.markFeedAsRead(selectedFeedId);
    }

    /**
     * Invoked when an intent receives.
     */
    @Override
    protected void onNewIntent(Intent intent) {

        Log.i(TAG, "Action = " + intent.getAction());

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            fromSearch = true;
            handleIntent(intent);
        }
    }

    /**
     * Handle search intents sent from search view.
     */
    public void handleIntent(Intent intent) {

        // Show results when the intent action is search.
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            Cursor cursor = getApplicationContext().getContentResolver().query(
                    SearchProvider.CONTENT_URI, null, null, null, null);

            List<SearchResult> searchResults;
            if (cursor != null) {

                if (!cursor.isClosed()) {

                    searchResults = provider.convertToSearchResult(cursor);
                    searchHandler.setSearchResults(searchResults);

                    searchHandler.new SearchResultHandler().execute(null, null,
                            null);
                }

            } else {
                searchResults = new ArrayList<SearchResult>();
                searchHandler.setSearchResults(searchResults);

                searchHandler.new SearchResultHandler().execute(null, null,
                        null);
            }

        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.linearLayoutForSearchView:

                // Remove search view when clicked on dim background.
                ((SearchListView) feedsFragmentList.getListView()).removeSearchView();

                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG, "onDestroy");

        if (isRegistered) {
            unregisterReceiver(refreshReceiver);
            selfRefreshTime.cancel();
            isRegistered = false;
        }
        complete();
        super.onDestroy();
    }

    @Override
    public void onLoginCompleted(Platform platform) {
    }

    @Override
    public void onUserProfileLoaded(Platform platform, Contact profile, boolean success) {
    }

    @Override
    public void onContactsUpdated(List<Contact> contacts) {
    }

    @Override
    public void onInboxUpdated(RequestStatus status) {
        // TODO REFRESH THE FEED VIEW.
        if (status == RequestStatus.COMPLETED)
            restartActivity();
    }

    /**
     * Asynchronous task to load feeds from database in the background then refresh list.
     *
     * @author Johan Hansson.
     */
    private class FeedsLoader extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {

            isDataReady = false;
            getFeedsData();
            isDataReady = true;

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            feedsFragmentList.setFeeds(feeds);
            feedsFragmentList.refresh();
        }
    }
}