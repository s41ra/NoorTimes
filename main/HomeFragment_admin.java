package com.example.islamic;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class HomeFragment_admin extends Fragment {

    private TextView titleTextView;
    private WebView webView;

    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    public HomeFragment_admin() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home_admin, container, false);

        // Initialize TextView
        titleTextView = view.findViewById(R.id.title);
        // Get username from SharedPreferences
        String username = getUsername();

        // Changed greeting to "Assalamu Alaikum" as requested
        titleTextView.setText("Assalamu Alaikum, " + username + "!");

        // Initialize WebView
        webView = view.findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript if needed
        webSettings.setDomStorageEnabled(true); // Enable DOM storage

        webView.setWebViewClient(new WebViewClient()); // Open URLs inside the WebView
        webView.setWebChromeClient(new WebChromeClient()); // Handle JavaScript dialogs, etc.
        webView.loadUrl("file:///android_asset/admin_monitoring.html"); // Default URL

        return view;
    }

    private String getUsername() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_FILE_NAME, getContext().MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "Admin");
    }
}
