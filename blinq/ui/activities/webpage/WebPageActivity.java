package com.blinq.ui.activities.webpage;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.blinq.R;
import com.blinq.ui.activities.HeadboxBaseActivity;
import com.blinq.utils.AppUtils;
import com.blinq.utils.Constants;
import com.blinq.utils.DialogUtils;
import com.blinq.utils.DialogUtils.DialogType;
import com.blinq.utils.ExternalAppsUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;

/**
 * Open web pages locally (in the application) without external browsers.
 *
 * @author Johan Hansson.
 */
public class WebPageActivity extends HeadboxBaseActivity {

    private static final String TAG = WebPageActivity.class.getSimpleName();
    private boolean youtubeAppOpened = false;

    private WebView webView;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_page);
        AppUtils.unRegisterActivityGoingIntoBackground(this);

        hideActionBar();

        loadingDialog = DialogUtils.createCustomDialog(this, DialogType.CUSTOM);

        init();

        openWebPageURL();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (AppUtils.isActivityIdle(this) || youtubeAppOpened)
            finish();
        super.onResume();
    }

    @Override
    protected void onPause() {
        AppUtils.registerActivityGoingIntoBackground(this);
        super.onPause();
    }

    /**
     * Initialize activity components.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void init() {

        webView = (WebView) findViewById(R.id.webViewForLicenseContent);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new PageWebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

    }

    /**
     * Get web page URL from activity extras.
     *
     * @return web page URL.
     */
    private String getWebPageURL() {

        return getIntent().getExtras().getString(Constants.WEB_PAGE_LINK);
    }

    /**
     * Open web page URL in local web-view.
     */
    private void openWebPageURL() {

        String webPageURL = getWebPageURL();

        if (!StringUtils.isBlank(webPageURL)) {

            if (StringUtils.validateYoutubeUrl(webPageURL)) {
                startActivity(ExternalAppsUtils.getYoutubeIntent(this,
                        webPageURL));
                youtubeAppOpened = true;
            } else {
                DialogUtils.showDialog(this, loadingDialog);
                webView.loadUrl(webPageURL);
            }

        } else {

            Log.e(TAG, "Failed URL");
        }

    }

    /**
     * Web View client for the web view.
     */
    private class PageWebViewClient extends WebViewClient {

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            Log.d(TAG, "Web Page error: " + errorCode + description);
            DialogUtils.hideDialog(WebPageActivity.this, loadingDialog);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d(TAG, "Web Page Started " + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            Log.d(TAG, "Web Page Finished " + url);

            DialogUtils.hideDialog(WebPageActivity.this, loadingDialog);
        }
    }
}
