package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences; // Import SharedPreferences
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient; // 1. Import WebChromeClient
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class admin_useraccount extends AppCompatActivity {

    private TextView titleTextView;
    private ImageButton backButton;
    private WebView webView;

    // Use the same constants as Login.java and AccountFragment_admin.java
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";
    // Default value if the username is not found in SharedPreferences
    private static final String DEFAULT_USERNAME_PLACEHOLDER = "AdminUser";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to your XML layout
        setContentView(R.layout.activity_admin_useraccount);

        // 1. Initialize Views
        titleTextView = findViewById(R.id.title);
        backButton = findViewById(R.id.back_button);
        webView = findViewById(R.id.webview);

        // 2. Set Personalized Username
        // Fetch the username from SharedPreferences
        String fetchedUsername = getUsername();
    //    String greeting = "User Account Management (" + fetchedUsername + ")"; // Updated title format
     //   titleTextView.setText(greeting);

        // 3. Set Back Button Listener
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This will close the current activity and return to the previous one
                finish();
            }
        });

        // 4. Initialize and Configure WebView
        setupWebView();
    }

    /**
     * Retrieves the username from SharedPreferences using the same keys as
     * AccountFragment_admin.java.
     */
    private String getUsername() {
        // Use MODE_PRIVATE to access the app's private preferences file
        SharedPreferences sharedPrefs = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        // Based on your saved information: The username is came from the provided signup code.
        // Retrieve the stored username or use the placeholder if not found
        return sharedPrefs.getString(KEY_USERNAME, DEFAULT_USERNAME_PLACEHOLDER);
    }

    /**
     * Configures the WebView settings and loads content.
     */
    private void setupWebView() {
        // Set a WebViewClient to keep the user within your app when clicking links
        webView.setWebViewClient(new WebViewClient());

        // 2. Set WebChromeClient
        // A WebChromeClient is necessary for handling UI-related events,
        // such as JavaScript alerts, progress changes, and the page's title.
        webView.setWebChromeClient(new WebChromeClient());

        // Enable JavaScript (important for most modern websites/admin panels)
        webView.getSettings().setJavaScriptEnabled(true);

        // Load the content (e.g., the admin user account dashboard URL)
        String adminDashboardUrl = "file:///android_asset/admin_modifyaccount.html";
        webView.loadUrl(adminDashboardUrl);

        Toast.makeText(this, "Loading Admin Panel...", Toast.LENGTH_SHORT).show();
    }

    // Optional: Handle the back button press to navigate within the WebView history first
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack(); // Go back in WebView history
        } else {
            super.onBackPressed(); // Otherwise, close the activity
        }
    }
}