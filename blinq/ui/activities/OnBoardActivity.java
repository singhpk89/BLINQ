package com.blinq.ui.activities;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.blinq.PreferencesManager;
import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.animation.SetInvisibleWhenFinishedAnimationListener;
import com.blinq.authentication.Authenticator;
import com.blinq.authentication.impl.Action;
import com.blinq.authentication.impl.AuthUtils;
import com.blinq.authentication.impl.OnActionListener;
import com.blinq.authentication.impl.facebook.Actions.FriendsAction;
import com.blinq.authentication.impl.facebook.Actions.InboxAction;
import com.blinq.authentication.impl.facebook.FacebookAuthenticator;
import com.blinq.models.Contact;
import com.blinq.models.HeadboxMessage;
import com.blinq.models.Platform;
import com.blinq.module.message.utils.MessageConverter;
import com.blinq.provider.FeedProvider;
import com.blinq.provider.FeedProviderImpl;
import com.blinq.service.ConnectivityService;
import com.blinq.service.platform.PlatformServiceBase;
import com.blinq.ui.views.CustomTypefaceTextView;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.Log;
import com.blinq.utils.ServerUtils;
import com.blinq.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dreamers.graphics.RippleDrawable;

import static com.blinq.authentication.impl.AuthUtils.AuthAction;

public class OnBoardActivity extends HeadboxBaseActivity implements AuthAction {

    private static final String TAG = OnBoardActivity.class.getSimpleName();

    boolean isServiceBounded = false;

