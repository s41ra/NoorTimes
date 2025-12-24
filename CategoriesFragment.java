package com.example.islamic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri; // 💡 NEW: Required for handling URI/Intent
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CategoriesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CategoriesFragment extends Fragment {

    // 🚀 CONSTANTS for SharedPreferences file name and key (must match user_welcome.java)
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";
    private static final String WEBVIEW_URL = "file:///android_asset/calendar.html"; // Defined URL

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CategoriesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CategoriesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CategoriesFragment newInstance(String param1, String param2) {
        CategoriesFragment fragment = new CategoriesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment. Assuming the layout XML is named fragment_categories.xml
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        // 🌟 START: Retrieve Username from SharedPreferences
        String username = "User"; // Default fallback username

        // Fragments need to get the Activity context to access SharedPreferences
        if (getActivity() != null) {
            SharedPreferences sharedPref = getActivity().getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            // Retrieve the username, falling back to "Guest" if not found (matching user_welcome's fallback)
            username = sharedPref.getString(KEY_USERNAME, "Guest");
        }
        // 🌟 END: Retrieve Username

        // 1. Update the TextView with the personalized username
        TextView titleTextView = view.findViewById(R.id.title);
        if (titleTextView != null) {
            // Use the retrieved username to personalize the message
            titleTextView.setText("Event Calendar");
        }

        // 2. Initialize and load content into the WebView
        WebView webView = view.findViewById(R.id.webview);
        if (webView != null) {
            WebSettings webSettings = webView.getSettings();

            // 1. Enable JavaScript
            webSettings.setJavaScriptEnabled(true);

            // 2. Enable DOM storage (localStorage/sessionStorage)
            webSettings.setDomStorageEnabled(true);

            // 3. Enable Database/Caching for better performance on re-loads
            webSettings.setDatabaseEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

            // 4. Enable support for the 'viewport' HTML meta tag
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);

            // 🚀 MODIFIED: Set a custom WebViewClient to handle map URLs
            webView.setWebViewClient(new WebViewClient() {

                // Override the URL loading method
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {

                    // Check if the URL should be handled by an external application
                    if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("geo:")) {
                        // Create an Intent to launch the system handler for this URI
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true; // Indicate that the host application handles the URL
                    }

                    // Also specifically check for common map domains if they are not Geo: URIs
                    if (url.contains("maps.google.com") || url.contains("waze.com") || url.contains("yandex.ru/maps")) {
                        // Create an Intent to launch the system handler for this URI
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true; // Indicate that the host application handles the URL
                    }

                    // Let the WebView load all other URLs internally
                    return false;
                }
            });

            // Load the content
            webView.loadUrl(WEBVIEW_URL);
        }

        return view;
    }
}