package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences; // Import SharedPreferences
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient; // 👈 1. Import WebChromeClient
import android.widget.ImageButton;
import android.widget.TextView;

public class admin_profile_account extends AppCompatActivity {

    private TextView titleTextView;
    private ImageButton backButton;
    private WebView webView;

    // 1. Define the Intent extra key for the username
    public static final String EXTRA_USERNAME = "com.example.islamic.extra.USERNAME";

    // Use the same constants as AccountFragment_admin.java
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming the layout file name is activity_admin_profile_account.xml based on the class name
        setContentView(R.layout.activity_admin_profile_account);

        // 2. Initialize views
        titleTextView = findViewById(R.id.title);
        backButton = findViewById(R.id.back_button);
        webView = findViewById(R.id.webview);

        // 3. Retrieve the username
        // Priority 1: Get from Intent (passed by AccountFragment_admin)
        String username = getIntent().getStringExtra(EXTRA_USERNAME);

        // Priority 2: If Intent data is null, get from SharedPreferences (for consistency/fallback)
        if (username == null) {
            username = getUsernameFromSharedPreferences();
        }

        // Ensure username is not null for display and JS
        final String finalUsername = (username != null && !username.isEmpty()) ? username : "Admin";

        // Based on saved information: [2025-09-29] The username is came from the provided signup code.
     //   titleTextView.setText("Hello, " + finalUsername + "!");

        // 4. Set up the Back Button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the current activity and return to the previous screen
                finish();
            }
        });

        // 5. Set up the WebView
        // Enable JavaScript (often necessary for modern web pages)
        webView.getSettings().setJavaScriptEnabled(true);

        // Prevent redirects from opening in the external browser
        // Set a WebViewClient to know when the page is finished loading
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // IMPORTANT: Inject JavaScript *after* the page has fully loaded
                // 6. Inject the username into the HTML page
                // Escape the username string to handle special characters in JavaScript
                String escapedUsername = finalUsername.replace("'", "\\'");

                // Call the JavaScript function setAdminUsername in the HTML
                String javascriptCode = "javascript:setAdminUsername('" + escapedUsername + "');";

                // Use evaluateJavascript for Android 4.4 (API 19) and later
                view.evaluateJavascript(javascriptCode, null);
            }
        });

        // Set up the WebChromeClient for handling UI-related events (like JavaScript alerts, favicons, and progress changes)
        webView.setWebChromeClient(new WebChromeClient());

        // Load the local HTML file from the assets folder
        String localAssetUrl = "file:///android_asset/admin_profile_account.html";
        webView.loadUrl(localAssetUrl);
    }

    /**
     * Retrieves the stored username from SharedPreferences for consistency/fallback.
     */
    private String getUsernameFromSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        // The saved username is derived from the signup code.
        return prefs.getString(KEY_USERNAME, "Admin"); // Default to "Admin" if not found
    }
}