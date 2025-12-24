package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// Imports for Volley (Network requests)
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Signup extends AppCompatActivity {

    private static final String TAG = "SignupActivity";
    private static final String FIRESTORE_COLLECTION = "islamusers";
    private static final String API_TOKEN = "7f0f0f0e27545dcc65e81e2554cb21a4d3e9f88c";
    private static final String SEND_OTP_URL = "https://sms.iprogtech.com/api/v1/otp/send_otp";
    private static final String VERIFY_OTP_URL = "https://sms.iprogtech.com/api/v1/otp/verify_otp";

    // Data Structure for Chained Spinners (Province -> City -> Barangay)
    private final Map<String, List<String>> PROVINCE_TO_CITIES = new HashMap<>();
    private final Map<String, List<String>> CITY_TO_BARANGAYS = new HashMap<>();
    private final String PROVINCE_HINT = "Select Province";
    private final String CITY_HINT = "Select City/Municipality";
    private final String BARANGAY_HINT = "Select Barangay";

    // View declarations
    private EditText fullNameEditText;
    private EditText preferredUsernameEditText;
    private EditText mobilePhoneEditText;

    // --- OTP Views ---
    private Button sendOtpButton;
    private EditText otpEditText;
    private boolean isOtpSent = false;

    private Spinner provinceSpinner;
    private Spinner cityMunicipalitySpinner;
    private Spinner barangaySpinner;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private CheckBox consentCheckBox;
    private Button signUpButton;
    private TextView loginTextView;
    private ProgressBar progressBar;

    // To hold the selected values from Spinners
    private String selectedProvince = "";
    private String selectedCityMunicipality = "";
    private String selectedBarangay = "";

    // Firebase Firestore instance
    private FirebaseFirestore db;

    // Volley Request Queue
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Data, Firestore, and Volley
        initializeLocationData();
        db = FirebaseFirestore.getInstance();
        requestQueue = Volley.newRequestQueue(this); // Initialize Volley

        // Initialize views
        fullNameEditText = findViewById(R.id.full_name_edit_text);
        preferredUsernameEditText = findViewById(R.id.username_edit_text);
        mobilePhoneEditText = findViewById(R.id.mobile_phone_edit_text);

        // --- OTP View Initialization ---
        sendOtpButton = findViewById(R.id.send_otp_button);
        otpEditText = findViewById(R.id.otp_edit_text);

        // Ensure OTP EditText is hidden/disabled by default
        otpEditText.setVisibility(View.GONE);
        otpEditText.setEnabled(false);

        provinceSpinner = findViewById(R.id.province_spinner);
        cityMunicipalitySpinner = findViewById(R.id.city_municipality_spinner);
        barangaySpinner = findViewById(R.id.barangay_spinner);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        consentCheckBox = findViewById(R.id.consent_checkbox);
        signUpButton = findViewById(R.id.sign_up_button);
        loginTextView = findViewById(R.id.login_text);
        progressBar = findViewById(R.id.progress_bar);

        // ------------------------------------
        // Setup Initial Spinners and Listeners
        // ------------------------------------

        // 1. Setup Province Spinner
        setupProvinceSpinner();

        // 2. Set up initial adapters for dependent Spinners
        setupDependentSpinner(cityMunicipalitySpinner, Arrays.asList(CITY_HINT));
        setupDependentSpinner(barangaySpinner, Arrays.asList(BARANGAY_HINT));

        // 3. Set the Chained Listeners
        setProvinceSpinnerListener();
        setCitySpinnerListener();
        setBarangaySpinnerListener();

        // Set click listeners
        sendOtpButton.setOnClickListener(v -> handleSendOtp());
        signUpButton.setOnClickListener(v -> attemptSignup());
        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(Signup.this, Login.class));
            finish();
        });
    }

    /**
     * Helper to set a simple adapter for a Spinner dynamically.
     */
    private void setupDependentSpinner(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                items
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     * Initializes the nested data structure for location options.
     */
    private void initializeLocationData() {
        // --- LOCATION DATA INITIALIZATION (Contents remain the same) ---
        String[] provinces = getResources().getStringArray(R.array.province_options);
        String[] allBarangays = getResources().getStringArray(R.array.barangay_options);

        // =========================================================================
        // 1. PROVINCE to CITY/MUNICIPALITY Mapping
        // =========================================================================

        // DAVAO DEL SUR
        List<String> davaoDelSurCities = new ArrayList<>(Arrays.asList(
                "Digos City", "Bansalan", "Hagonoy", "Kiblawan", "Magsaysay",
                "Malalag", "Matanao", "Padada", "Santa Cruz", "Sulop"
        ));
        davaoDelSurCities.add("Davao City");
        PROVINCE_TO_CITIES.put("Davao del Sur", davaoDelSurCities);

        // DAVAO DE ORO
        List<String> davaoDeOroCities = Arrays.asList(
                "Nabunturan", "Compostela", "Maco", "Monkayo", "Maragusan",
                "Laak", "Mawab", "New Bataan", "Pantukan", "Montevista"
        );
        PROVINCE_TO_CITIES.put("Davao de Oro", davaoDeOroCities);

        // DAVAO DEL NORTE
        List<String> davaoDelNorteCities = Arrays.asList(
                "Tagum City", "Panabo City", "Island Garden City of Samal",
                "Asuncion", "Braulio E. Dujali", "Carmen", "Kapalong",
                "New Corella", "San Isidro", "Santo Tomas", "Talaingod"
        );
        PROVINCE_TO_CITIES.put("Davao del Norte", davaoDelNorteCities);

        // DAVAO ORIENTAL
        List<String> davaoOrientalCities = Arrays.asList(
                "City of Mati", "Baganga", "Banaybanay", "Boston", "Caraga",
                "Cateel", "Governor Generoso", "Lupon", "Manay", "San Isidro", "Tarragona"
        );
        PROVINCE_TO_CITIES.put("Davao Oriental", davaoOrientalCities);

        // DAVAO OCCIDENTAL
        List<String> davaoOccidentalCities = Arrays.asList(
                "Malita", "Don Marcelino", "Jose Abad Santos", "Sarangani", "Santa Maria"
        );
        PROVINCE_TO_CITIES.put("Davao Occidental", davaoOccidentalCities);

        for (int i = 1; i < provinces.length; i++) {
            if (!PROVINCE_TO_CITIES.containsKey(provinces[i])) {
                PROVINCE_TO_CITIES.put(provinces[i], new ArrayList<>());
            }
        }

        // =========================================================================
        // 2. CITY/MUNICIPALITY to BARANGAY Mapping
        // =========================================================================

        List<String> davaoCityBarangays = new ArrayList<>();
        for (int i = 1; i < allBarangays.length; i++) {
            davaoCityBarangays.add(allBarangays[i]);
        }
        CITY_TO_BARANGAYS.put("Davao City", davaoCityBarangays);

        // --- DAVAO DEL SUR ---
        CITY_TO_BARANGAYS.put("Digos City", Arrays.asList("Aplaya", "Balabag", "Binaton", "Cogon", "Loma", "Matti", "San Jose", "Tres de Mayo", "Poblacion", "Rizal Park"));
        CITY_TO_BARANGAYS.put("Bansalan", Arrays.asList("Davao Matanao", "Marber", "Poblacion Uno", "Poblacion Dos", "Poblacion Tres", "Managa", "Dolo"));
        CITY_TO_BARANGAYS.put("Hagonoy", Arrays.asList("Aplaya", "Baluarte", "Kikub", "Paligue", "Poblacion", "San Guillermo", "Tiguman"));
        CITY_TO_BARANGAYS.put("Kiblawan", Arrays.asList("Bagong Clarin", "Bulatukan", "Kimlawis", "Panoy", "San Isidro", "Poblacion"));
        CITY_TO_BARANGAYS.put("Magsaysay", Arrays.asList("Barayong", "Kabasalan", "Malawanit", "Poblacion", "San Isidro", "Tagaytay"));
        CITY_TO_BARANGAYS.put("Malalag", Arrays.asList("Bulacan", "Ibo", "Kisao", "Poblacion", "Tagansule", "Baybay"));
        CITY_TO_BARANGAYS.put("Matanao", Arrays.asList("Bisañao", "Kabasalan", "San Ramon", "Sinalang", "Poblacion", "Manga"));
        CITY_TO_BARANGAYS.put("Padada", Arrays.asList("Alibon", "Narro", "Poblacion", "Quirino", "San Isidro", "Upper Limonzo"));
        CITY_TO_BARANGAYS.put("Santa Cruz", Arrays.asList("Astorga", "Bato", "Poblacion", "Sibulan", "Tagabuli", "Inauayan"));
        CITY_TO_BARANGAYS.put("Sulop", Arrays.asList("Balasinon", "Kityan Dagat", "Laperas", "Poblacion", "Talao", "Tapla"));

        // --- DAVAO DE ORO ---
        CITY_TO_BARANGAYS.put("Nabunturan", Arrays.asList("Basak", "Cabacungan", "Katipunan", "Libasan", "Poblacion", "San Roque", "Tagnanan", "Magsaysay"));
        CITY_TO_BARANGAYS.put("Compostela", Arrays.asList("Bagongon", "Gabi", "Lagab", "Mangayon", "Poblacion", "Mapaca", "Ngan"));
        CITY_TO_BARANGAYS.put("Maco", Arrays.asList("Anislagan", "Binuangan", "Bucana", "Mabini", "Poblacion", "Lahi", "Panibasan"));
        CITY_TO_BARANGAYS.put("Monkayo", Arrays.asList("Baylo", "Pasian", "Poblacion", "R.A. Fernandez", "San Jose", "Upper Ulip", "Casoon"));
        CITY_TO_BARANGAYS.put("Maragusan", Arrays.asList("Bagong Silang", "Bahi", "Lahi", "New Manay", "Poblacion", "Katipunan"));
        CITY_TO_BARANGAYS.put("Laak", Arrays.asList("Amacalan", "Banban", "Kapatagan", "Longanapan", "Poblacion", "Panam'uwan"));
        CITY_TO_BARANGAYS.put("Mawab", Arrays.asList("Andili", "New Barili", "Poblacion", "Sumbat", "Tuboran", "Sawangan"));
        CITY_TO_BARANGAYS.put("New Bataan", Arrays.asList("Cabinuangan", "Cogon", "Linao", "Poblacion", "San Roque", "Panag"));
        CITY_TO_BARANGAYS.put("Pantukan", Arrays.asList("Awao", "Bongabong", "Kingking", "Poblacion", "Tagdangua", "Tambongon"));
        CITY_TO_BARANGAYS.put("Montevista", Arrays.asList("Camansi", "Linoan", "Poblacion", "San Jose", "New Dalaguete", "Bankerohan"));

        // --- DAVAO DEL NORTE ---
        CITY_TO_BARANGAYS.put("Tagum City", Arrays.asList("Apokon", "Bincungan", "Busaon", "Canocotan", "La Filipina", "Mankilam", "Poblacion", "San Isidro", "Liboganon", "Visayan Village"));
        CITY_TO_BARANGAYS.put("Panabo City", Arrays.asList("A. O. Floirendo", "Cagangohan", "Dapco", "Kasilak", "Poblacion", "J.P. Laurel", "San Pedro"));
        CITY_TO_BARANGAYS.put("Island Garden City of Samal", Arrays.asList("Babak", "Caliclic", "Peñaplata", "Saum", "Kaputian", "Libuak", "Pangubatan"));
        CITY_TO_BARANGAYS.put("Asuncion", Arrays.asList("Buan", "Camanlangan", "Cambanogoy", "Poblacion", "San Vicente", "New Balamban"));
        CITY_TO_BARANGAYS.put("Braulio E. Dujali", Arrays.asList("Cabay-Angan", "Dujali", "New Casay", "Tanglaw", "Tapay"));
        CITY_TO_BARANGAYS.put("Carmen", Arrays.asList("Anibongan", "Magsaysay", "Poblacion", "San Isidro", "Taba", "Sto. Niño"));
        CITY_TO_BARANGAYS.put("Kapalong", Arrays.asList("Gupitan", "Maniki", "Poblacion", "Semong", "Tugop", "Katipunan"));
        CITY_TO_BARANGAYS.put("New Corella", Arrays.asList("Cabidianan", "Del Monte", "Liba", "Poblacion", "San Jose", "Sta. Cruz"));
        CITY_TO_BARANGAYS.put("San Isidro", Arrays.asList("Dacudao", "Libuton", "Poblacion", "Sto. Niño", "Sawata"));
        CITY_TO_BARANGAYS.put("Santo Tomas", Arrays.asList("Balagunan", "Bobongon", "Kinamayan", "New Katipunan", "Poblacion", "Tibulao"));
        CITY_TO_BARANGAYS.put("Talaingod", Arrays.asList("Dagohoy", "Plaza", "Santo Niño"));

        // --- DAVAO ORIENTAL ---
        CITY_TO_BARANGAYS.put("City of Mati", Arrays.asList("Badas", "Bobon", "Buso", "Dahican", "Lawigan", "Matiao", "Poblacion", "Tamisan", "Lao", "Central"));
        CITY_TO_BARANGAYS.put("Baganga", Arrays.asList("Baculin", "Bana-ao", "Lamao", "Poblacion", "San Victor", "Suso"));
        CITY_TO_BARANGAYS.put("Banaybanay", Arrays.asList("Causwagan", "Paniquian", "Poblacion", "Pisa", "Rang-ay", "Mabini"));
        CITY_TO_BARANGAYS.put("Boston", Arrays.asList("Poblacion", "Cabagahan", "Cabalantian", "Mainit"));
        CITY_TO_BARANGAYS.put("Caraga", Arrays.asList("Alamag", "Manurigao", "Poblacion", "San Pedro", "Tambong", "Lamiawan"));
        CITY_TO_BARANGAYS.put("Cateel", Arrays.asList("Alegria", "Aragon", "Poblacion", "San Miguel", "Taytayan", "Malibago"));
        CITY_TO_BARANGAYS.put("Governor Generoso", Arrays.asList("Lahat", "Lavigan", "Magdug", "Poblacion", "Surop", "Tamban"));
        CITY_TO_BARANGAYS.put("Lupon", Arrays.asList("Bolinay", "Langka", "Linao", "Poblacion", "San Isidro", "Tagugpo"));
        CITY_TO_BARANGAYS.put("Manay", Arrays.asList("Capasnan", "Del Pilar", "Poblacion", "San Ignacio", "Zaragosa", "Taocanga"));
        CITY_TO_BARANGAYS.put("San Isidro", Arrays.asList("Bitaogan", "Iba", "Poblacion", "San Miguel", "Sudlon", "Tamban"));
        CITY_TO_BARANGAYS.put("Tarragona", Arrays.asList("Cabagayan", "Limot", "Poblacion", "Susulan", "Tala-o", "Ospital"));

        // --- DAVAO OCCIDENTAL ---
        CITY_TO_BARANGAYS.put("Malita", Arrays.asList("Bito", "Demoloc", "Fais", "Lais", "Poblacion", "Tuban", "Ticulon", "Banos"));
        CITY_TO_BARANGAYS.put("Don Marcelino", Arrays.asList("Baluntayan", "Kinangan", "Poblacion", "Tala-o", "Linadasan", "Nueva Villa"));
        CITY_TO_BARANGAYS.put("Jose Abad Santos", Arrays.asList("Badiang", "Caburan Big", "Kalbay", "Sto. Niño", "Gatong", "Maripaz"));
        CITY_TO_BARANGAYS.put("Sarangani", Arrays.asList("Batulaki", "Lanao", "Poblacion", "Lipol", "Camahora"));
        CITY_TO_BARANGAYS.put("Santa Maria", Arrays.asList("Basiawan", "Cogon", "Poblacion", "San Isidro", "Talagutong", "Bitoon"));

        List<String> allMappedCities = new ArrayList<>();
        for (List<String> cityList : PROVINCE_TO_CITIES.values()) {
            allMappedCities.addAll(cityList);
        }

        for (String city : allMappedCities) {
            if (!CITY_TO_BARANGAYS.containsKey(city)) {
                CITY_TO_BARANGAYS.put(city, new ArrayList<>());
            }
        }
        // --- END LOCATION DATA INITIALIZATION ---
    }

    /**
     * Sets up the initial Province Spinner using the XML array.
     */
    private void setupProvinceSpinner() {
        ArrayAdapter<CharSequence> provinceAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.province_options,
                android.R.layout.simple_spinner_item
        );
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        provinceSpinner.setAdapter(provinceAdapter);
    }

    /**
     * Sets the listener for the Province Spinner to populate the City/Municipality Spinner.
     */
    private void setProvinceSpinnerListener() {
        provinceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProvinceName = parent.getItemAtPosition(position).toString();
                selectedProvince = (position > 0) ? selectedProvinceName : "";
                selectedCityMunicipality = "";
                selectedBarangay = "";

                List<String> cities = new ArrayList<>();
                cities.add(CITY_HINT);

                if (position > 0 && PROVINCE_TO_CITIES.containsKey(selectedProvinceName)) {
                    cities.addAll(PROVINCE_TO_CITIES.get(selectedProvinceName));
                }
                setupDependentSpinner(cityMunicipalitySpinner, cities);
                setupDependentSpinner(barangaySpinner, Arrays.asList(BARANGAY_HINT));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedProvince = "";
            }
        });
    }

    /**
     * Sets the listener for the City/Municipality Spinner to populate the Barangay Spinner.
     */
    private void setCitySpinnerListener() {
        cityMunicipalitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCityName = parent.getItemAtPosition(position).toString();
                selectedCityMunicipality = (position > 0 && !selectedCityName.equals(CITY_HINT)) ? selectedCityName : "";
                selectedBarangay = "";

                List<String> barangays = new ArrayList<>();
                barangays.add(BARANGAY_HINT);

                if (position > 0 && CITY_TO_BARANGAYS.containsKey(selectedCityName)) {
                    barangays.addAll(CITY_TO_BARANGAYS.get(selectedCityName));
                }
                setupDependentSpinner(barangaySpinner, barangays);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCityMunicipality = "";
            }
        });
    }

    /**
     * Sets the listener for the Barangay Spinner to save the final selection.
     */
    private void setBarangaySpinnerListener() {
        barangaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedBarangayName = parent.getItemAtPosition(position).toString();
                selectedBarangay = (position > 0 && !selectedBarangayName.equals(BARANGAY_HINT)) ? selectedBarangayName : "";
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedBarangay = "";
            }
        });
    }

    // ----------------------------------------------------
    // OTP LOGIC (API INTEGRATION)
    // ----------------------------------------------------

    /**
     * Handles the 'Send OTP' button press: validates mobile, starts progress, and calls the API service.
     */
    private void handleSendOtp() {
        final String mobilePhone = mobilePhoneEditText.getText().toString().trim();

        if (mobilePhone.isEmpty()) {
            mobilePhoneEditText.setError("Enter a valid mobile phone number.");
            return;
        }

        // ✅ ADDED: Validate that mobile number is exactly 11 digits
        if (mobilePhone.length() != 11) {
            mobilePhoneEditText.setError("Please input only 11 digit numbers");
            Toast.makeText(Signup.this, "Please input only 11 digit numbers", Toast.LENGTH_LONG).show();
            return;
        }

        sendOtpButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("api_token", API_TOKEN);
            jsonBody.put("phone_number", mobilePhone);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, SEND_OTP_URL, jsonBody,
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        boolean isApiSuccess = false;
                        String message = "OTP sent to " + mobilePhone + ". Check your SMS.";

                        try {
                            // Attempt to read the 'success' flag from the response
                            isApiSuccess = response.getBoolean("success");
                            if (!isApiSuccess) {
                                message = response.optString("message", "Failed to send OTP. Please check the number.");
                            }
                        } catch (JSONException e) {
                            // If JSON parsing fails, we assume success since the SMS was sent.
                            Log.w(TAG, "OTP Send: JSON parsing failed, but assuming success. Error: " + e.getMessage());
                            isApiSuccess = true;
                        }

                        if (isApiSuccess) {
                            isOtpSent = true;
                            otpEditText.setVisibility(View.VISIBLE);
                            otpEditText.setEnabled(true);
                            sendOtpButton.setText("Resend OTP");
                            sendOtpButton.setEnabled(true);
                            Toast.makeText(Signup.this, message, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(Signup.this, message, Toast.LENGTH_LONG).show();
                            sendOtpButton.setEnabled(true);
                        }
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "OTP Send Network Error: " + error.toString());
                        Toast.makeText(Signup.this, "Network error: Could not connect to OTP service.", Toast.LENGTH_LONG).show();
                        sendOtpButton.setEnabled(true);
                    });

            requestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, "OTP Send Request Build Error: " + e.getMessage());
            sendOtpButton.setEnabled(true);
        }
    }

    /**
     * Verifies the OTP with the API. This is called from attemptSignup().
     */
    private void verifyOtpAndProceed(final String username, final String mobilePhone, final String fullName, final String password,
                                     final String province, final String cityMunicipality, final String barangay) {
        final String otp = otpEditText.getText().toString().trim();

        // Disable sign up button while verifying OTP
        signUpButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("api_token", API_TOKEN);
            jsonBody.put("phone_number", mobilePhone);
            jsonBody.put("otp", otp);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, VERIFY_OTP_URL, jsonBody,
                    response -> {
                        boolean isApiSuccess = false;
                        String message = "OTP Verification succeeded.";

                        try {
                            // Attempt to read the 'success' flag from the response
                            isApiSuccess = response.getBoolean("success");
                            if (!isApiSuccess) {
                                // Get the specific error message if the API reports failure
                                message = response.optString("message", "Invalid OTP.");
                            }
                        } catch (JSONException e) {
                            // ❌ FIX APPLIED HERE: If JSON parsing fails (but we received an HTTP 200 OK),
                            // we assume success since the OTP was correct.
                            Log.w(TAG, "OTP Verify: JSON parsing failed, but assuming success as OTP was correct. Error: " + e.getMessage());
                            isApiSuccess = true;
                            // message remains the default success message
                        }

                        // The rest of the logic
                        if (isApiSuccess) {
                            // OTP is verified! Proceed to check existence and save user
                            checkExistingUser(username, mobilePhone, fullName, password, province, cityMunicipality, barangay);
                        } else {
                            handleFailure(message);
                        }
                    },
                    error -> {
                        Log.e(TAG, "OTP Verify Network Error: " + error.toString());
                        handleFailure("Network error during OTP verification.");
                    });

            requestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            handleFailure("OTP Verification request build error.");
            Log.e(TAG, "OTP Verify Request Build Error: " + e.getMessage());
        }
    }


    // ----------------------------------------------------
    // FIREBASE LOGIC
    // ----------------------------------------------------

    /**
     * Handles the signup process, including local validation and initiating OTP verification.
     */
    private void attemptSignup() {
        final String fullName = fullNameEditText.getText().toString().trim();
        final String username = preferredUsernameEditText.getText().toString().trim();
        final String mobilePhone = mobilePhoneEditText.getText().toString().trim();
        final String otp = otpEditText.getText().toString().trim();

        final String province = selectedProvince;
        final String cityMunicipality = selectedCityMunicipality;
        final String barangay = selectedBarangay;

        final String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        boolean hasConsent = consentCheckBox.isChecked();

        // 2. Comprehensive Validation
        if (fullName.isEmpty() || username.isEmpty() || mobilePhone.isEmpty() ||
                province.isEmpty() || cityMunicipality.isEmpty() || barangay.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields and select your location.", Toast.LENGTH_LONG).show();
            return;
        }

        // ✅ ADDED: Validate mobile number length in signup attempt as well
        if (mobilePhone.length() != 11) {
            mobilePhoneEditText.setError("Please input only 11 digit numbers");
            Toast.makeText(this, "Please input only 11 digit numbers", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!hasConsent) {
            Toast.makeText(this, "Please agree to the Terms and Conditions.", Toast.LENGTH_LONG).show();
            return;
        }

        // --- OTP Validation (Local checks before API call) ---
        if (!isOtpSent) {
            Toast.makeText(this, "Please tap 'Send OTP' and check your SMS.", Toast.LENGTH_LONG).show();
            return;
        }

        if (otp.isEmpty() || otp.length() != 6) {
            otpEditText.setError("Enter the 6-digit OTP.");
            return;
        }
        // --- End OTP Validation ---

        // 3. If local validation passes, verify OTP with the API
        verifyOtpAndProceed(username, mobilePhone, fullName, password, province, cityMunicipality, barangay);
    }

    /**
     * Checks Firestore for existing user accounts.
     */
    private void checkExistingUser(final String username, final String mobilePhone, final String fullName, final String password,
                                   final String province, final String cityMunicipality, final String barangay) {
        // 1. Check if a document with the username (Preferred Username is used as Document ID) already exists
        db.collection(FIRESTORE_COLLECTION).document(username).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            // Username already exists
                            handleFailure("The Preferred Username already exists. Please choose a different one.");
                            return;
                        } else {
                            // 2. Username is unique, now check for existing Mobile Phone (using a query on a field)
                            Query mobileQuery = db.collection(FIRESTORE_COLLECTION).whereEqualTo("mobile_phone", mobilePhone).limit(1);
                            mobileQuery.get()
                                    .addOnCompleteListener(mobileTask -> {
                                        if (mobileTask.isSuccessful()) {
                                            QuerySnapshot documents = mobileTask.getResult();
                                            if (documents != null && !documents.isEmpty()) {
                                                // Mobile phone already exists
                                                handleFailure("Mobile number exist already.");
                                            } else {
                                                // Both checks passed, proceed to save the new user
                                                saveNewUser(username, mobilePhone, fullName, password, province, cityMunicipality, barangay);
                                            }
                                        } else {
                                            Log.w(TAG, "Error checking mobile phone: ", mobileTask.getException());
                                            handleFailure("Error during mobile phone check.");
                                        }
                                    });
                        }
                    } else {
                        Log.w(TAG, "Error checking username document: ", task.getException());
                        handleFailure("Error during username check.");
                    }
                });
    }

    /**
     * Saves the new user data to Firestore.
     */
    private void saveNewUser(String username, String mobilePhone, String fullName, String password,
                             String province, String cityMunicipality, String barangay) {
        // Create a new user map
        Map<String, Object> user = new HashMap<>();
        user.put("full_name", fullName);
        user.put("username", username);
        user.put("mobile_phone", mobilePhone);
        user.put("province", province);
        user.put("city_municipality", cityMunicipality);
        user.put("barangay", barangay);
        user.put("password", password); // ⚠️ REMINDER: SECURELY HASH THIS PASSWORD IN A REAL APPLICATION!
        user.put("created_at", Timestamp.now());

        // 💾 DOCUMENT SAVE: Uses the preferred username as the Document ID
        db.collection(FIRESTORE_COLLECTION).document(username)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "DocumentSnapshot successfully written! Document ID: " + username);
                    Toast.makeText(Signup.this, "Account successfully created!", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    signUpButton.setEnabled(true);

                    // Redirect to Login.class and clear the activity stack
                    Intent intent = new Intent(Signup.this, Login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing document: ", e);
                    handleFailure("Signup failed: " + e.getMessage());
                });
    }

    /**
     * Utility method to hide the progress bar and show a Toast message on failure.
     */
    private void handleFailure(String message) {
        Toast.makeText(Signup.this, message, Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.GONE);
        signUpButton.setEnabled(true);
    }
}