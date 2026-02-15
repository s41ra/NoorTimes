package com.example.islamic;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent; // Import for navigating between activities
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.TextView; // Import for the new TextView

public class MainActivity extends AppCompatActivity {

    // Define the delay time for the splash screen in milliseconds (3 seconds)
    private static final int SPLASH_TIME_OUT = 3000;

    private ProgressBar loadingSpinner;
    private ImageView logoImage;
    private TextView appNameText; // Declared the new TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sets the content view to your provided XML layout
        setContentView(R.layout.activity_main);

        // 1. Find the views defined in the XML layout
        loadingSpinner = findViewById(R.id.loadingSpinner);
        logoImage = findViewById(R.id.logoImage);
        appNameText = findViewById(R.id.appNameText); // Initialized the new TextView

        // 2. Use a Handler to delay the execution of code
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // This code will execute after SPLASH_TIME_OUT milliseconds

                // 3. Hide the loading spinner (optional, but good practice)
                if (loadingSpinner != null) {
                    loadingSpinner.setVisibility(View.GONE);
                }

                // Note: The appNameText is not being manipulated here, as it's static.

                // 4. Create an Intent to navigate from MainActivity to Login.class
                Intent i = new Intent(MainActivity.this, Login.class);

                // 5. Start the new activity
                startActivity(i);

                // 6. Finish the current activity (Splash Screen)
                // This prevents the user from pressing the back button to return to the splash screen.
                finish();

                Log.d("MainActivity", "Loading complete. Redirecting to Login.");
            }
        }, SPLASH_TIME_OUT);
    }
}