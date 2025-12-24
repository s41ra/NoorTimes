package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

public class admin_prayer_analytics extends AppCompatActivity {

    private ImageButton backButton;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sets the content view to the layout defined in activity_admin_prayer_analytics.xml
        setContentView(R.layout.activity_admin_prayer_analytics);

        // 1. Initialize views
        backButton = findViewById(R.id.back_button);
        webView = findViewById(R.id.webview);

        // 2. Set up the back button's click listener
        // The back button should typically finish the current activity
        backButton.setOnClickListener(v -> finish());

        // 3. Configure and load content into the WebView
        setupWebView();
    }

    /**
     * Configures the WebView settings and loads a URL.
     * NOTE: You need to replace "YOUR_ANALYTICS_URL" with the actual URL
     * where the analytics content is hosted.
     */
    private void setupWebView() {
        // Enable JavaScript (often required for analytics dashboards)
        webView.getSettings().setJavaScriptEnabled(true);

        // Set a WebViewClient to keep the loading within the app
        webView.setWebViewClient(new WebViewClient() {
            // Optional: You can override methods like onPageStarted/onPageFinished
        });

        // Load the URL for the analytics dashboard
        // ✅ FIX: Changed "ile" to "file" in the protocol.
        String analyticsUrl = "file:///android_asset/admin_prayer_analytics.html";
        webView.loadUrl(analyticsUrl);
    }

    /**
     * Optional: Handle the system's back button press to navigate within the WebView.
     * If the WebView can go back, navigate within the WebView. Otherwise, close the Activity.
     */
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}