package com.blinq.authentication.impl.Twitter;

import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.blinq.utils.Log;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.blinq.R;
import com.blinq.authentication.Authenticator.LoginCallBack;
import com.blinq.authentication.settings.TwitterSettings;
import com.blinq.utils.DialogUtils;
import com.blinq.utils.DialogUtils.DialogType;
import com.blinq.utils.StringUtils;
import com.blinq.utils.UIUtils;

/**
 * Activity used to open Twitter login page.
 *
 * @author Johan Hansson
 */
public class TwitterLoginActivity extends Activity {

    private String TAG = TwitterLoginActivity.class.getSimpleName();
    private WebView twitterLoginWebView = null;
    private TwitterLoginWebViewClient twitterLoginWebViewClient = null;
    private LoginCallBack twitterLoginCallBack;
    private Twitter twitter;
    private RequestToken twitterRequestToken;
    private String twitterOauthVerifier;
    private TwitterAuthenticator twitterAuthenticator;
    private AlertDialog customWaitingDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_login);
        twitterLoginWebView = (WebView) findViewById(R.id.webViewForTwitterLogin);
        UIUtils.hideActionbar(this);
        twitterAuthenticator = TwitterAuthenticator.getInstance(this);
        twitterLoginCallBack = twitterAuthenticator.getTwitterLoginCallBack();
        new TwitterLoginInitializer().execute();

    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setUpTwitterLoginWebView(Uri twitterAuthenticationUri) {

        twitterLoginWebView.setVerticalScrollBarEnabled(false);
        twitterLoginWebView.setHorizontalScrollBarEnabled(false);
        twitterLoginWebViewClient = new TwitterLoginWebViewClient();
        twitterLoginWebView.setWebViewClient(twitterLoginWebViewClient);
        WebSettings webSettings = twitterLoginWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        twitterLoginWebView.loadUrl(twitterAuthenticationUri.toString());
    }


    /**
     * Web View client for the Twitter's login web view.
     */
    private class TwitterLoginWebViewClient extends WebViewClient {

        boolean closed = false;

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (!closed && url.startsWith(TwitterSettings.TWITTER_CALLBACK_URL)) {

                if (!url.contains(TwitterSettings.TWITTER_DENIED)) {

                    // Get the Twitter's login authentication information.
                    Uri uri = Uri.parse(url);
                    twitterOauthVerifier = uri
                            .getQueryParameter(TwitterSettings.TWITTER_URL_OAUTH_VERIFIER);
                    new TwitterTokensSaver().execute();
                } else {
                    if (twitterLoginCallBack != null) {
                        twitterLoginCallBack.onCancel();
                    }
                    finish();
                }
            }
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            closed = true;
            DialogUtils.hideDialog(TwitterLoginActivity.this, customWaitingDialog);
            if (twitterLoginCallBack != null) {
                twitterLoginCallBack.onException(getString(R.string.connection_failed), new Exception(
                        getString(R.string.connection_failed)));
            }
            finish();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            DialogUtils.hideDialog(TwitterLoginActivity.this, customWaitingDialog);
        }
    }

    /**
     * Asynchronous task to initialize the login process to Twitter in
     * background
     */
    private class TwitterLoginInitializer extends
            AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            customWaitingDialog = DialogUtils.createCustomDialog(TwitterLoginActivity.this, DialogType.CUSTOM);
            DialogUtils.showDialog(TwitterLoginActivity.this, customWaitingDialog);
        }

        @Override
        protected Boolean doInBackground(String... args) {
            try {

                if (twitterLoginCallBack != null) {
                    twitterLoginCallBack.doWhileLogin();
                }

                twitter = twitterAuthenticator.createTwitter();
                twitterRequestToken = twitter.getOAuthRequestToken();

            } catch (Exception ex) {

                Log.e(TAG, "Login error to Twitter :" + ex);

                if (twitterRequestToken != null
                        && !StringUtils.isBlank(twitterRequestToken
                        .getAuthenticationURL())) {
                    return true;
                }
                if (twitterLoginCallBack != null) {
                    twitterLoginCallBack
                            .onException(
                                    getString(R.string.connection_failed),
                                    new Exception(
                                            getString(R.string.connection_failed))
                            );
                }
                return false;
            }
            return true;

        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (twitterRequestToken != null && result) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Uri twitterAuthenticationUri = Uri
                                .parse(twitterRequestToken
                                        .getAuthenticationURL());
                        setUpTwitterLoginWebView(twitterAuthenticationUri);
                    }
                });

            } else {
                finish();
            }

        }
    }

    /**
     * Asynchronous task to save Twitter's authentication information in
     * background.
     */
    private class TwitterTokensSaver extends AsyncTask<String, String, Boolean> {

        private Exception twitterException = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... args) {

            AccessToken twitterAccessToken = null;
            try {

                Log.i(TAG, "TwitterTokensSaver : twitterRequestToken ="
                        + twitterRequestToken);
                Log.i(TAG, "TwitterTokensSaver : twitterOauthVerifier ="
                        + twitterOauthVerifier);
                if (twitterRequestToken != null && twitterOauthVerifier != null) {
                    twitterAccessToken = twitter.getOAuthAccessToken(
                            twitterRequestToken, twitterOauthVerifier);

                    if (twitterAccessToken != null) {
                        Log.i(TAG, "TwitterTokensSaver : twitterAccessToken ="
                                + twitterAccessToken);
                        long userID = twitterAccessToken.getUserId();
                        User user = twitter.showUser(userID);
                        twitterAuthenticator.saveLoggedInTwitterAccount(
                                user.getName(), twitterAccessToken.getToken(),
                                twitterAccessToken.getTokenSecret());
                        return true;
                    }

                }

            } catch (Exception ex) {
                twitterException = ex;
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (twitterLoginCallBack != null) {
                if (result) {
                    twitterLoginCallBack.onLoggedIn();
                } else if (twitterException != null) {
                    twitterLoginCallBack.onException(
                            twitterException.getMessage(), twitterException);
                }
            }

            finish();

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (twitterLoginCallBack != null) {
            twitterLoginCallBack.onCancel();
        }
    }

}