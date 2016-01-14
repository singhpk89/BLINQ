package com.blinq.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ViewFlipper;

import com.blinq.BlinqApplication;
import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.AnalyticsSender;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.Authenticator.ProfileRequestCallback;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.authentication.impl.AuthUtils.AuthAction;
import com.blinq.authentication.impl.AuthUtils.RequestStatus;
import com.blinq.authentication.impl.Google.GooglePlusAuthenticator;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.models.Contact;
import com.blinq.models.FeedModel;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.MemberContact;
import com.blinq.models.MessageType;
import com.blinq.models.Platform;
import com.blinq.models.social.window.SocialWindowPost;
import com.blinq.provider.CallsManager;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.provider.Provider;
import com.blinq.service.FloatingDotService;
import com.blinq.service.notification.HeadboxNotificationManager;
import com.blinq.ui.activities.instantmessage.InstantMessageActivity;
import com.blinq.ui.activities.search.SearchHandler;
import com.blinq.ui.adapters.MessageListAdapter;
import com.blinq.ui.adapters.PlatformCoverListAdapter;
import com.blinq.ui.animations.AnimationManager;
import com.blinq.ui.platformcircle.PlatformCircleManager;
import com.blinq.ui.platformcircle.PlatformCircleSettings;
import com.blinq.ui.recyclers.RecycleMessageListHolder;
import com.blinq.ui.views.UndoActionView;
import com.blinq.ui.views.UndoActionView.UndoListener;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.DialogUtils;
import com.blinq.utils.EmailUtils;
import com.blinq.utils.ExternalAppsUtils;
import com.blinq.utils.Log;
import com.blinq.utils.NetworkUtils;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;
import com.touchmenotapps.widget.radialmenu.menu.v1.RadialMenuItem;
import com.touchmenotapps.widget.radialmenu.menu.v1.RadialMenuItem.RadialMenuItemClickListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import me.imid.swipebacklayout.lib.app.SwipeBackActivity;

/**
 * Initialize fragment of Comlog view.
 *
 * @author Johan Hansson
 */
