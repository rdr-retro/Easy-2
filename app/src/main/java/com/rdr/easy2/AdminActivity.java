package com.rdr.easy2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {
    private TextView statusView;
    private View emptyStateView;
    private WebView webView;
    private String dashboardUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        statusView = findViewById(R.id.admin_status_view);
        emptyStateView = findViewById(R.id.admin_empty_state);
        webView = findViewById(R.id.admin_web_view);

        Button refreshButton = findViewById(R.id.admin_refresh_button);
        Button settingsButton = findViewById(R.id.admin_settings_button);
        Button emptyStateButton = findViewById(R.id.admin_open_setup_button);

        if (refreshButton != null) {
            refreshButton.setOnClickListener(view -> reloadDashboard());
        }
        if (settingsButton != null) {
            settingsButton.setOnClickListener(view -> openSetup());
        }
        if (emptyStateButton != null) {
            emptyStateButton.setOnClickListener(view -> openSetup());
        }

        configureWebView();
        loadDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private void configureWebView() {
        if (webView == null) {
            return;
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                showStatus(getString(R.string.admin_panel_connected, url));
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error
            ) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    showStatus(getString(R.string.admin_panel_load_error));
                }
            }
        });
    }

    private void loadDashboard() {
        if (!LauncherPreferences.isAdminMode(this)) {
            finish();
            return;
        }

        String serverUrl = LauncherPreferences.getRemoteServerUrl(this);
        if (TextUtils.isEmpty(serverUrl)) {
            dashboardUrl = "";
            if (webView != null) {
                webView.setVisibility(View.GONE);
            }
            if (emptyStateView != null) {
                emptyStateView.setVisibility(View.VISIBLE);
            }
            showStatus(getString(R.string.admin_panel_missing_server));
            return;
        }

        dashboardUrl = RemoteServerClient.buildDashboardUrl(serverUrl);

        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.GONE);
        }
        if (webView != null) {
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(dashboardUrl);
        }
        showStatus(getString(R.string.admin_panel_loading, dashboardUrl));
    }

    private void reloadDashboard() {
        if (webView == null || TextUtils.isEmpty(dashboardUrl)) {
            loadDashboard();
            return;
        }
        showStatus(getString(R.string.admin_panel_loading, dashboardUrl));
        webView.reload();
    }

    private void openSetup() {
        startActivity(new Intent(this, SetupActivity.class));
    }

    private void showStatus(String message) {
        if (statusView != null) {
            statusView.setText(message);
        }
    }
}
