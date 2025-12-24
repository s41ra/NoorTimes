package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings; // Import for WebSettings
import android.webkit.WebView;
import android.webkit.WebViewClient; // Import for WebViewClient
import android.widget.ImageButton;
import android.widget.TextView;

public class admin_event_analytics extends AppCompatActivity {

    private ImageButton backButton;
    private TextView titleTextView;
    private WebView webView;

    // A placeholder URL for a generic analytics or information page
    private static final String ANALYTICS_URL = "file:///android_asset/admin_event_analytics.html"; // Replace with your actual dashboard URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_analytics);

        // 1. Initialize Views
        backButton = findViewById(R.id.back_button);
        titleTextView = findViewById(R.id.title);
        webView = findViewById(R.id.webview);

        // 2. Set up the Back Button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finishes the current activity and returns to the previous one
                finish();
            }
        });

        // 3. Handle the Username Display (Personalization based on saved information)
        // Using the rule: "The username to be shared is derived from the user's email
        // address by removing everything from the '@' symbol onwards."
        String actualEmailOrSignupCode = "admin.user@islamicapp.com"; // Placeholder
        String username;

        // Simulate username extraction logic
        if (actualEmailOrSignupCode.contains("@")) {
            username = actualEmailOrSignupCode.substring(0, actualEmailOrSignupCode.indexOf('@'));
        } else {
            username = actualEmailOrSignupCode;
        }

        // Update the TextView: "Hello, username!"
     //   titleTextView.setText("Hello, " + username + "!");

        // 4. Set up the WebView (for event analytics display)
        setupWebView();
    }

    // New method to encapsulate WebView setup
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // Enable JavaScript support, often required for modern web dashboards
        webSettings.setJavaScriptEnabled(true);

        // Optional: Enable features like local storage, important for web apps
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // Set a WebViewClient to keep navigation inside the app
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Load all URLs within this WebView (don't open external browser)
                view.loadUrl(url);
                return true;
            }
        });

        // Load the analytics URL
        webView.loadUrl(ANALYTICS_URL);
    }

    // Optional: Allow the user to navigate back within the WebView using the device's back button
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}