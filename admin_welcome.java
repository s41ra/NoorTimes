package com.example.islamic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class admin_welcome extends AppCompatActivity {

    private WebView centerWebView;
    private ProgressBar loadingSpinner;
    private TextView welcomeTextView;
    private TextView userFirstNameTextView;
    private Button getStartedButton;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_welcome);

        centerWebView = findViewById(R.id.centerWebView);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        welcomeTextView = findViewById(R.id.welcomeTextView);
        userFirstNameTextView = findViewById(R.id.userFirstNameTextView);
        getStartedButton = findViewById(R.id.getStartedButton);

        // Get username from previous activity
        String username = getIntent().getStringExtra("username");

        // Get first name from SharedPreferences or other source
        String firstName = getCurrentUserFirstName();

        // Update welcome message and first name display
        if (firstName != null && !firstName.isEmpty()) {
            welcomeTextView.setText("Assalamu Alaikum,");
            userFirstNameTextView.setText(firstName);
        } else if (username != null && !username.isEmpty()) {
            // Fallback to username if first name is not available
            welcomeTextView.setText("Assalamu Alaikum,");
            userFirstNameTextView.setText(username);
        } else {
            welcomeTextView.setText("Assalamu Alaikum,");
            userFirstNameTextView.setText("Admin");
        }

        // Configure WebView
        centerWebView.getSettings().setJavaScriptEnabled(true);
        centerWebView.setWebViewClient(new WebViewClient());
        centerWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100 && loadingSpinner.getVisibility() == View.GONE) {
                    loadingSpinner.setVisibility(View.VISIBLE);
                }
                if (newProgress == 100) {
                    loadingSpinner.setVisibility(View.GONE);
                }
            }
        });

        centerWebView.loadUrl("file:///android_asset/todayevent_admin.html");

        // Button -> go to AdminDashboard
        getStartedButton.setOnClickListener(v -> {
            Intent intent = new Intent(admin_welcome.this, admindashboard.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });
    }

    /**
     * Retrieves the current user's first name from SharedPreferences
     * You can modify this method to get from Firebase, database, etc.
     */
    private String getCurrentUserFirstName() {
        try {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            return prefs.getString("firstName", "");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void onBackPressed() {
        if (centerWebView.canGoBack()) {
            centerWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}