    private FacebookAuthenticator facebookAuthenticator;
    private ConnectivityService connectivityService;
    // Defines callbacks for service binding, passed to bindService().
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            // We've bound to ConnectorService, cast the binder and get
            // ConnectivityService instance
            ConnectivityService.ServiceBinder binder = (ConnectivityService.ServiceBinder) service;
            connectivityService = binder.getService();
            isServiceBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBounded = false;
        }
    };

    private CustomTypefaceTextView pageTitleLayout;
    private CustomTypefaceTextView pageDescriptionLayout;
    private ImageView fbLoginView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        fillScreen();
        setContentView(R.layout.activity_onboard);

        facebookAuthenticator = FacebookAuthenticator.getInstance(this);

        initializeComponents();
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
        super.onStop();
        // Unbind from the ConnectivityService
        if (isServiceBounded) {

            unbindService(serviceConnection);
            isServiceBounded = false;
        }
    }

    @Override
    public void onBackPressed() {
        sendEvent(AnalyticsConstants.BACK_FACEBOOK_EVENT, true, AnalyticsConstants.ONBOARDING_CATEGORY);
        super.onBackPressed();

    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeView();
    }

    @Override
    protected void onDestroy() {
        //ActivityUtils.unbindDrawables(findViewById(R.id.onboard_main_layout));
        super.onDestroy();
    }

    /**
     * Initialize login view depending on connected user accounts.
     */
    private void initializeView() {

        // check if the user is connected with facebook.
        facebookAuthenticator = FacebookAuthenticator.getInstance(this);
        if (facebookAuthenticator.isConnected()) {
            AuthUtils.UpdateUserProfile(this, facebookAuthenticator, this,
                    Platform.FACEBOOK);
        }

    }

    /**
     * Open facebook authentication screen/dialog.
     */
    private void loginToFacebook() {

        facebookAuthenticator = FacebookAuthenticator
                .getInstance(OnBoardActivity.this);
        login(facebookAuthenticator, false, Platform.FACEBOOK);
    }

    /**
     * Login to specific Platform.
     *
     * @param authenticator - authentication instance.
     * @param platform      - Authentication platform.
     */
    private void login(Authenticator authenticator, boolean showDialog, Platform platform) {

        if (!authenticator.isConnected()) {
            Authenticator.LoginCallBack loginCallCallback = new AuthUtils().new LoginCallBack(
                    platform, OnBoardActivity.this, OnBoardActivity.this, showDialog,
                    AnalyticsConstants.LOGIN_FROM_LOGIN_SCREEN);
            authenticator.login(loginCallCallback);
        }

    }

    /**
     * Initialize all components of the login view.
     */
    private void initializeComponents() {
        fbLoginView = (ImageView) OnBoardActivity.this.findViewById(R.id.connect_to_facebook);
        RippleDrawable.createRipple(fbLoginView,getResources().getColor(R.color.onboard_login_screen_facebook_button_ripple));
        fbLoginView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginToFacebook();
            }
        });

        final View mainLayout = findViewById(R.id.onboard_main_layout);
        mainLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendEvent(AnalyticsConstants.CLICKED_RANDOMLY_SIGNIN_SCREEN_EVENT, false, AnalyticsConstants.ONBOARDING_CATEGORY);
            }
        });

        preferencesManager.setInstagramContactsSyncStatus(Constants.SYNC_UNKNOWN);
        preferencesManager.setFacebookContactsSyncSatus(Constants.SYNC_UNKNOWN);
        preferencesManager.setTwitterContactsSyncStatus(Constants.SYNC_UNKNOWN);
        initializeAnimation();
    }

    private void initializeAnimation() {
        final Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_logo);
        final Animation logoTextAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_logo_text);
        final Animation emailIconAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_icon1);
        final Animation skypeIconAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_icon1);
        final Animation hangoutsIconAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_icon2);
        final Animation messengerIconAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_icon2);
        final Animation whatsappIconAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_icon3);
        final Animation textAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_text);
        final Animation connectToFacebookAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_connect_to_facebook);

        final View logoView = findViewById(R.id.splash_logo);
        final View logoTextView = findViewById(R.id.splash_logo_text);
        final View emailIconView = findViewById(R.id.email_icon);
        final View skypeIconView = findViewById(R.id.skype_icon);
        final View hangoutsIconView = findViewById(R.id.hangouts_icon);
        final View messengerIconView = findViewById(R.id.messenger_icon);
        final View whatsappIconView = findViewById(R.id.whatsapp_icon);
        final View textView = findViewById(R.id.text);
        final View connectToFacebookView = findViewById(R.id.connect_to_facebook);

        emailIconAnimation.setAnimationListener(new SetInvisibleWhenFinishedAnimationListener(emailIconView));
        skypeIconAnimation.setAnimationListener(new SetInvisibleWhenFinishedAnimationListener(skypeIconView));
        hangoutsIconAnimation.setAnimationListener(new SetInvisibleWhenFinishedAnimationListener(hangoutsIconView));
        messengerIconAnimation.setAnimationListener(new SetInvisibleWhenFinishedAnimationListener(messengerIconView));
        whatsappIconAnimation.setAnimationListener(new SetInvisibleWhenFinishedAnimationListener(whatsappIconView));

        logoView.startAnimation(logoAnimation);
        logoTextView.startAnimation(logoTextAnimation);
        emailIconView.startAnimation(emailIconAnimation);
        skypeIconView.startAnimation(skypeIconAnimation);
        hangoutsIconView.startAnimation(hangoutsIconAnimation);
        messengerIconView.startAnimation(messengerIconAnimation);
        whatsappIconView.startAnimation(whatsappIconAnimation);
        textView.startAnimation(textAnimation);
        connectToFacebookView.startAnimation(connectToFacebookAnimation);

        // Schedule the background animation.
        // Unfortunately, it can't be defined in the xml file.
        final View layout = findViewById(R.id.onboard_main_layout);
        final TransitionDrawable background = (TransitionDrawable) layout.getBackground();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                background.startTransition(200 /* background animation duration */);
            }
        }, 2400 /* background animation delay */);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        facebookAuthenticator.onActivityResult(this, requestCode, resultCode,
                data);

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onLoginCompleted(Platform platform) {

    }

    @Override
    public void onUserProfileLoaded(Platform platform, Contact profile, boolean success) {
        switch (platform) {
            case FACEBOOK:

                if (success) {
                    getPreferencesManager().setFacebookProfileLoadingStatus(true);
                    facebookAuthenticator.setLoginCompleted(true);
                    getPreferencesManager().setProperty(PreferencesManager.APP_AUTHENTICATED, true);
                    //enableHeadboxComponents();
                    startFacebookInitialLoad();
                    forwardToNextView();
                }

                break;
            default:
                break;

        }
    }

    /**
     * Enable headbox services and receivers after establishing a successful connection with Facebook.
     */
    private void enableHeadboxComponents() {

        AppUtils.setAppComponentsEnabledSetting(this,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        AppUtils.startMainService(this);

    }

    private void startFacebookInitialLoad() {

        String feedHistoryMode = getPreferencesManager().getProperty(PreferencesManager.AB_FEED_HISTORY,
                AnalyticsConstants.AB_FEED_HISTORY_FULL);

        if (feedHistoryMode.equalsIgnoreCase(AnalyticsConstants.AB_FEED_HISTORY_FULL)) {

            //loadFacebookInbox();
            loadFacebookFriends();


        } else {

            PlatformServiceBase.startContactsFullSync(Platform.FACEBOOK, OnBoardActivity.this);
        }

    }

    /**
     * To Forward the user to the next view according to the connectivity status.
     */
    private void forwardToNextView() {
        finish();
        Intent intent = new Intent(this, WalkthroughActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    /**
     * To Start an initial loading for facebook inbox & contacts.
     */
    private void loadFacebookInbox() {

        InboxAction action = new InboxAction(FacebookAuthenticator.getInstance(this));
        action.setLimit(PreferencesManager.FACEBOOK_INBOX_FIRST_LOAD_LIMIT);
        action.setActionListener(new OnActionListener<Map<Contact, List<HeadboxMessage>>>() {
            @Override
            public void onComplete(Map<Contact, List<HeadboxMessage>> response) {

                List<Contact> contacts = new ArrayList<Contact>(response.keySet());

                FeedProviderImpl.getInstance(OnBoardActivity.this).buildContacts(contacts, Platform.FACEBOOK);

                HashMap<String, ContentValues[]> values = new HashMap<String, ContentValues[]>();

                for (Contact contact : response.keySet()) {
                    List<HeadboxMessage> messages = response.get(contact);
                    ContentValues[] content = MessageConverter.convertToContentValues(messages);
                    String id = contact.getContactId().concat(StringUtils.FACEBOOK_SUFFIX);
                    values.put(id, content);
                }

                FeedProvider.setMessages(values);
                OnBoardActivity.this.getContentResolver().bulkInsert(
                        FeedProvider.FACEBOOK_URI, null);
                PlatformServiceBase.startContactsFullSync(Platform.FACEBOOK, OnBoardActivity.this);
            }

            @Override
            public void onFail(String reason) {
                Log.d(TAG, "onFail , failed to load facebook inbox = " + reason);
                super.onFail(reason);
            }

            @Override
            public void onException(Throwable throwable) {
                Log.d(TAG, "onException = " + throwable.getMessage());
                super.onException(throwable);
            }
        });

        action.execute();
    }

    private void loadFacebookFriends() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                preferencesManager.setFacebookContactsSyncSatus(Constants.SYNC_STARTED);
                FriendsAction action = new FriendsAction(FacebookAuthenticator.getInstance(getApplicationContext()));
                action.setRequestType(Action.RequestType.SYNC);
                action.execute();
                if (action.getResult() == null)
                    return;
                FeedProviderImpl.getInstance(OnBoardActivity.this).buildContacts(action.getResult(), Platform.FACEBOOK);
                preferencesManager.setFacebookContactsSyncSatus(Constants.SYNC_ENDED);
                Context context = getApplicationContext();
                ServerUtils.getInstance(context).sendContactsDatabase();
            }
        }).start();
    }

    @Override
    public void onContactsUpdated(List<Contact> contacts) {

    }

    @Override
    public void onInboxUpdated(AuthUtils.RequestStatus status) {

    }



}
