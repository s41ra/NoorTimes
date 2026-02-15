package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class user_analytics extends AppCompatActivity {

    private TextView titleTextView;
    private WebView webView;
    // Placeholder URL for demonstration. Replace this with your actual analytics dashboard URL.
    private static final String ANALYTICS_URL = "file:///android_asset/user_monitoring.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming your layout file is named activity_user_analytics.xml
        setContentView(R.layout.activity_user_analytics);

        // 1. Initialize Views
        titleTextView = findViewById(R.id.title);
        webView = findViewById(R.id.webview);

        // 2. Set the Title/Username
        // TODO: Replace "User" with the actual personalized username logic
        String username = "User";
        titleTextView.setText("Data Analytics");

        // 3. Configure and Load WebView
        setupWebView();
    }

    private void setupWebView() {
        // Enable JavaScript, which is essential for most web applications/dashboards
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Set WebViewClient to handle redirects and loading within the WebView itself,
        // preventing the links from opening in an external browser.
        webView.setWebViewClient(new WebViewClient());

        // Optional: Improve performance and user experience
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);

        // Load the specified URL
        webView.loadUrl(ANALYTICS_URL);
    }

    // Optional: Add support for back button navigation within the WebView
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