@SuppressLint("ValidFragment")
public class InstantMessageFragment extends Fragment implements
        RadialMenuItemClickListener, AuthAction, OnGlobalLayoutListener {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final String TAG = "InstantMessageFragment";

    private static final int FIRST_LOAD_LIMIT = 40;
    private static final int LOAD_MORE_LIMIT = 40;

    // 1 minute timer to remove typing message if there are no updates received
    // from contact.
    private static final int TYPING_WATCHDOG_TIMER = 60000;

    private static final long FIRST_TIME_REFRESH_DELAY = 1000;
    private static final int DISPLAY_UNDO_MERGE_DIALOG_TIME = 5000;

    private static final long REFRESH_DELAY = 60 * 1000;
    public static final String SHOW_KEYBOARD = "ShowKeyboard";

    // Set user as not typing if there are no changes in the message input for 1
    // second.
    private static final int TYPING_TIMER = 1000;

    private static final int NOT_FOUND_ERROR = -1;


    // ---------------------------------------------------------------------------
    // Views
    // ---------------------------------------------------------------------------
    private View root;


    private ListView messageList;
    private ViewFlipper accountFlipper;
    private EditText messageInput;
    private ImageButton sendButton;
    private View bottomLayout;
    private Menu menu;


    private Provider provider;

    private int feedId;
    private FeedModel friendFeed;
    private int currentPlatformIndex;

    private MessageListAdapter messageAdapter;
    private boolean hasMoreMessageToLoad = true;
    private Context context;

    private PlatformCircleManager platformCircleManager;
    /**
     * Defined for sending SMS/MMS messages.
     */
    private BroadcastReceiver refreshReceiver;
    private Timer selfRefreshTime;
    private boolean isPaused = false;
    private InputMethodManager inputMethodManager;
    private HeadboxMessage draftMessage;
    private HashMap<Platform, List<MemberContact>> memberContacts;
    private HashMap<Platform, String> lastContactsToReply;

    private String from = "Message";
    private String to = "";
    /**
     * The selected menu item index.
     */
    private int index = -1;
    private Platform platform;
    private boolean isContactable = true;
    private boolean showKeyboard;
    private boolean hasSocialWindow = false;

    private boolean isRefreshed = false;
    private Platform typingStatePlatform;
    private Platform mergePlatform = Platform.NOTHING;

    private TextWatcher textChangeWatcher;
    private AnalyticsSender analyticsSender;

    private List<List<SocialWindowPost>> platformCovers;
    private FragmentActivity activity;
    private FacebookAuthenticator facebookAuthenticator;
    private GooglePlusAuthenticator googleAuthenticator;

    private PreferencesManager preferencesManager;
    private AnimationManager animationManager;
    private List<Platform> sortedPlatformsList;
    private boolean hasContact = false;
    private boolean coverPagerHidden = false;
    private boolean platformIndexSet = false;


    // -------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------
    private OnSocialWindowAffected onSocialWindowAffected;
    private OnTypingMessageChanged onTypingMessageChanged;
    private OnUiUpdate onUiUpdate;


    @Override
    public void onGlobalLayout() {

        if (isKeyboardVisible()) {

            InstantMessageActivity.SocialWindowStatus socialWindowStatus = ((InstantMessageActivity) activity).getSocialWindowStatus();

            if (socialWindowStatus != InstantMessageActivity.SocialWindowStatus.PROCESSING
                    && socialWindowStatus == InstantMessageActivity.SocialWindowStatus.DEFAULT) {

                onSocialWindowAffected.hideSocialWindow();
            }

            messageList.postDelayed(new Runnable() {

                @Override
                public void run() {

                    messageList.smoothScrollToPosition(messageAdapter
                            .getCount() - 1);
                }
            }, 200);
        }
    }


    /**
     * Check if the keyboard visible or not depends on the position of bottom layout of the com-log activity.
     *
     * @return true if bottom layout not in the bottom of screen, false otherwise.
     */
    public boolean isKeyboardVisible() {

        return bottomLayout.getY()
                < (UIUtils.getScreenHeightWithoutUpperBars(activity) - bottomLayout.getHeight());
    }


    public void setMergePlatform(Platform mergePlatform) {
        this.mergePlatform = mergePlatform;
    }

    public void setHasCoverPage(boolean hasCoverPage) {
        this.hasSocialWindow = hasCoverPage;
    }


    public void setCoverPagerHidden(boolean coverPagerHidden) {
        this.coverPagerHidden = coverPagerHidden;
    }

    public void setCurrentPlatformIndex(int currentPlatformIndex) {
        this.currentPlatformIndex = currentPlatformIndex;
    }

    public void setPlatformIndexSet(boolean platformIndexSet) {
        this.platformIndexSet = platformIndexSet;
    }

    public List<Platform> getPlatformList() {
        return sortedPlatformsList;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public InstantMessageFragment() {

    }

    public InstantMessageFragment(FragmentActivity activity) {
        this.activity = activity;
        initComponents();
    }

    /**
     * Initialize the data components
     */
    private void initComponents() {

        if (activity != null) {

            context = activity.getApplicationContext();
            feedId = activity.getIntent().getExtras().getInt(Constants.FEED_ID);
            provider = FeedProviderImpl.getInstance(context);

            if (provider != null) {
                friendFeed = provider.getFeed(feedId);
                memberContacts = provider.getContacts(feedId);
            }
            if (friendFeed != null && friendFeed.getContact() != null) {
                BlinqApplication.contactId = friendFeed.getContact()
                        .getContactId();
            }

        }

        initSortedPlatformList();

    }

    private void initSortedPlatformList() {
        // Sorted platform List.
        sortedPlatformsList = new ArrayList<Platform>();
        sortedPlatformsList.add(Platform.FACEBOOK);
        sortedPlatformsList.add(Platform.TWITTER);
        sortedPlatformsList.add(Platform.INSTAGRAM);
        currentPlatformIndex = 0;

        List<Platform> tempPlatformsList = new ArrayList<Platform>();
        tempPlatformsList.add(Platform.FACEBOOK);
        // add member contacts platforms
        if (memberContacts != null) {
            for (Platform platform : sortedPlatformsList) {

                if (memberContacts.keySet().contains(platform) && platform != Platform.FACEBOOK) {
                    tempPlatformsList.add(platform);
                }
            }

        }

        // add connected platforms

        for (Platform platform : sortedPlatformsList) {
            if (!tempPlatformsList.contains(platform)
                    && AuthUtils.isConnected(platform, activity)) {
                tempPlatformsList.add(platform);
            }
        }

        for (Platform platform : sortedPlatformsList) {
            if (!tempPlatformsList.contains(platform))
                tempPlatformsList.add(platform);
        }

        sortedPlatformsList = tempPlatformsList;

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        boolean isPlatformCircleShown = platformCircleManager
                .isPlatformCircleShown();
        platformCircleManager.dismiss();
        platformCircleManager = new PlatformCircleManager(activity, this);
        if (isPlatformCircleShown) {
            platformCircleManager.showPlatformCircle(accountFlipper, friendFeed
                            .getContact().getIdentifier(), memberContacts,
                    isContactable
            );
        }

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {

        Log.d(TAG, "onResume");

        AppUtils.startMainService(activity.getApplicationContext());

        onUiUpdate.updateActionBar(this);

        if (preferencesManager.isComlogFirstUse()) {

            AppUtils.hideKeyboard(activity);
            onSocialWindowAffected.hideWithoutAnimation();

            animationManager = new AnimationManager(this,
                    root);
            animationManager.applyFirstTimeAnimation();

            currentPlatformIndex = 0;

        } else {

            onSocialWindowAffected.showWithoutAnimation();

            refreshOnResume();
            fillUserData();
        }

        // Clear notification history.
        HeadboxNotificationManager.cancelNotifications();
        HeadboxNotificationManager.clearNotificationHistory();
        facebookAuthenticator = FacebookAuthenticator.getInstance(activity);
        googleAuthenticator = GooglePlusAuthenticator.getInstance(activity);
        provider = FeedProviderImpl.getInstance(context);

        if (facebookAuthenticator.isConnected()
                && !preferencesManager.isFacebookProfileLoaded()) {
            UpdateUserProfile(facebookAuthenticator,
                    new AuthUtils().new ProfileRequestCallback(
                            Platform.FACEBOOK, activity, this)
            );
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_REFRESH_INSERT);
        intentFilter.addAction(Constants.ACTION_REFRESH_UPDATE);
        intentFilter.addAction(Constants.ACTION_REFRESH_MESSAGE_TYPE_CHANGE);

        activity.registerReceiver(refreshReceiver, intentFilter);

        selfRefreshTime = new Timer();
        selfRefreshTime.schedule(new TimerTask() {
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        onUiUpdate.refresh();
                    }
                });

            }
        }, FIRST_TIME_REFRESH_DELAY, REFRESH_DELAY);

        openExternalAppConversation(mergePlatform);

        selectCurrentPlatformIndex();
        if (!isRefreshed) {
            accountFlipper.setDisplayedChild(currentPlatformIndex);
        }
        isRefreshed = false;
        setSendButtonSelector(currentPlatformIndex);

        super.onResume();

        showUndoMergeView();

        watchKeyboardStatus();
    }

    /**
     * Show the undo merge view after the merge operation
     */
    private void showUndoMergeView() {

        if (preferencesManager.getProperty(PreferencesManager.IS_SHOW_UNDO_MERGE,
                false)) {
            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    UndoActionView undoActionView = new UndoActionView(root
                            .findViewById(R.id.undobar), undoMergeListener,
                            DISPLAY_UNDO_MERGE_DIALOG_TIME
                    );
                    preferencesManager.setProperty(
                            PreferencesManager.IS_SHOW_UNDO_MERGE, false);
                    undoActionView.showUndoBar(false,
                            activity.getString(R.string.merge_undo_message), null);

                }
            });

        }

    }

    /**
     * Select of default chat platform.
     */
    private void selectCurrentPlatformIndex() {
        draftMessage = provider.getDraftMessage(feedId);
        if (draftMessage != null) {
            messageInput.setText(draftMessage.getBody());
            provider.deleteDraftMessage(feedId);
        }
        if (platformIndexSet)
            return;
        checkIdleState();
        if (draftMessage != null) {
            currentPlatformIndex = mapPlatformToRetroDialerItem(draftMessage
                    .getPlatform());
            showKeyboardOnMessageInput();

        } else if (mergePlatform != Platform.NOTHING
                && mergePlatform != Platform.TWITTER
                && mergePlatform != Platform.INSTAGRAM) {

            currentPlatformIndex = mapPlatformToRetroDialerItem(mergePlatform);
            mergePlatform = Platform.NOTHING;

        } else if (messageAdapter.getCount() > 0) {

            HeadboxMessage lastMessage = (HeadboxMessage) messageAdapter
                    .getItem(messageAdapter.getCount() - 1);
            currentPlatformIndex = mapPlatformToRetroDialerItem(lastMessage
                    .getPlatform());

        } else {

            platform = getCurrentPlatformIndexForEmptyFeed();
            currentPlatformIndex = mapPlatformToRetroDialerItem(platform);
        }

    }

    /**
     * Get a proper platform to show on the conversation bottom left area according to its priority.
     *
     * @return - Platform.
     */
    private Platform getCurrentPlatformIndexForEmptyFeed() {

        Set<Platform> platforms = getContactPlatforms();
        Platform selected = Platform.ALL;
        for (Platform platform : platforms) {
            if (platform.getId() < selected.getId())
                selected = platform;
        }

        if (selected == Platform.CALL) {
            selected = Platform.SMS;
        }

        return selected;
    }

    /**
     * Remove Instagram/Twitter from member platform if there are another
     * platforms
     *
     * @return
     */
    private Set<Platform> getContactPlatforms() {
        Set<Platform> memberPlatforms = memberContacts.keySet();

        if (memberPlatforms.size() > 1) {
            if (memberPlatforms.contains(Platform.TWITTER))
                memberPlatforms.remove(Platform.TWITTER);

            if (memberPlatforms.size() > 1) {
                if (memberPlatforms.contains(Platform.INSTAGRAM))
                    memberPlatforms.remove(Platform.INSTAGRAM);
            }

        }

        return memberPlatforms;
    }

    /**
     * Open skype,whatsapp,and email conversation for a certain contact.
     */
    private void openExternalAppConversation(Platform platform) {

        if (platform == null)
            return;

        String contact = getContactId(platform);

        if (contact == null)
            return;

        switch (platform) {
            case WHATSAPP:
                ExternalAppsUtils.openWhatsappConversation(activity, contact);
                break;
            case SKYPE:
                ExternalAppsUtils.openSkypeChat(context, contact);
                break;
            case EMAIL:
                EmailUtils.sendEmail(new String[]{contact}, "", "", context);
                break;
            default:
                break;
        }

    }

    /**
     * Check if the Comlog was idle to return it to home screen.
     */
    private void checkIdleState() {
        if (AppUtils.isActivityIdle(activity) && draftMessage == null) {
            Log.d(TAG, "Detect comlog idle state.");
            activity.onBackPressed();

        } else {
            AppUtils.unRegisterActivityGoingIntoBackground(activity);
        }

    }

    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof InstantMessageActivity)
            ((InstantMessageActivity) activity).setRedirectToHomeEnabled(true);

        if (this.activity == null) {
            this.activity = (FragmentActivity) activity;
            initComponents();
        }

        super.onAttach(activity);

        // Get instance of the listener implemented by given activity.
        try {
            onSocialWindowAffected = (OnSocialWindowAffected) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + OnSocialWindowAffected.class.getSimpleName());
        }


        // Get instance of the listener implemented by given activity.
        try {
            onTypingMessageChanged = (OnTypingMessageChanged) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + OnTypingMessageChanged.class.getSimpleName());
        }


        try {
            onUiUpdate = (OnUiUpdate) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement " + OnTypingMessageChanged.class.getSimpleName());
        }


    }

    @Override
    public void onDetach() {
        super.onDetach();
        onSocialWindowAffected = null;
    }

    /**
     * Map platform to its index on retro dialer.
     *
     * @param platform current selected platform.
     * @return platform index of current selected platform.
     */
    private int mapPlatformToRetroDialerItem(Platform platform) {

        if (platform == null)
            return PlatformCircleSettings.MESSAGE_INDEX;

        switch (platform) {
            case SMS:
                return PlatformCircleSettings.MESSAGE_INDEX;
            case FACEBOOK:
                return PlatformCircleSettings.FACEBOOK_INDEX;
            case HANGOUTS:
                return PlatformCircleSettings.HANGOUTS_INDEX;
            case SKYPE:
                return PlatformCircleSettings.SKYPE_INDEX;
            case TWITTER:
                return PlatformCircleSettings.TWITTER_INDEX;
            case WHATSAPP:
                return PlatformCircleSettings.WHATSAPP_INDEX;
            case EMAIL:
                return PlatformCircleSettings.MAIL_FILLER_INDEX;
            case INSTAGRAM:
                return PlatformCircleSettings.INSTAGRAM_INDEX;
            default:
                return PlatformCircleSettings.MESSAGE_INDEX;
        }

    }

    /**
     * Map retro dialer menu item to its platform.
     *
     * @param index - menu item index.
     * @return - platform.
     */
    private Platform mapRetroDialerItemToPlatform(int index) {

        switch (index) {
            case PlatformCircleSettings.MESSAGE_INDEX:
                return Platform.SMS;
            case PlatformCircleSettings.FACEBOOK_INDEX:
                return Platform.FACEBOOK;
            case PlatformCircleSettings.HANGOUTS_INDEX:
                return Platform.HANGOUTS;
            case PlatformCircleSettings.SKYPE_INDEX:
                return Platform.SKYPE;
            case PlatformCircleSettings.TWITTER_INDEX:
                return Platform.TWITTER;
            case PlatformCircleSettings.WHATSAPP_INDEX:
                return Platform.WHATSAPP;
            case PlatformCircleSettings.MAIL_FILLER_INDEX:
                return Platform.EMAIL;
            default:
                return Platform.SMS;
        }
    }

    @Override
    public void onPause() {

        if (preferencesManager.isComlogFirstUse()) {

            animationManager.stopAnimation();
        }
        AppUtils.hideKeyboard(activity);
        activity.unregisterReceiver(refreshReceiver);
        selfRefreshTime.cancel();
        isPaused = true;
        saveDraftMessage();
        super.onPause();
        Log.d(TAG, "pause");
    }

    private void saveDraftMessage() {
        if (messageInput.getText().length() > 0) {
            buildDraftMessage();
            provider.insertMessage(draftMessage);
        }

    }

    /**
     * Build draft Message to be inserted later in the database.
     */
    private void buildDraftMessage() {

        String draftBody = messageInput.getText().toString();
        Contact draftContact = friendFeed.getContact();
        MessageType draftType = MessageType.DRAFT;

        Platform draftPlatform = mapIndexToPlatform(currentPlatformIndex);
        Date draftDate = new Date();
        draftMessage = new HeadboxMessage(draftContact, draftBody, draftType,
                draftPlatform, draftDate);
    }

    /**
     * Map the index of current selected platform to its platform type.
     *
     * @param currentPlatformIndex index of current selected platform.
     * @return platform type of current selected platform.
     */
    private Platform mapIndexToPlatform(int currentPlatformIndex) {

        switch (currentPlatformIndex) {

            case PlatformCircleSettings.MESSAGE_INDEX:
                return Platform.SMS;
            case PlatformCircleSettings.CALL_INDEX:
                return Platform.CALL;
            case PlatformCircleSettings.FACEBOOK_INDEX:
                return Platform.FACEBOOK;
            case PlatformCircleSettings.HANGOUTS_INDEX:
                return Platform.HANGOUTS;
            case PlatformCircleSettings.SKYPE_INDEX:
                return Platform.SKYPE;
            case PlatformCircleSettings.TWITTER_INDEX:
                return Platform.TWITTER;
            case PlatformCircleSettings.WHATSAPP_INDEX:
                return Platform.WHATSAPP;
            case PlatformCircleSettings.MAIL_FILLER_INDEX:
                return Platform.EMAIL;
            default:
                return Platform.NOTHING;
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        if (friendFeed == null) {
            handleNullFeed();
            return root;
        }

        Log.d(TAG, "onCreateView");
        // In order to slide social window smoothly after restarting the
        // fragment.
        isPaused = false;

        context = activity.getApplicationContext();

        facebookAuthenticator = FacebookAuthenticator.getInstance(activity);
        googleAuthenticator = GooglePlusAuthenticator.getInstance(activity);

        preferencesManager = new PreferencesManager(context);

        root = inflater.inflate(R.layout.im_fragment_layout, container, false);

        showKeyboard = activity.getIntent().getExtras()
                .getBoolean(SHOW_KEYBOARD);


        setContactStatus();
        provider.markFeedAsRead(context, feedId);

        // Initialize Platforms Circle.
        platformCircleManager = new PlatformCircleManager(activity, this);

        init();


        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(Constants.ACTION_REFRESH_INSERT)) {

                    handleRefreshOnInsert(intent);

                } else if (intent.getAction().equals(
                        Constants.ACTION_REFRESH_UPDATE)) {

                    handleRefreshOnUpdate(intent);

                } else if (intent.getAction().equals(
                        Constants.ACTION_REFRESH_MESSAGE_TYPE_CHANGE)) {

                    handleRefreshOnMessageTypeChanged(intent);
                }
            }

        };


        return root;
    }


    private void handleNullFeed() {
        BlinqApplication.refresh = true;
        activity.finish();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {


        inflater.inflate(R.menu.conversation_menu, menu);
        if (memberContacts == null) {
            memberContacts = provider.getContacts(feedId);
        }
        this.menu = menu;
        if (menu == null)
            return;


        setupHeaderActions();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                // send Comlog Event
                analyticsSender.sendInstantMessageBackButtonPressedEvent();
                ((InstantMessageActivity) activity).setSendBackPressEvent(false);
                activity.onBackPressed();
                return true;

            case R.id.conversation_actbar_call_contact:
                openCallDialog();
                analyticsSender.setInstantMessageActionBarCallContactEvent();
                return true;

//            case R.id.conversation_actbar_refresh_contact:
//                refreshConversation();
//                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshConversation() {
        if (NetworkUtils.isConnectedToInternet(activity)) {

            updateRefreshLayout(true);
            AuthUtils.updateContacts(activity, memberContacts,
                    InstantMessageFragment.this);
        } else {
            UIUtils.showMessage(activity, activity.getResources()
                    .getString(R.string.connection_failed_message));
        }

        analyticsSender.setInstantMessageRefreshEvent();
    }


    private void updateRefreshLayout(final boolean isRefreshing) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // hide refresh button for device contacts.
//                if (isRefreshing) {
//                    menu.findItem(R.id.conversation_actbar_refresh_contact).setVisible(false);
//                } else {
//                    menu.findItem(R.id.conversation_actbar_refresh_contact).setVisible(true);
//                }
                activity.setProgressBarIndeterminateVisibility(isRefreshing);
            }
        });


    }


    private void handleRefreshOnInsert(Intent intent) {

        long messageID = Long.valueOf(intent
                .getStringExtra(Constants.MESSAGE_EXTRA));
        long feedID = Long.valueOf(intent.getStringExtra(Constants.FEED_EXTRA));

        if (feedID == feedId) {

            refreshUIOnNewMessage(provider.getMessage(messageID));
            provider.markFeedAsRead(context, feedId);
        }
    }

    private void handleRefreshOnMessageTypeChanged(Intent intent) {

        Log.d(TAG, "handleRefreshOnMessageTypeChanged()");

        long messageID = Long.valueOf(intent
                .getStringExtra(Constants.MESSAGE_EXTRA));
        long feedID = Long.valueOf(intent.getStringExtra(Constants.FEED_EXTRA));

        if (feedID == feedId) {
            updateMessageUI(messageID);
        }
    }

    private void handleRefreshOnUpdate(Intent intent) {

        long messageID = Long.valueOf(intent
                .getStringExtra(Constants.MESSAGE_EXTRA));
        long oldMessageID = Long.valueOf(intent
                .getStringExtra(Constants.OLD_MESSAGE_EXTRA));
        int feedID = Integer.valueOf(intent
                .getStringExtra(Constants.FEED_EXTRA));

        Log.d(TAG, "feed id = " + feedID);
        if (feedID == feedId) {

            refreshUIOnMessageUpdated(oldMessageID, messageID);
            provider.markFeedAsRead(context, feedId);

        }
    }

    /**
     * Initialize UI components of InstantMessageFragment view.
     */
    private void init() {

        setHasOptionsMenu(true);

        preferencesManager = new PreferencesManager(context);

        bottomLayout = root.findViewById(R.id.im_bottom_layout);

        messageList = (ListView) root.findViewById(R.id.message_list);
        messageList.setRecyclerListener(new RecycleMessageListHolder());
        accountFlipper = (ViewFlipper) root.findViewById(R.id.account_flipper);
        messageInput = (EditText) root.findViewById(R.id.message_input);
        messageInput.setEnabled(false);
        sendButton = (ImageButton) root.findViewById(R.id.send_button);

        analyticsSender = new AnalyticsSender(context);

        initMessageListView();
        initAccountFlipper();
        initMessageSend();

        setSwipeBackStatus(false);
    }


    /**
     * Initializes cover page and status view.
     */
    private void initCoverStatusPager() {

        List<SocialWindowPost> covers = getFriendCoverData();
        platformCovers = new ArrayList<List<SocialWindowPost>>();

        for (SocialWindowPost cover : covers) {

            final ArrayList<SocialWindowPost> coverList = new ArrayList<SocialWindowPost>();
            coverList.add(cover);
            platformCovers.add(coverList);
        }
    }

    /**
     * Display/Hide the cover page view *
     */
    private void configureSocialWindowsView() {

        if (!isPaused && !isKeyboardVisible() && isContactable && !coverPagerHidden) {

            ((InstantMessageActivity) getActivity()).setSocialWindowStatus(InstantMessageActivity.SocialWindowStatus.DEFAULT);
            setSwipeBackStatus(false);

        }

    }


    /**
     * Set a listener on global layout to watch changes in the activity layout. Used to
     * know when keyboard opened.
     */
    private void watchKeyboardStatus() {

        root.getViewTreeObserver().addOnGlobalLayoutListener(this);

    }

    /**
     * Enable/Disable swipe back effect.
     *
     * @param isEnabled swipe back status (Enable/Disable).
     */
    public void setSwipeBackStatus(boolean isEnabled) {

        if (activity instanceof SwipeBackActivity) {
            ((SwipeBackActivity) activity).setSwipeBackEnable(isEnabled);
        }

    }

    private class MessagesLoader extends AsyncTask<Void, Void, List<HeadboxMessage>> {

        Activity mainActivity;

        /**
         * @param mainActivity instant message main activity.
         */
        public MessagesLoader(Activity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        protected void onPreExecute() {

        }

        protected List<HeadboxMessage> doInBackground(Void... unused) {

            List<HeadboxMessage> messages = provider.getFeedMessages(feedId,
                    0, FIRST_LOAD_LIMIT);

            return (messages);
        }

        @Override
        protected void onPostExecute(List<HeadboxMessage> headboxMessages) {
            super.onPostExecute(headboxMessages);

            for (HeadboxMessage message : headboxMessages) {
                messageAdapter.addTop(message);
            }

            if (headboxMessages.size() > 0) {
                setLastIncomingMessageContact();
            }

            selectCurrentPlatformIndex();
            accountFlipper.setDisplayedChild(currentPlatformIndex);
            setSendButtonSelector(currentPlatformIndex);

            messageList.setSelectionFromTop(FIRST_LOAD_LIMIT, 0);

            messageList.setOnScrollListener(new OnScrollListener() {

                int firstVisibleItem;
                MoreMessagesLoader loadMore;

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                    if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                        if (hasMoreMessageToLoad && (firstVisibleItem == 0)
                                && (!view.canScrollVertically(-1))) {

                            loadMore = new MoreMessagesLoader(activity);
                            loadMore.execute();
                        }
                    }
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem,
                                     int visibleItemCount, int totalItemCount) {

                    this.firstVisibleItem = firstVisibleItem;
                }

            });

            messageList.setOnTouchListener(new OnTouchListener() {

                private float previousY;
                private float previousX;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            previousY = event.getY();
                            previousX = event.getX();
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float y = event.getY();
                            float x = event.getX();

                            float diff = y - previousY;
                            float diffX = x - previousX;

                            previousY = y;
                            previousX = x;

                            InstantMessageActivity.SocialWindowStatus socialWindowStatus = ((InstantMessageActivity) getActivity()).getSocialWindowStatus();

                            if (socialWindowStatus != InstantMessageActivity.SocialWindowStatus.PROCESSING
                                    && socialWindowStatus == InstantMessageActivity.SocialWindowStatus.DEFAULT
                                    && diff > 0.5 && Math.abs(diffX) < 5) {

                                onSocialWindowAffected.hideSocialWindow();

                            }
                            break;
                    }

                    return messageList.onTouchEvent(event);
                }
            });

        }
    }

    /**
     * Initialize Message ListView.
     */
    private void initMessageListView() {

        List<HeadboxMessage> messages = new ArrayList<HeadboxMessage>();
        messageAdapter = new MessageListAdapter(activity, messages, feedId);
        messageList.setAdapter(messageAdapter);

        MessagesLoader loader = new MessagesLoader(activity);
        loader.execute();
        messageList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        messageList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            private int numberOfSelections = 0;

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

                messageAdapter.clearSelection();
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                numberOfSelections = 0;
                // Inflate a menu resource providing context menu items
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.conversion_cab_menu, menu);

                analyticsSender.setInstantMessageLongClickOnListItem();
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.message_delete:

                        messageAdapter.deleteSelectedMessages();
                        analyticsSender.setDeleteMessageFromInstantMessageListEvent();

                        break;

                    case R.id.message_copy:
                        messageAdapter.copySelectedMessages();
                        analyticsSender.setCopyMessageFromInstantMessageListEvent();
                        break;
                    default:
                        return true;


                }
                numberOfSelections = 0;
                messageAdapter.clearSelection();
                mode.finish();
                return false;
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                  long id, boolean checked) {

                if (checked) {
                    numberOfSelections++;
                    messageAdapter.setNewSelection(position, checked);
                } else {
                    numberOfSelections--;
                    messageAdapter.removeSelection(position);
                }
                mode.setTitle(numberOfSelections + StringUtils.SPACE + getString(R.string.cab_conversion_title));

            }
        });

        messageList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int position, long arg3) {
                messageList.setItemChecked(position, !messageAdapter.isPositionChecked(position));
                return false;
            }
        });
    }

    /**
     * {@see setLastIncomingMessageContact(Platform platform, String contact) }
     */
    private void setLastIncomingMessageContact(HeadboxMessage message) {

        if (message == null) {
            return;
        }
        setLastIncomingMessageContact(message.getPlatform(), message
                .getContact().getIdentifier());
    }

    /**
     * {@see setLastIncomingMessageContact(Platform platform, String contact) }
     */
    private void setLastIncomingMessageContact() {

        HashMap<Platform, MemberContact> contacts = provider
                .getAllContacts(feedId);

        if (contacts != null && contacts.size() > 0) {

            for (Platform platform : contacts.keySet()) {

                String identifier = contacts.get(platform).getIdentifier();
                if (!StringUtils.isBlank(identifier))
                    setLastIncomingMessageContact(platform,
                            contacts.get(platform).getIdentifier());
            }
        }

    }

    /**
     * Save mobile/phone number/facebook id,etc.. for the last incoming message.
     */
    private void setLastIncomingMessageContact(Platform platform, String contact) {

        if (lastContactsToReply == null)
            lastContactsToReply = new HashMap<Platform, String>();
        if (platform != Platform.FACEBOOK)
            lastContactsToReply.put(platform, contact);
    }

    /**
     * Initialize the AccountFlipper view.
     */
    private void initAccountFlipper() {

        accountFlipper.setInAnimation(AnimationUtils.loadAnimation(activity,
                R.anim.bottom_in));
        accountFlipper.setOutAnimation(AnimationUtils.loadAnimation(activity,
                R.anim.top_out));
        accountFlipper.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                AppUtils.hideKeyboard(activity);

                analyticsSender.setOpenRetroDialerEvent();

                ((InstantMessageActivity) activity)
                        .setRedirectToHomeEnabled(false);
                platformCircleManager.setActivity(activity);
                platformCircleManager.showPlatformCircle(v, friendFeed
                                .getContact().getIdentifier(), memberContacts,
                        isContactable
                );
            }

        });
    }

    /**
     * Initialize message Input and the send Button
     * Initialize the keyboard
     */
    private void initMessageSend() {

        inputMethodManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(messageInput,
                InputMethodManager.SHOW_IMPLICIT);

        if (showKeyboard && !preferencesManager.isComlogFirstUse()) {
            activity.getIntent().putExtra(SHOW_KEYBOARD, false);
        }

        sendButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            }

        });

    }

    /**
     * Show an error dialog when trying to send sms to invalid contact.
     */
    private void showInvalidNumberDialog() {

        UIUtils.alertUser(context,
                context.getString(R.string.message_sender_invalid_contact));
    }

    /**
     * Select contact to send message for or to make call.
     */
    private String getContactIdentifier(Platform platform) {

        // Select suitable contact to send the message for.
        // By Handling the case when having multiple contacts associated with
        // certain
        // platform by selecting the last one the user talked with.
        String contact = null;
        if (lastContactsToReply != null
                && lastContactsToReply.containsKey(platform)) {
            contact = lastContactsToReply.get(platform);
        } else if (memberContacts.containsKey(platform)) {
            contact = memberContacts.get(platform).get(0).getIdentifier();
        }
        return contact;
    }

    private String getContactId(Platform platform) {

        String contact = null;
        if (memberContacts.containsKey(platform)) {
            contact = memberContacts.get(platform).get(0).getId();
        }
        return contact;
    }


    /**
     * Refreshes the UI of instant message fragment when new message added.
     */
    private void refreshUIOnNewMessage(HeadboxMessage message) {

        if (message == null
                || message.getPlatform() == null
                || message.getType() == null)
            return;

        friendFeed = provider.getFeed(feedId);
        messageAdapter.addEnd(message);
        messageAdapter.setDisplayMode(MessageListAdapter.ANIMATION_MODE);
        messageAdapter.notifyDataSetChanged();
        messageList.setSelection(messageAdapter.getCount() - 1);
        setLastIncomingMessageContact(message);
        onUiUpdate.refresh();
    }

    private void updateMessageUI(long messageId) {

        HeadboxMessage updatedMessage = provider.getMessage(messageId);

        if (updatedMessage == null
                || updatedMessage.getPlatform() == null
                || updatedMessage.getType() == null)
            return;

        messageAdapter.updateItemView(updatedMessage);
        messageAdapter.notifyDataSetChanged();
        // Temporary - above code not working...
        restartInstantMessage(Platform.NOTHING);
    }

    /**
     * Refreshes the UI of instant message fragment when message updated.
     */
    private void refreshUIOnMessageUpdated(long oldMessageId, long messageId) {

        HeadboxMessage message = provider.getMessage(messageId);

        if (message == null
                || message.getPlatform() == null
                || message.getType() == null)
            return;


        Log.e(TAG, "updated" + message.toString());
        messageAdapter.removeMessage(oldMessageId);
        messageAdapter.notifyDataSetChanged();
    }


    /**
     * Refreshes instant message view on resume if it was on background.
     */
    private void refreshOnResume() {

        long startIndex;
        List<HeadboxMessage> messages;

        if (isPaused) {
            friendFeed = provider.getFeed(feedId);
            memberContacts = provider.getContacts(feedId);
            if (messageAdapter.getCount() > 0) {
                HeadboxMessage lastMessage = (HeadboxMessage) messageAdapter
                        .getItem(messageAdapter.getCount() - 1);
                startIndex = Long.valueOf(lastMessage.getMessageId());
                messages = provider.getMessagesAfter(feedId, startIndex);
            } else {
                messages = provider.getFeedMessages(feedId, 0, 100);
            }

            setLastIncomingMessageContact();

            for (HeadboxMessage message : messages) {
                messageAdapter.addEnd(message);
            }
            provider.markFeedAsRead(context, feedId);
            messageAdapter.notifyDataSetChanged();
            messageList.setSelection(messageAdapter.getCount() - 1);
            onUiUpdate.refresh();
            setContactStatus();
            onUiUpdate.refreshOnResume();
        }

        initCoverStatusPager();
        configureSocialWindowsView();

    }

    /**
     * Show / hide refresh button
     */
    private void setupHeaderActions() {

        hasSocialWindow = false;
        for (Platform platform : sortedPlatformsList) {
            if (memberContacts.containsKey(platform)) {
                hasSocialWindow = true;
                break;
            }
        }
        // hide refresh button for device contacts.
//        if (!hasSocialWindow) {
//            menu.findItem(R.id.conversation_actbar_refresh_contact).setVisible(false);
//        } else {
//            menu.findItem(R.id.conversation_actbar_refresh_contact).setVisible(true);
//        }
        // Hide/show call contact button.
        if (memberContacts.containsKey(Platform.CALL)) {

            menu.findItem(R.id.conversation_actbar_call_contact).setVisible(true);

        } else {
            menu.findItem(R.id.conversation_actbar_call_contact).setVisible(false);
        }
    }

    /**
     * Check if the feed is contactable.
     */
    private void setContactStatus() {

        if (friendFeed.getContact() != null
                && !StringUtils.isBlank(friendFeed.getContact()
                .getContactId()))
            hasContact = true;

        if (!friendFeed.getContact().isContactable()) {
            isContactable = false;
            hasContact = false;
        }

    }


    /**
     * Async task that generate new messages and add them at the top of message
     * list.
     */
    private class MoreMessagesLoader extends AsyncTask<Void, Void, Void> {

        Activity mainActivity;

        /**
         * @param mainActivity instant message main activity.
         */
        public MoreMessagesLoader(Activity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        protected void onPreExecute() {

        }

        protected Void doInBackground(Void... unused) {

            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    int previousMessageCount = messageAdapter.getCount();
                    for (HeadboxMessage message : provider.getFeedMessages(
                            feedId, messageAdapter.getCount(), LOAD_MORE_LIMIT))
                        messageAdapter.addTop(message);

                    messageAdapter.notifyDataSetChanged();
                    messageList.setSelectionFromTop(messageAdapter.getCount()
                            - previousMessageCount, 0);
                    if ((messageAdapter.getCount() - previousMessageCount) != LOAD_MORE_LIMIT) {
                        hasMoreMessageToLoad = false;
                    }

                }

            });

            return (null);
        }

        protected void onPostExecute(Void unused) {

        }
    }

    private void startDotService() {
        Intent intent = new Intent(this.activity, FloatingDotService.class);
        intent.putExtra(FloatingDotService.FEED_ID_EXTRA_TAG, feedId);
        this.activity.startService(intent);
    }

    @Override
    public void execute(RadialMenuItem radialMenuItem) {

        index = Integer.parseInt(radialMenuItem.getName());

        if (radialMenuItem.isEnabled()) {
            startDotService();

            switch (index) {

                // Email.
                case PlatformCircleSettings.MAIL_FILLER_INDEX:

                    to = "Gmail";

                    openEmailsDialog();

                    platformCircleManager.dismiss();
                    accountFlipper
                            .setDisplayedChild(PlatformCircleSettings.MAIL_FILLER_INDEX);
                    currentPlatformIndex = PlatformCircleSettings.MAIL_FILLER_INDEX;

                    break;

                // Hangouts.
                case PlatformCircleSettings.HANGOUTS_INDEX:

                    to = "Hangouts";

                    setSendButtonSelector(index);
                    showKeyboardOnMessageInput();

                    platformCircleManager.dismiss();
                    accountFlipper
                            .setDisplayedChild(PlatformCircleSettings.HANGOUTS_INDEX);
                    currentPlatformIndex = PlatformCircleSettings.HANGOUTS_INDEX;

                    break;

                // Whatsapp.
                case PlatformCircleSettings.WHATSAPP_INDEX:

                    to = "Whatsapp";

                    openExternalAppConversation(Platform.WHATSAPP);

                    platformCircleManager.dismiss();
                    accountFlipper
                            .setDisplayedChild(PlatformCircleSettings.WHATSAPP_INDEX);
                    currentPlatformIndex = PlatformCircleSettings.WHATSAPP_INDEX;

                    break;

                // Message.
                case PlatformCircleSettings.MESSAGE_INDEX:

                    to = "Message";

                    setSendButtonSelector(index);
                    showKeyboardOnMessageInput();

                    platformCircleManager.dismiss();
                    accountFlipper
                            .setDisplayedChild(PlatformCircleSettings.MESSAGE_INDEX);
                    currentPlatformIndex = PlatformCircleSettings.MESSAGE_INDEX;

                    break;

                // Call.
                case PlatformCircleSettings.CALL_INDEX:

                    to = "Call";
                    openCallDialog();

                    platformCircleManager.dismiss();
                    setSendButtonSelector(index);

                    break;

                // Skype.
                case PlatformCircleSettings.SKYPE_INDEX:

                    to = "Skype";

                    openExternalAppConversation(Platform.SKYPE);

                    platformCircleManager.dismiss();
                    accountFlipper
                            .setDisplayedChild(PlatformCircleSettings.SKYPE_INDEX);
                    currentPlatformIndex = PlatformCircleSettings.SKYPE_INDEX;

                    break;

                // Facebook.
                case PlatformCircleSettings.FACEBOOK_INDEX:

                    to = "Facebook";

                    setSendButtonSelector(index);
                    showKeyboardOnMessageInput();

                    platformCircleManager.dismiss();
                    currentPlatformIndex = PlatformCircleSettings.FACEBOOK_INDEX;
                    accountFlipper
                            .setDisplayedChild(PlatformCircleSettings.FACEBOOK_INDEX);

                    break;

                // Instagram.
                case PlatformCircleSettings.INSTAGRAM_INDEX:

                    suggestAnApp();

                    break;
            }

            sendSwitchPlatformAnalytics(from, to);

            from = to;
        } else {

            if (hasContact && isContactable) {

                switch (index) {

                    case PlatformCircleSettings.MAIL_FILLER_INDEX:
                        platformCircleManager.dismiss();
                        displayMergeView(Platform.EMAIL, SearchHandler.SearchViewMode.MERGE);
                        break;

                    case PlatformCircleSettings.HANGOUTS_INDEX:
                        platformCircleManager.dismiss();
                        login(googleAuthenticator, Platform.HANGOUTS);
                        break;

                    case PlatformCircleSettings.FACEBOOK_INDEX:
                        platformCircleManager.dismiss();
                        login(facebookAuthenticator, Platform.FACEBOOK);
                        break;

                    case PlatformCircleSettings.SKYPE_INDEX:
                        platformCircleManager.dismiss();
                        displayMergeView(Platform.SKYPE, SearchHandler.SearchViewMode.MERGE);
                        break;

                    case PlatformCircleSettings.WHATSAPP_INDEX:
                        platformCircleManager.dismiss();
                        displayMergeView(Platform.WHATSAPP, SearchHandler.SearchViewMode.MERGE);
                        break;

                    case PlatformCircleSettings.INSTAGRAM_INDEX:
                        platformCircleManager.dismiss();
                        suggestAnApp();
                        break;

                    case PlatformCircleSettings.CALL_INDEX:
                    case PlatformCircleSettings.MESSAGE_INDEX:
                        platformCircleManager.dismiss();
                        displayMergeView(Platform.CALL, SearchHandler.SearchViewMode.MERGE);
                        break;

                }
            }

        }

    }

    /**
     * open a new email message with subject to let user suggest app.
     */
    private void suggestAnApp() {

        String email = context.getString(R.string.headbox_support_email);
        String subject = context
                .getString(R.string.retro_dialer_suggest_app_email_subject);
        EmailUtils.sendEmail(new String[]{email}, subject, "", context);
    }

    private void openCallDialog() {

        if (isContactable) {

            final List<MemberContact> callContacts = memberContacts
                    .get(Platform.CALL);

            // Show "select number " dialog if we don't have a previous.
            if ((lastContactsToReply == null || !lastContactsToReply
                    .containsKey(Platform.CALL)) && callContacts.size() > 1) {

                OnItemClickListener listener = new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        String number = String.valueOf(parent
                                .getItemAtPosition(position));
                        setLastIncomingMessageContact(Platform.CALL, number);
                        CallsManager.makeACall(activity, number);
                    }
                };

                DialogUtils.openChooseNumberDialog(activity, listener,
                        callContacts, platform);

            } else {
                CallsManager.makeACall(activity,
                        getContactIdentifier(Platform.CALL));
            }
        }
    }

    /**
     * Open dialog to select an email to send message for.
     */
    private void openEmailsDialog() {

        final List<MemberContact> emails = memberContacts.get(Platform.EMAIL);

        if (emails.size() > 1) {

            OnItemClickListener listener = new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    String email = String.valueOf(parent
                            .getItemAtPosition(position));
                    EmailUtils.sendEmail(new String[]{email}, "", "",
                            context);
                }
            };

            DialogUtils.openChooseNumberDialog(activity, listener, emails,
                    Platform.EMAIL);

        } else {

            String email = emails.get(0).getIdentifier();
            EmailUtils.sendEmail(new String[]{email}, "", "", context);

        }
    }

    @Override
    public void executeLongClick(RadialMenuItem radialMenuItem) {

        index = Integer.parseInt(radialMenuItem.getName());

        // we don't need to re-merge if we have only one platform.
        boolean condition = hasContact
                && (memberContacts.size() != 1
                || index == PlatformCircleSettings.MESSAGE_INDEX || index == PlatformCircleSettings.CALL_INDEX);

        if (condition) {

            if (radialMenuItem.isEnabled()) {

                SearchHandler.SearchViewMode mode = SearchHandler.SearchViewMode.REMERGE;
                switch (index) {

                    case PlatformCircleSettings.HANGOUTS_INDEX:
                        platform = Platform.HANGOUTS;
                        displayMergeView(platform, mode);
                        break;
                    case PlatformCircleSettings.FACEBOOK_INDEX:
                        platform = Platform.FACEBOOK;
                        displayMergeView(platform, mode);
                        break;
                    case PlatformCircleSettings.SKYPE_INDEX:
                        platform = Platform.SKYPE;
                        displayMergeView(platform, mode);
                        break;
                    case PlatformCircleSettings.WHATSAPP_INDEX:
                        platform = Platform.WHATSAPP;
                        displayMergeView(platform, mode);
                        break;
                    case PlatformCircleSettings.MESSAGE_INDEX:
                        platform = Platform.SMS;
                        break;
                    case PlatformCircleSettings.CALL_INDEX:
                        platform = Platform.CALL;
                        break;
                    case PlatformCircleSettings.MAIL_FILLER_INDEX:
                        platform = Platform.EMAIL;
                        displayMergeView(platform, mode);
                        break;
                }

                final List<MemberContact> contacts = memberContacts
                        .get(platform);

                OnItemClickListener listener = new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {

                        // Get selected number.
                        String number = String.valueOf(parent
                                .getItemAtPosition(position));

                        if (platform == Platform.CALL) {

                            CallsManager.makeACall(activity, number);
                            setSendButtonSelector(index);

                        } else if (platform == Platform.SMS) {

                            setSendButtonSelector(index);
                            currentPlatformIndex = PlatformCircleSettings.MESSAGE_INDEX;
                            showKeyboardOnMessageInput();
                            accountFlipper
                                    .setDisplayedChild(PlatformCircleSettings.MESSAGE_INDEX);
                            setLastIncomingMessageContact(Platform.SMS, number);
                        }
                    }
                };

                if (platform == Platform.SMS || platform == Platform.CALL)
                    DialogUtils.openChooseNumberDialog(activity, listener,
                            contacts, platform);

                platformCircleManager.dismiss();
            }
        }
    }

    private void sendSwitchPlatformAnalytics(String fromPlatform,
                                             String toPlatform) {

        if (!from.equalsIgnoreCase("Call")) // ignore switching from call event
        {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(AnalyticsConstants.TYPE_PROPERTY, toPlatform);

            analyticsSender.sendPlatformSwitchEvent(properties);
        }
    }

    /**
     * Sets the send Button selector according to the current selected platform.
     *
     * @param index the index of current platform.
     */
    private void setSendButtonSelector(int index) {

        TypedArray iconsIds;

        switch (index) {

            case PlatformCircleSettings.TWITTER_INDEX:
            case PlatformCircleSettings.INSTAGRAM_INDEX:
                sendButton.setClickable(false);
                messageInput.setFocusable(true);
                messageInput.setEnabled(false);
                sendButton.setImageResource(R.drawable.conversation_icon_sendmsg);
                messageInput.setOnClickListener(null);
                messageInput.setHint(StringUtils.EMPTY_STRING);
                break;
            case PlatformCircleSettings.CALL_INDEX:
                messageInput.setOnClickListener(null);
                messageInput.setFocusable(true);
                break;
            default:
                messageInput.setFocusable(true);
                messageInput.setOnClickListener(null);
                sendButton.setClickable(true);
                messageInput.setFocusableInTouchMode(true);
                iconsIds = activity.getResources().obtainTypedArray(
                        R.array.platform_send_button_selectors);

                if (iconsIds.getResourceId(index, NOT_FOUND_ERROR) != NOT_FOUND_ERROR) {

                    //  sendButton.setImageResource(iconsIds.getResourceId(index,
                    ///       NOT_FOUND_ERROR));

                    sendButton.setImageResource(R.drawable.conversation_icon_sendmsg);
                }

                if (index == PlatformCircleSettings.WHATSAPP_INDEX
                        || index == PlatformCircleSettings.MAIL_FILLER_INDEX
                        || index == PlatformCircleSettings.SKYPE_INDEX) {
                    final int platformIndex = index;
                    messageInput.setFocusable(false);
                    messageInput.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            analyticsSender.sendMessageInputClickedEvent();
                            openExternalAppConversation(mapIndexToPlatform(platformIndex));
                        }
                    });
                }

                break;
        }

        if (!isContactable) {

            messageInput.setEnabled(false);
            sendButton.setClickable(false);
        }

    }

    /**
     * Shows keyboard and scrolls list view to bottom on chat platform
     * selection.
     */
    private void showKeyboardOnMessageInput() {

        if (currentPlatformIndex != PlatformCircleSettings.SKYPE_INDEX
                && currentPlatformIndex != PlatformCircleSettings.WHATSAPP_INDEX) {
            UIUtils.showKeyboardOnTextView(activity, messageInput);
        }
    }

    private void displayMergeView(Platform platform, SearchHandler.SearchViewMode searchViewMode) {

        onUiUpdate.displayMergeView(feedId, platform, searchViewMode);
    }


    /**
     * Login to specific Platform.
     *
     * @param authenticator - authentication instance.
     * @param platform      - Authentication platform.
     */
    private void login(Authenticator authenticator, Platform platform) {

        if (authenticator.isConnected() && !authenticator.isLoginCompleted()) {
            AuthUtils.LoadContacts(platform, this, activity, true,
                    AnalyticsConstants.LOGIN_FROM_RETRO);
            return;
        }
        if (!authenticator.isConnected()) {
            AuthUtils.LoginCallBack loginCallCallback = new AuthUtils().new LoginCallBack(
                    platform, activity, this, true,
                    AnalyticsConstants.LOGIN_FROM_RETRO);
            authenticator.login(loginCallCallback);
        } else {
            displayMergeView(platform, SearchHandler.SearchViewMode.MERGE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case AppUtils.CONTACT_SAVE_INTENT_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "CONTACT_SAVE_INTENT_REQUEST onActivityResult");
                }
                break;
            default:
                facebookAuthenticator.onActivityResult(activity, requestCode,
                        resultCode, data);
                googleAuthenticator.onActivityResult(activity, requestCode,
                        resultCode, data);
                break;
        }

    }

    /**
     * Get data from friend feed and returns it in a list of CoverPageStatus
     * model with a model for each platform.
     *
     * @return list of CoverPageStatus models.
     */
    private ArrayList<SocialWindowPost> getFriendCoverData() {

        List<SocialWindowPost> coverPageData = new ArrayList<SocialWindowPost>();

        hasSocialWindow = false;

        if (hasContact) {

            for (Platform platform : sortedPlatformsList) {

                SocialWindowPost coverPageStatus = new SocialWindowPost();
                coverPageStatus.setCoverPageStatusPlatform(platform);
                coverPageStatus.setStatusBody(activity
                        .getString(R.string.account_status_text));
                if (memberContacts.containsKey(platform)) {

                    for (MemberContact memberContact : memberContacts
                            .get(platform)) {
                        hasSocialWindow = true;
                        coverPageStatus = new SocialWindowPost();
                        coverPageStatus.setCoverPageStatusPlatform(platform);
                        coverPageStatus.setStatusBody(activity
                                .getString(R.string.account_status_text));
                        setCoverPageSource(memberContact, coverPageStatus);
                        coverPageData.add(coverPageStatus);

                    }
                } else {
                    coverPageStatus.setHasLink(true);
                    coverPageStatus
                            .setLink(PlatformCoverListAdapter.INTIAL_POST);
                    coverPageData.add(coverPageStatus);
                }
            }

        } else {
            if (isContactable) {
                SocialWindowPost coverPageStatus = new SocialWindowPost(
                        PlatformCoverListAdapter.INTIAL_POST, Platform.NOTHING,
                        activity.getString(R.string.account_status_text));
                coverPageData.add(coverPageStatus);
            }

        }
        return (ArrayList<SocialWindowPost>) coverPageData;
    }

    /**
     * Sets the drawable source of cover page for given platform.
     *
     * @param memberContact   the MemberContact object for given platform.
     * @param coverPageStatus the CoverPageStatus object for given platform.
     */
    private void setCoverPageSource(MemberContact memberContact,
                                    SocialWindowPost coverPageStatus) {

        if (memberContact.hasCover()) {

            coverPageStatus.setPictureUrl(memberContact.getCoverUrl());
            coverPageStatus.setHasPicture(true);

        }
    }

    /**
     * Handles typing state on the UI
     *
     * @author Johan Hansson
     */
    public class TypingStatusHandler extends
            AsyncTask<Boolean, Boolean, Boolean> {

        Timer typingWatchdogTimer = new Timer();

        @Override
        protected Boolean doInBackground(Boolean... params) {
            return params[0];
        }

        @Override
        protected void onPostExecute(Boolean isTyping) {
            super.onPostExecute(isTyping);

            if (isTyping) {

                onTypingMessageChanged.showTypingMessage();

                resetTypingWatchdogTimer(typingWatchdogTimer);

            } else {

                onTypingMessageChanged.hideTypingMessage();

                typingWatchdogTimer.cancel();
            }

        }
    }


    /**
     * @param typingWatchdogTimer typing watchdog timer to reset.
     */
    private void resetTypingWatchdogTimer(Timer typingWatchdogTimer) {

        typingWatchdogTimer.cancel();

        typingWatchdogTimer = new Timer();
        typingWatchdogTimer.schedule(new TimerTask() {
            public void run() {

                activity.runOnUiThread(new Thread(new Runnable() {

                    @Override
                    public void run() {

                        onTypingMessageChanged.hideTypingMessage();
                    }
                }));

            }
        }, TYPING_WATCHDOG_TIMER);

    }

    public void UpdateUserProfile(final Authenticator auth,
                                  final ProfileRequestCallback callback) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                auth.getProfile(callback);
            }
        });
    }

    /**
     * After completing login process to certain platform.
     */
    @Override
    public void onLoginCompleted(final Platform platform) {
        if (!AppUtils.isActivityActive(activity))
            return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                memberContacts = provider.getContacts(feedId);

                if (memberContacts.containsKey(platform)) {

                    restartInstantMessage(platform);
                } else {
                    displayMergeView(platform, SearchHandler.SearchViewMode.MERGE);
                }

            }

        });

    }

    /**
     * Restart the Instant Message.
     */
    public void restartInstantMessage(Platform mergePlatform) {

        try {
            BlinqApplication.refresh = true;
            InstantMessageFragment instantMessageFragment = new InstantMessageFragment(
                    activity);

            instantMessageFragment.setMergePlatform(mergePlatform);
            instantMessageFragment.setCoverPagerHidden(this.coverPagerHidden);
            instantMessageFragment
                    .setCurrentPlatformIndex(currentPlatformIndex);
            instantMessageFragment.setPlatformIndexSet(platformIndexSet);
            FragmentTransaction transaction = activity
                    .getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.linearLayoutForComlogContents,
                    instantMessageFragment,
                    InstantMessageActivity.INSTANT_MESSAGE_FRAGMENT_TAG);
            transaction.commit();
        } catch (IllegalStateException illegalStateException) {
            Log.e(TAG, "IllegalStateException while restarting comlog:"
                    + illegalStateException);

            ((InstantMessageActivity) activity).restartInstantMessageFragment();

        } catch (Exception exception) {
            Log.e(TAG, "Exception while restarting comlog: " + exception);
            ((InstantMessageActivity) activity).restartInstantMessageFragment();
        }
    }

    @Override
    public void onUserProfileLoaded(Platform platform,
                                    Contact profile, boolean success) {
    }

    @Override
    public void onContactsUpdated(List<Contact> contacts) {

        try {
            Log.d(TAG, "Complete updating profile");
            updateRefreshLayout(false);

            InstantMessageActivity.SocialWindowStatus socialWindowStatus = ((InstantMessageActivity) getActivity()).getSocialWindowStatus();
            coverPagerHidden = socialWindowStatus != InstantMessageActivity.SocialWindowStatus.DEFAULT;
            setPlatformIndexSet(true);
            isRefreshed = true;

            // Update in UI thread because it's run in background
            // thread.
            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    initMessageListView();
                    onResume();

                }
            });
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException while updating profile.");
        }

    }

    @Override
    public void onInboxUpdated(RequestStatus status) {
        // TODO show toast.
    }

    /**
     * Fill social window with correct user data after applying animation.
     */
    public void fillUserData() {

        if (platformCovers.size() <= 0) {
            setHasCoverPage(false);
            return;
        }

        setHasCoverPage(true);
    }

    /**
     * Social Window AuthAction Handler.
     */
    private AuthAction socialWindowAuthAction = new AuthAction() {

        @Override
        public void onContactsUpdated(List<Contact> contacts) {
        }

        @Override
        public void onLoginCompleted(Platform platform) {

            Log.d(TAG, "complete login from social window for " + platform);
            restartInstantMessageAfterLogin(platform);
        }

        @Override
        public void onUserProfileLoaded(Platform platform,
                                        Contact profile, boolean success) {
        }

        @Override
        public void onInboxUpdated(RequestStatus status) {
        }
    };

    /**
     * Restart instant message after login from social window
     */
    protected void restartInstantMessageAfterLogin(Platform platform) {

        try {
            BlinqApplication.refresh = true;
            InstantMessageFragment instantMessageFragment = new InstantMessageFragment(
                    activity);
            instantMessageFragment.setMergePlatform(Platform.NOTHING);
            FragmentTransaction transaction = activity
                    .getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.linearLayoutForComlogContents,
                    instantMessageFragment,
                    InstantMessageActivity.INSTANT_MESSAGE_FRAGMENT_TAG);
            transaction.commit();
        } catch (IllegalStateException illegalStateException) {
            Log.e(TAG,
                    "IllegalStateException while restarting comlog after login:"
                            + illegalStateException
            );
            onResume();
        } catch (Exception exception) {
            Log.e(TAG, "Exception while restarting conversation after login:"
                    + exception);
            onResume();
        }

    }

    /**
     * Undo listener to the merge operation
     */
    UndoListener undoMergeListener = new UndoListener() {

        @Override
        public void onUndo(Parcelable token) {
            provider.undoLastMerge();
            preferencesManager.setProperty(
                    MergeFragment.DISPLAY_LAST_SEARCH_TEXT, true);


            SearchHandler.SearchViewMode mode = SearchHandler.SearchViewMode
                    .fromId(preferencesManager.
                            getProperty(Constants.MERGE_TYPE, SearchHandler.SearchViewMode.MERGE.getId()));

            displayMergeView(BlinqApplication.searchSource, mode);
            preferencesManager.setProperty(PreferencesManager.IS_SHOW_UNDO_MERGE,
                    false);

        }
    };


    /**
     * Change message input status (Enable/Disable) depends on given status.
     *
     * @param status true to enable message input, false to disable message input.
     */
    public void changeMessageInputStatus(boolean status) {
        messageInput.setEnabled(status);
    }


    /**
     * Work as call back to be sent to the container activity when action (show/hide) should apply on
     * social window.
     */
    public interface OnSocialWindowAffected {

        /**
         * Called when hide social window action should applied.
         */
        public void hideSocialWindow();

        /**
         * Called when show social window action should applied.
         */
        public void showSocialWindow();

        /**
         * Called when animation start to hide the unified social window fast without notice.
         */
        public void hideWithoutAnimation();

        /**
         * Called when activity started to show the unified social window fast without notice
         * after hidden by animation.
         */
        public void showWithoutAnimation();
    }


    /**
     * Interface to communicate with container activity to show/hide typing message.
     */
    public interface OnTypingMessageChanged {

        /**
         * Called to show the typing message.
         */
        public void showTypingMessage();

        /**
         * Called to hide the typing message.
         */
        public void hideTypingMessage();
    }


    /**
     * Interface to communicate with container activity when UI update.
     */
    public interface OnUiUpdate {

        /**
         * Called to refresh some components in the container activity.
         */
        public void refresh();


        /**
         * Called to refresh some components in the container activity in on resume.
         */
        public void refreshOnResume();


        /**
         * Called to display merge fragment in place of messages list fragment.
         */
        public void displayMergeView(int feedId, Platform platform, SearchHandler.SearchViewMode mode);

        /**
         * Update the action bar based on the attached fragment.
         */
        public void updateActionBar(Fragment fragment);
    }

}