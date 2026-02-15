package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; // Import SharedPreferences
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient; // 👈 1. Import WebChromeClient
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class admin_sms extends AppCompatActivity {

    private WebView webView;
    private ImageButton backButton;
    private TextView titleTextView;

    // Define the URL for the local asset file
    private static final String ANALYTICS_ASSET_URL = "file:///android_asset/sms_final.html";

    // ✅ Added Constants for SharedPreferences (Same as AccountFragment_admin)
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 0. The current theme is set to use the landscape style.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_sms);

        // 1. Initialize the Views
        webView = findViewById(R.id.webview);
        backButton = findViewById(R.id.back_button);
        titleTextView = findViewById(R.id.title);

        // 2. Set the Username
        // ✅ Retrieve the actual username from SharedPreferences
        String username = getUsername();

        // Per the instruction: "The username is came from the provided signup code."
        // The getUsername() method now correctly retrieves the username saved during login.
        String greetingText = "SMS Tool (" + username + ")";
        titleTextView.setText(greetingText);

        // 3. Set up the Back Button functionality
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to the previous activity or close the app
                onBackPressed();
            }
        });

        // 4. Set up the WebView
        setupWebView();
    }

    /**
     * ✅ Retrieves the current username from SharedPreferences.
     * Uses the same logic and keys as AccountFragment_admin.
     * @return The saved username or "Admin" as a default.
     */
    private String getUsername() {
        // Use getSharedPreferences() on the Context (Activity)
        SharedPreferences prefs = getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        // The saved username is derived from the signup code.
        return prefs.getString(KEY_USERNAME, "Admin");
    }

    private void setupWebView() {
        // Optional: Get WebSettings for customization
        WebSettings webSettings = webView.getSettings();

        // Enable JavaScript (often required for interactive local files too)
        webSettings.setJavaScriptEnabled(true);

        // This setting is often needed when loading local files, especially if they
        // try to load other assets like CSS/JS from relative paths.
        webSettings.setAllowFileAccess(true);

        // 2. Set the WebChromeClient for handling UI-related events (like JavaScript alerts)
        webView.setWebChromeClient(new WebChromeClient()); // 👈 **Added WebChromeClient**

        // Ensures links are opened within the WebView
        webView.setWebViewClient(new WebViewClient() {
            // Optional: Handle page load error (less common for local files unless they are missing)
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Check if the error is related to the specific file
                if (failingUrl != null && failingUrl.equals(ANALYTICS_ASSET_URL)) {
                    // Corrected the filename in the Toast message
                    Toast.makeText(admin_sms.this, "Error loading local file: " + description + ". Make sure 'sms_final.html' is in the 'assets' folder.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Load the local HTML file from the assets folder
        webView.loadUrl(ANALYTICS_ASSET_URL);
    }

    // Override onBackPressed to handle back navigation within the WebView
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            // If the WebView has a history, navigate back within the WebView
            webView.goBack();
        } else {
            // Otherwise, let the system handle it (go to the previous Activity)
            super.onBackPressed();
        }
    }
}