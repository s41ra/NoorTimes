package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

// IMPORTANT FIREBASE IMPORTS
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

// NEW IMPORTS FOR NETWORKING (OkHttp)
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

// NOTE: You must add the OkHttp dependency to your app/build.gradle file:
// implementation 'com.squareup.okhttp3:okhttp:4.12.0'

public class Login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREF_FILE_NAME = "IslamicAppPrefs";
    private static final String KEY_USERNAME = "current_username";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ADMIN = "admin";

    // API CONFIGURATION
    private static final String API_TOKEN = "7f0f0f0e27545dcc65e81e2554cb21a4d3e9f88c";
    private static final String SEND_OTP_URL = "https://sms.iprogtech.com/api/v1/otp/send_otp";
    private static final String VERIFY_OTP_URL = "https://sms.iprogtech.com/api/v1/otp/verify_otp";


    // LOGIN UI ELEMENTS
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpText;
    private ImageView logoImageView;
    private TextView loginTextView;

    // NEW OTP UI ELEMENTS
    private EditText otpEditText;
    private Button verifyOtpButton;
    private TextView otpInstructionText;

    // COMMON UI ELEMENTS
    private ProgressBar progressBar;

    // STATE VARIABLES
    private FirebaseFirestore db;
    private OkHttpClient httpClient; // OkHttp Client
    private String currentLoginUsername = ""; // Stores username after successful password check
    private String currentLoginRole = ""; // Stores role after successful password check
    private String currentMobilePhone = ""; // Stores the user's mobile phone number


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for existing login credentials
        if (isUserLoggedIn()) {
            Log.d(TAG, "User is already logged in. Bypassing Login screen.");
            navigateToAppropriateWelcomeScreen();
            return;
        }

        // NOTE: Ensure you have an activity_login.xml layout file in res/layout/
        setContentView(R.layout.activity_login);

        // Initialize Firebase Firestore and OkHttp Client
        db = FirebaseFirestore.getInstance();
        httpClient = new OkHttpClient(); // Initialize OkHttp

        // Initialize Views
        initializeViews();

        // Set up Listeners
        setupListeners();
    }

    private void initializeViews() {
        // Login Views
        usernameEditText = findViewById(R.id.username_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        signUpText = findViewById(R.id.sign_up_text);
        logoImageView = findViewById(R.id.logo);
        loginTextView = findViewById(R.id.login_text);

        // OTP Views (New)
        otpEditText = findViewById(R.id.otp_edit_text);
        verifyOtpButton = findViewById(R.id.verify_otp_button);
        otpInstructionText = findViewById(R.id.otp_instruction_text);

        // Common Views
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        verifyOtpButton.setOnClickListener(v -> attemptOtpVerification());
        signUpText.setOnClickListener(v -> navigateToSignUp());
    }

    /**
     * Toggles the visibility of the UI elements based on the current step (Login or OTP).
     * @param isLoginStep true for the initial login screen, false for the OTP screen.
     */
    private void switchUI(boolean isLoginStep) {
        int loginVisibility = isLoginStep ? View.VISIBLE : View.GONE;
        // int otpVisibility = isLoginStep ? View.GONE : View.VISIBLE; // Not needed as Verify button is always VISIBLE when isLoginStep is false

        // Login UI elements
        usernameEditText.setVisibility(loginVisibility);
        passwordEditText.setVisibility(loginVisibility);
        loginButton.setVisibility(loginVisibility);
        loginTextView.setText(isLoginStep ? "Login to your account" : "Verify Your Account");

        // OTP UI elements
        otpInstructionText.setVisibility(isLoginStep ? View.GONE : View.VISIBLE);
        otpEditText.setVisibility(isLoginStep ? View.GONE : View.VISIBLE);
        // CRITICAL CHANGE: Ensure verifyOtpButton is visible only in the OTP step, and login button is hidden
        verifyOtpButton.setVisibility(isLoginStep ? View.GONE : View.VISIBLE);

        // Hide sign-up text during OTP verification
        signUpText.setVisibility(loginVisibility);
    }

    // NEW OVERRIDE METHOD
    @Override
    public void onBackPressed() {
        if (!currentLoginUsername.isEmpty() && !currentLoginRole.equals(ROLE_ADMIN)) {
            // User is on the OTP screen (and not an admin who would have already logged in),
            // pressing back should return to login/reset state
            resetUI();
            Toast.makeText(this, "Login session reset.", Toast.LENGTH_SHORT).show();
        } else {
            // User is on the login screen or is an admin who successfully logged in (which shouldn't happen
            // because of finish() call, but good practice to allow default back behavior otherwise)
            super.onBackPressed();
        }
    }

    // --- STEP 1: LOGIN ATTEMPT ---

    private void attemptLogin() {
        final String username = usernameEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            if (username.isEmpty()) usernameEditText.setError("Username is required");
            if (password.isEmpty()) passwordEditText.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        Log.d(TAG, "Attempting login for user: " + username);

        // Try admin login first
        checkAdminLogin(username, password);
    }

    /**
     * Checks for admin credentials. If successful, bypasses OTP and navigates directly.
     */
    private void checkAdminLogin(final String username, final String password) {
        db.collection("islamicadmin").document(username)
                .get()
                .addOnCompleteListener(adminTask -> {
                    progressBar.setVisibility(View.GONE); // Always hide progress bar after check begins

                    if (adminTask.isSuccessful()) {
                        DocumentSnapshot adminDocument = adminTask.getResult();
                        if (adminDocument.exists()) {
                            String storedPassword = adminDocument.getString("password");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                // **ADMIN PASSWORD SUCCESS - DIRECT LOGIN (BYPASS OTP)**
                                Log.d(TAG, "Password correct for Admin: " + username + ". Bypassing OTP.");

                                // 1. Save Login State
                                saveLoginState(username, ROLE_ADMIN);

                                // 2. Navigate Directly
                                Toast.makeText(Login.this, "Welcome Admin, " + username + "!", Toast.LENGTH_LONG).show();
                                navigateToWelcomeScreen(admin_welcome.class, username);

                                // 3. Finish Login Activity
                                finish();
                                return; // Stop execution here
                            }
                        }
                    }

                    // If not an admin, or admin password failed, try user login next
                    // Note: Progress bar is already hidden by the adminTask's listener
                    checkUserLogin(username, password);
                });
    }

    /**
     * Checks for regular user credentials. If successful, proceeds to OTP verification.
     */
    private void checkUserLogin(final String username, final String password) {
        db.collection("islamusers").document(username)
                .get()
                .addOnCompleteListener(userTask -> {
                    // Progress bar should already be hidden from checkAdminLogin, but we might show it again if we send OTP

                    if (userTask.isSuccessful()) {
                        DocumentSnapshot userDocument = userTask.getResult();
                        if (userDocument.exists()) {
                            String storedPassword = userDocument.getString("password");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                // USER PASSWORD SUCCESS -> PROCEED TO OTP
                                Log.d(TAG, "Password correct for User: " + username);
                                currentLoginUsername = username;
                                currentLoginRole = ROLE_USER;

                                String mobilePhone = userDocument.getString("mobile_phone");

                                handlePasswordSuccess(mobilePhone); // Calls OTP send API and shows progress bar
                                return;
                            } else {
                                Log.d(TAG, "Login failed: Password mismatch.");
                                Toast.makeText(Login.this, "Invalid username or password.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.d(TAG, "Login failed: User not found in either collection.");
                            Toast.makeText(Login.this, "Invalid username or password.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "Firestore query failed: ", userTask.getException());
                        Toast.makeText(Login.this, "Error accessing database. Check connection.", Toast.LENGTH_LONG).show();
                    }

                    resetUI(); // Reset UI only if login fails completely
                });
    }

    /**
     * Called after the username/password is verified for regular users. Stores phone and attempts to send OTP.
     * @param mobilePhone The user's mobile phone number retrieved from Firestore.
     */
    private void handlePasswordSuccess(String mobilePhone) {
        // Progress bar should be hidden by checkAdminLogin completion, but we hide it here just in case,
        // then show it again in sendOtpToApi
        progressBar.setVisibility(View.GONE);
        currentMobilePhone = (mobilePhone != null) ? mobilePhone.trim() : "";

        if (currentMobilePhone.isEmpty()) {
            Log.e(TAG, "Cannot send OTP: Mobile phone number is missing from Firestore record.");
            Toast.makeText(Login.this, "Verification error: Phone number missing.", Toast.LENGTH_LONG).show();
            resetUI();
            return;
        }

        switchUI(false); // Switch to OTP input view

        // Masking the phone number for user instruction
        String maskedPhone = currentMobilePhone;
        if (currentMobilePhone.length() > 4) {
            // Masks all but the last 4 digits
            maskedPhone = currentMobilePhone.substring(currentMobilePhone.length() - 4);
            maskedPhone = "****" + maskedPhone;
        }
        otpInstructionText.setText("An OTP has been sent to the number ending in " + maskedPhone + ".");

        // Call the API to send the OTP
        sendOtpToApi(currentMobilePhone);
    }

    // --- API CALL TO SEND OTP ---
    private void sendOtpToApi(String phoneNumber) {
        progressBar.setVisibility(View.VISIBLE);
        verifyOtpButton.setEnabled(false);
        otpEditText.setEnabled(false);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("api_token", API_TOKEN);
            jsonBody.put("phone_number", phoneNumber);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(SEND_OTP_URL)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "OTP Send API Failure: " + e.getMessage());
                        Toast.makeText(Login.this, "Failed to send OTP. Check connection.", Toast.LENGTH_LONG).show();
                        // Allow user to try again or verify later
                        progressBar.setVisibility(View.GONE);
                        verifyOtpButton.setEnabled(true);
                        otpEditText.setEnabled(true);
                        otpEditText.requestFocus();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        verifyOtpButton.setEnabled(true);
                        otpEditText.setEnabled(true);
                        otpEditText.requestFocus();

                        if (response.isSuccessful()) {
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                // The API response indicates success or failure
                                if (jsonResponse.has("status") && "success".equals(jsonResponse.getString("status"))) {
                                    Log.d(TAG, "OTP Sent successfully. Response: " + responseBody);
                                    Toast.makeText(Login.this, "OTP Sent! Check your phone.", Toast.LENGTH_LONG).show();
                                } else {
                                    String message = jsonResponse.optString("message", "Unknown OTP send error.");
                                    Log.e(TAG, "OTP API reported error: " + message);
                                    Toast.makeText(Login.this, "Error: " + message, Toast.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parse error on OTP send: " + e.getMessage());
                                Toast.makeText(Login.this, "Server response error.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e(TAG, "OTP Send HTTP Error: " + response.code() + " Body: " + responseBody);
                            Toast.makeText(Login.this, "OTP API failed. Try again.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error for send OTP: " + e.getMessage());
            resetUI();
        }
    }


    // --- STEP 2: OTP VERIFICATION ATTEMPT ---

    private void attemptOtpVerification() {
        // Key step for validity: Check if we even have a valid state to verify against
        if (currentMobilePhone.isEmpty() || currentLoginUsername.isEmpty()) {
            Toast.makeText(Login.this, "Error: Invalid login state. Please try logging in again.", Toast.LENGTH_LONG).show();
            resetUI();
            return;
        }

        final String enteredOtp = otpEditText.getText().toString().trim();

        if (enteredOtp.isEmpty() || enteredOtp.length() < 4) { // Assuming min 4-digit OTP
            otpEditText.setError("Enter the OTP code");
            otpEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        verifyOtpButton.setEnabled(false);

        // Call the API to verify the OTP
        verifyOtpWithApi(currentMobilePhone, enteredOtp);
    }

    // --- API CALL TO VERIFY OTP ---
    private void verifyOtpWithApi(String phoneNumber, String otp) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("api_token", API_TOKEN);
            jsonBody.put("phone_number", phoneNumber);
            jsonBody.put("otp", otp);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(VERIFY_OTP_URL)
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "OTP Verify API Failure: " + e.getMessage());
                        Toast.makeText(Login.this, "Verification failed. Check connection.", Toast.LENGTH_LONG).show();
                        verifyOtpButton.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        verifyOtpButton.setEnabled(true);
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                // The API response indicates success or failure
                                if (jsonResponse.has("status") && "success".equals(jsonResponse.getString("status"))) {
                                    // **OTP VERIFICATION SUCCESS**
                                    Log.d(TAG, "OTP Verification Successful for user: " + currentLoginUsername);
                                    saveLoginState(currentLoginUsername, currentLoginRole);
                                    Toast.makeText(Login.this, "Verification Successful! Welcome.", Toast.LENGTH_LONG).show();

                                    // Navigate based on the stored role (should be ROLE_USER here)
                                    navigateToWelcomeScreen(user_welcome.class, currentLoginUsername);

                                    // Crucial: Finish the login activity after successful navigation
                                    finish();
                                } else {
                                    // **OTP FAILED**
                                    String message = jsonResponse.optString("message", "Invalid OTP. Please try again.");
                                    Log.d(TAG, "OTP Verification Failed: " + message);
                                    Toast.makeText(Login.this, message, Toast.LENGTH_LONG).show();
                                    otpEditText.setError("Invalid OTP");
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parse error on OTP verify: " + e.getMessage());
                                Toast.makeText(Login.this, "Server response error.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // HTTP Error, likely invalid token or malformed request
                            Log.e(TAG, "OTP Verify HTTP Error: " + response.code() + " Body: " + responseBody);
                            Toast.makeText(Login.this, "Verification API failed. Try again.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error for verify OTP: " + e.getMessage());
            verifyOtpButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }
    }

    // --- HELPER METHODS ---

    private void saveLoginState(String username, String role) {
        SharedPreferences sharedPref = getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences sharedPref = getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPref.contains(KEY_USERNAME);
    }

    private void navigateToAppropriateWelcomeScreen() {
        SharedPreferences sharedPref = getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        String username = sharedPref.getString(KEY_USERNAME, null);
        String role = sharedPref.getString(KEY_USER_ROLE, ROLE_USER);

        if (username != null) {
            if (ROLE_ADMIN.equals(role)) {
                navigateToWelcomeScreen(admin_welcome.class, username);
            } else {
                navigateToWelcomeScreen(user_welcome.class, username);
            }
            // Finish this activity so the user cannot press back to the login screen
            finish();
        }
    }

    private void navigateToWelcomeScreen(Class<?> destinationActivity, String username) {
        Intent intent = new Intent(Login.this, destinationActivity);
        intent.putExtra(KEY_USERNAME, username); // Pass the username to the welcome screen
        startActivity(intent);
    }

    /**
     * This method creates an Intent to navigate to the Signup.class.
     */
    private void navigateToSignUp() {
        // Assuming SignUpActivity is named Signup.class
        Intent intent = new Intent(Login.this, Signup.class);
        startActivity(intent);
    }

    /**
     * Resets UI and all state variables to return to the initial login screen.
     */
    private void resetUI() {
        // Reset to initial login state
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);

        // **CRUCIAL STATE RESET**
        currentLoginUsername = "";
        currentLoginRole = "";
        currentMobilePhone = ""; // Reset phone number

        // Clear any text fields to prevent stale data
        usernameEditText.setText("");
        passwordEditText.setText("");
        otpEditText.setText("");

        switchUI(true); // Switch back to the login view
    }
}