package com.example.islamic;

import android.content.Context; // 💡 ADDED: Required for getSharedPreferences
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
import android.content.Intent; // 💡 ADDED
import android.net.Uri; // 💡 ADDED
import android.util.Log; // 💡 ADDED

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class CategoriesFragment_admin extends Fragment {

    private static final String TAG = "CategoriesFragment_admin"; // 💡 ADDED for Log
    private TextView titleTextView;
    private WebView webView;

    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    public CategoriesFragment_admin() {
        // Required empty public constructor
    }

    public static CategoriesFragment_admin newInstance() {
        return new CategoriesFragment_admin();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories_admin, container, false);

        // Bind views
        titleTextView = view.findViewById(R.id.title);
        webView = view.findViewById(R.id.webview);

        // Load username from SharedPreferences
        String username = getUsername();
    //    titleTextView.setText("Hello, " + username + "!");

        // Configure WebView
        if (webView != null) {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);

            // 💡 MODIFIED: Use the CustomWebViewClient to handle map/external links
            webView.setWebViewClient(new CustomWebViewClient());
            webView.setWebChromeClient(new WebChromeClient());

            // Load a default page (replace with your URL)
            webView.loadUrl("file:///android_asset/calendar.html");
        }

        return view;
    }

    private String getUsername() {
        if (getContext() != null) {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            return prefs.getString(KEY_USERNAME, "Admin");
        }
        return "Admin"; // Fallback
    }

    // ---------------------------------------------------------------------
    // Custom WebViewClient to force specific links (like Google Maps) to open in the native app
    // ---------------------------------------------------------------------
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // 1. Check if the URL is a Google Maps link or a Geo URI
            if (url.contains("maps.google.com") || url.contains("google.com/maps") || url.startsWith("geo:")) {
                Log.d(TAG, "Attempting to open Maps link externally: " + url);

                try {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                    // --- START OF FIX: Force Google Maps app ---
                    // Try 1: Force opening the Google Maps application by setting the package name.
                    final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";
                    intent.setPackage(GOOGLE_MAPS_PACKAGE);

                    if (getActivity() != null && intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        // Found the specific Google Maps app, launch it directly.
                        startActivity(intent);
                        return true; // Indicate that we handled the URL with the native app
                    } else {
                        // Try 2 (Fallback): Google Maps app not found or cannot resolve.
                        // Use Intent.createChooser to strongly signal the system that the user must
                        // select an external app (preferably a map app).
                        Log.d(TAG, "Google Maps app not found or cannot resolve. Trying generic map intent with chooser.");

                        Intent genericIntent = new Intent(Intent.ACTION_VIEW, uri);
                        Intent chooser = Intent.createChooser(genericIntent, "View Map with...");

                        if (getActivity() != null && chooser.resolveActivity(getActivity().getPackageManager()) != null) {
                            startActivity(chooser);
                            return true;
                        }

                        // Final fallback: If no suitable app is found even with the chooser, let WebView load the URL (the website).
                        Log.d(TAG, "No suitable app found to handle the Maps Intent. Letting WebView handle.");
                        return false;
                    }
                    // --- END OF FIX ---
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Google Maps link:", e);
                }
            }

            // 2. Handle other specific native schemes (phone, email, SMS)
            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if (getActivity() != null && intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                        return true; // Link handled by native app
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening specific scheme link:", e);
                }
            }

            // 3. Handle all other external http/https links by opening them in the default browser/app
            if (url.startsWith("http://") || url.startsWith("https://")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if (getActivity() != null && intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                        return true; // Link handled by external app
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening generic external link:", e);
                }
            }

            // 4. For all other URLs (e.g., internal assets), let WebView load them.
            return false;
        }
    }
}