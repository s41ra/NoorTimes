package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
// ⭐️ New imports for WebChromeClient
import android.webkit.WebChromeClient;
import android.widget.ImageButton;

public class admin_data_analytics extends AppCompatActivity {

    private ImageButton backButton;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure this matches your layout file name, which is assumed to be R.layout.activity_admin_data_analytics
        setContentView(R.layout.activity_admin_data_analytics);

        // 1. Initialize UI elements
        backButton = findViewById(R.id.back_button);
        webView = findViewById(R.id.webview);

        // 2. Set up the Back Button functionality
        backButton.setOnClickListener(v -> {
            // Finish the current activity, navigating back to the previous screen
            finish();
        });

        // 3. Set up the WebView
        setupWebView();
    }

    private void setupWebView() {
        // Get WebSettings object to configure the WebView
        WebSettings webSettings = webView.getSettings();

        // Enable JavaScript (often necessary for interactive analytics dashboards)
        webSettings.setJavaScriptEnabled(true);

        // Enable DOM Storage (useful for modern web applications)
        webSettings.setDomStorageEnabled(true);

        // Optional: Ensure links open within the WebView instead of a default browser
        webView.setWebViewClient(new WebViewClient());

        // ⭐️ ADDED: Set WebChromeClient to handle UI-related events
        // (like JavaScript dialogs, favicons, titles, and full-screen video)
        webView.setWebChromeClient(new WebChromeClient());

        // Load the URL for your data analytics dashboard
        // 🚨 IMPORTANT: Replace "YOUR_ANALYTICS_DASHBOARD_URL_HERE" with the actual URL
        // Example: webView.loadUrl("https://your-firebase-dashboard.com/analytics");
        webView.loadUrl("file:///android_asset/admin_data_analytics.html");
    }

    // Optional: Handle the device's back button press to allow going back in web history
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}