package com.edison.tamirkar;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.webkit.PermissionRequest;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.net.Uri;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String APP_URL =
            "https://snappai.co/s/6aa6149ff34e2be15933a1830991878f";
    private static final int NAVY = Color.rgb(10, 17, 34);
    private static final int PANEL = Color.rgb(20, 31, 55);
    private static final int CYAN = Color.rgb(67, 220, 218);
    private static final int WHITE = Color.rgb(244, 248, 255);

    private WebView webView;
    private FrameLayout root;
    private LinearLayout statusPanel;
    private ProgressBar progressBar;
    private TextView statusTitle;
    private TextView statusMessage;
    private TextView retryButton;
    private boolean pageLoaded = false;
    private static final int AUDIO_PERMISSION_REQUEST = 4102;
    private PermissionRequest pendingAudioRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildInterface();
        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL);
        } else if (webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(APP_URL);
        }
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        root.setBackgroundColor(NAVY);
        setContentView(root);

        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (!pageLoaded) showLoading();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                hideStatusPanel();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) showError();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                showError();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    Uri origin = request.getOrigin();
                    boolean trustedOrigin = origin != null
                            && "https".equalsIgnoreCase(origin.getScheme())
                            && "snappai.co".equalsIgnoreCase(origin.getHost());
                    boolean audioOnly = request.getResources().length == 1
                            && PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(request.getResources()[0]);
                    if (!trustedOrigin || !audioOnly) {
                        request.deny();
                        return;
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                            || checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                    } else {
                        pendingAudioRequest = request;
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                                AUDIO_PERMISSION_REQUEST);
                    }
                });
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingAudioRequest == request) pendingAudioRequest = null;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) progressBar.setProgress(newProgress);
                if (newProgress >= 100 && pageLoaded) hideStatusPanel();
            }
        });
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        buildStatusPanel();
        showLoading();
    }

    private void buildStatusPanel() {
        statusPanel = new LinearLayout(this);
        statusPanel.setOrientation(LinearLayout.VERTICAL);
        statusPanel.setGravity(Gravity.CENTER);
        statusPanel.setPadding(dp(30), dp(34), dp(30), dp(34));
        statusPanel.setBackgroundColor(NAVY);

        LinearLayout logo = new LinearLayout(this);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable logoBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(29, 199, 207), Color.rgb(95, 91, 232)});
        logoBg.setShape(GradientDrawable.OVAL);
        logo.setBackground(logoBg);
        TextView logoText = new TextView(this);
        logoText.setText("م");
        logoText.setTextColor(Color.WHITE);
        logoText.setTextSize(42);
        logoText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        logoText.setGravity(Gravity.CENTER);
        logo.addView(logoText, new LinearLayout.LayoutParams(dp(92), dp(92)));
        statusPanel.addView(logo);

        TextView brand = new TextView(this);
        brand.setText("دستیار اس ممد");
        brand.setTextColor(WHITE);
        brand.setTextSize(27);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        brand.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams brandLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brandLp.topMargin = dp(22);
        statusPanel.addView(brand, brandLp);

        TextView tagline = new TextView(this);
        tagline.setText("همراه هوشمند تو، همیشه آماده");
        tagline.setTextColor(Color.rgb(166, 184, 211));
        tagline.setTextSize(14);
        tagline.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagLp.topMargin = dp(8);
        statusPanel.addView(tagline, tagLp);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(22), dp(20), dp(22), dp(20));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(PANEL);
        cardBg.setCornerRadius(dp(20));
        card.setBackground(cardBg);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(34);
        statusPanel.addView(card, cardLp);

        statusTitle = new TextView(this);
        statusTitle.setText("در حال آماده‌سازی دستیار...");
        statusTitle.setTextColor(WHITE);
        statusTitle.setTextSize(16);
        statusTitle.setGravity(Gravity.CENTER);
        statusTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        card.addView(statusTitle);

        statusMessage = new TextView(this);
        statusMessage.setText("لطفاً چند لحظه صبر کن");
        statusMessage.setTextColor(Color.rgb(166, 184, 211));
        statusMessage.setTextSize(13);
        statusMessage.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgLp.topMargin = dp(8);
        card.addView(statusMessage, msgLp);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(12);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(CYAN));
        progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(CYAN));
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        progressLp.topMargin = dp(22);
        card.addView(progressBar, progressLp);

        retryButton = new TextView(this);
        retryButton.setText("تلاش دوباره");
        retryButton.setTextColor(NAVY);
        retryButton.setTextSize(15);
        retryButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        retryButton.setGravity(Gravity.CENTER);
        retryButton.setPadding(dp(24), dp(12), dp(24), dp(12));
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setColor(CYAN);
        buttonBg.setCornerRadius(dp(14));
        retryButton.setBackground(buttonBg);
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> {
            pageLoaded = false;
            showLoading();
            webView.loadUrl(APP_URL);
        });
        LinearLayout.LayoutParams retryLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        retryLp.topMargin = dp(18);
        statusPanel.addView(retryButton, retryLp);

        TextView footer = new TextView(this);
        footer.setText("توسعه و ساخت: مهندس رعیتی");
        footer.setTextColor(Color.rgb(111, 132, 165));
        footer.setTextSize(11);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = dp(28);
        statusPanel.addView(footer, footerLp);

        root.addView(statusPanel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void showLoading() {
        statusTitle.setText("در حال اتصال به دستیار...");
        statusMessage.setText("لطفاً چند لحظه صبر کن");
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        statusPanel.setVisibility(View.VISIBLE);
    }

    private void showError() {
        statusTitle.setText("اتصال برقرار نشد");
        statusMessage.setText("اینترنت را بررسی کن و دوباره تلاش کن.");
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
        statusPanel.setVisibility(View.VISIBLE);
    }

    private void hideStatusPanel() {
        if (statusPanel != null) statusPanel.setVisibility(View.GONE);
    }

    private boolean handleUrl(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) { }
        return true;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION_REQUEST && pendingAudioRequest != null) {
            PermissionRequest request = pendingAudioRequest;
            pendingAudioRequest = null;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
            } else {
                request.deny();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (pendingAudioRequest != null) {
            pendingAudioRequest.deny();
            pendingAudioRequest = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
