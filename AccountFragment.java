package com.example.islamic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
// 🌟 NEW IMPORT: Required for WebChromeClient
import android.webkit.WebChromeClient;
// 💡 NEW IMPORT: Required for the @JavascriptInterface annotation
import android.webkit.JavascriptInterface;
import android.widget.TextView;
// 🌟 NEW IMPORT: Required for the ImageButton used for logout
import android.widget.ImageButton;
// 💡 NEW IMPORTS for file chooser
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.widget.Toast;

/**
 * A fragment to display account information and an embedded WebView for dynamic content.
 */
public class AccountFragment extends Fragment {

    // 🚀 CONSTANTS for SharedPreferences file name and key (must match user_welcome.java)
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";

    // 💡 NEW CONSTANT: Request code for the file chooser intent
    private static final int FILECHOOSER_RESULTCODE = 1;

    // Argument keys for passing data
    private static final String ARG_URL = "url";

    private String username;
    private String contentUrl;

    // View references
    private TextView titleTextView;
    private WebView webView;
    // 🌟 NEW FIELD: Reference to the logout button
    private ImageButton logoutButton;

    // 💡 NEW FIELD: To hold the callback for the file chooser
    private ValueCallback<Uri[]> mUploadMessage;


    public AccountFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param url The URL to load in the embedded WebView.
     * @return A new instance of fragment AccountFragment.
     */
    public static AccountFragment newInstance(String url) {
        AccountFragment fragment = new AccountFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            contentUrl = getArguments().getString(ARG_URL, "https://en.wikipedia.org/wiki/Islam");
        } else {
            contentUrl = "file:///android_asset/manual_withreview.html";
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout (R.layout.fragment_account)
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // 1. Initialize Views
        titleTextView = view.findViewById(R.id.title);
        webView = view.findViewById(R.id.webview);
        // 🌟 NEW: Initialize the logout button
        logoutButton = view.findViewById(R.id.logout_button);

        // 🌟 START: Retrieve Username from SharedPreferences
        SharedPreferences sharedPref = requireActivity().getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        // Load the username, defaulting to "Guest" if not found
        username = sharedPref.getString(KEY_USERNAME, "Guest");
        // 🌟 END: Retrieve Username

        // 2. Set the Title Text using the retrieved username
      //  titleTextView.setText("Hello, " + username + "!");

        // 🌟 NEW: Set up the Logout Button Listener to call the logout function
        logoutButton.setOnClickListener(v -> performLogout());

        // 3. Configure and load WebView
        // Enable JavaScript for modern web content (crucial for most websites)
        webView.getSettings().setJavaScriptEnabled(true);

        // 🟢 ADDED: Enable DOM Storage, required for HTML5 web apps
        webView.getSettings().setDomStorageEnabled(true);

        // 💡 NEW: Set WebChromeClient to handle UI-related events (like alerts, prompts, and progress)
        // This is crucial for handling the file picker from an HTML <input type="file">
        webView.setWebChromeClient(new CustomWebChromeClient());

        // Set a WebViewClient to ensure that all links clicked within the WebView
        // open inside the WebView itself, instead of launching an external browser.
        webView.setWebViewClient(new WebViewClient());

        // 🟢 NEW: Add JavaScript Interface to allow HTML/JS to call Java methods
        // The JavaScript object name exposed to the WebView will be "Android"
        webView.addJavascriptInterface(new WebAppInterface(requireContext()), "Android");

        // Load the specified content URL
        webView.loadUrl(contentUrl);

        return view;
    }

    /**
     * 🌟 NEW METHOD: Handles clearing preferences and navigating to the login screen.
     */
    private void performLogout() {
        // 1. Clear SharedPreferences
        SharedPreferences sharedPref = requireActivity().getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        // Remove the stored username, effectively logging out the current user session.
        editor.remove(KEY_USERNAME);
        // Commit changes asynchronously
        editor.apply();

        // 2. Give user feedback
        Toast.makeText(requireContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();

        // 3. Navigate back to the Login activity
        // NOTE: Changed target activity to Login.class as requested.
        Intent intent = new Intent(requireActivity(), Login.class);

        // Clear the activity stack so the user cannot press 'Back' to return to this fragment/activity
        // This clears the current flow and prevents the user from going back to the authenticated state.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        requireActivity().finish(); // Finish the current hosting activity
    }

    // ---

    /**
     * 💡 NEW: Handle the result from the file picker Intent.
     * This method is called when the user selects a file and returns to the app.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if this is the result for our file chooser request
        if (requestCode == FILECHOOSER_RESULTCODE) {
            // Check if we have an active callback
            if (mUploadMessage == null) {
                return;
            }

            // Get the result data (the selected file URIs)
            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK) {
                // If the intent data is not null, get the URI(s) from it
                if (intent != null) {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    } else if (intent.getClipData() != null) {
                        // Handle multiple file selection if supported
                        int count = intent.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = intent.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
            }

            // Pass the result back to the WebView's JavaScript
            mUploadMessage.onReceiveValue(results);
            mUploadMessage = null; // Clear the callback
        }
    }

    // ---

    /**
     * 💡 NEW INNER CLASS: Custom WebChromeClient to override file chooser.
     */
    private class CustomWebChromeClient extends WebChromeClient {
        // 💡 NEW: This method is called when the HTML/JS initiates a file selection
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            // Check if there is already an active file selection request
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
            }

            mUploadMessage = filePathCallback;

            // Create the intent to pick a photo from the device
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            // Set the MIME type to images (you can change this to suit your needs, e.g., "video/*")
            i.setType("image/*");

            try {
                // Start the activity for result
                startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILECHOOSER_RESULTCODE);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Cannot open file chooser", Toast.LENGTH_LONG).show();
                mUploadMessage = null; // Clear the callback if there's an error
                return false;
            }

            return true;
        }
    }

    // ---

    /**
     * 💡 NEW INNER CLASS: Interface for the JavaScript code to retrieve necessary Android data.
     * Methods must be annotated with @JavascriptInterface to be callable from JS.
     */
    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** 🟢 NEW: Method to retrieve the username from Android. */
        @JavascriptInterface
        public String getUsername() {
            // Retrieve the username again from SharedPreferences
            SharedPreferences sharedPref = mContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            return sharedPref.getString(KEY_USERNAME, "Guest");
        }

        /** 🟢 NEW: Method to retrieve the User ID (which is the username in this app). */
        @JavascriptInterface
        public String getUserId() {
            // Return the same value as the username, as it's used as the ID for database
            SharedPreferences sharedPref = mContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            return sharedPref.getString(KEY_USERNAME, "Guest");
        }
    }
}
