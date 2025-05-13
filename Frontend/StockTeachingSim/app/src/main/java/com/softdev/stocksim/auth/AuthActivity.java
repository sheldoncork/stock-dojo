package com.softdev.stocksim.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.MainActivity;
import com.softdev.stocksim.R;
import com.softdev.stocksim.data.UserPreferences;
import com.softdev.stocksim.api.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/**
 * The AuthActivity class is responsible for handling the authentication flow.
 * Entry point for user login/registration
 * Handles both Login/Registration and automatic login attempt when
 * opening app after already logging in before
 *
 * @author Blake Nelson
 */
public class AuthActivity extends AppCompatActivity {
    private static final String TAG = "AuthActivity";

    private UserPreferences userPreferences;
    private boolean isAuthComplete = false;
    NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Initialize preferences and apply theme before splash screen
        userPreferences = UserPreferences.getInstance(this);

        // Apply user theme
        AppCompatDelegate.setDefaultNightMode(userPreferences.getTheme());

        // Apply user contrast
        getTheme().applyStyle(userPreferences.getContrast(), true);

        // Initialize splash screen (wait screen)
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        setBackPress();

        setContentView(R.layout.activity_auth);

        // Keep splash screen visible until auth check completes
        splashScreen.setKeepOnScreenCondition(() -> !isAuthComplete);

        // Initialize navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.auth_nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // Check if user is logged in on frontend
        if (userPreferences.isLoggedIn()) {
            // Attempt auto-login with stored credentials
            attemptAutoLogin();
        } else {
            isAuthComplete = true;
        }
    }

    /**
     * Set up back press handling for navigation.
     * User not allowed to go back to previous screens before login or registration.
     */
    private void setBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController.getCurrentDestination() == null){
                    Log.d(TAG, "Tried to access navController.getCurrentDestination() but was null");
                    return;
                }
                // Only allow back navigation if not on login or registration screens
                if (navController.getCurrentDestination().getId() != R.id.loginFragment &&
                        navController.getCurrentDestination().getId() != R.id.registrationFragment) {
                    navController.navigateUp();
                }
            }
        });
    }

    /**
     * Attempt to automatically log in the user with stored credentials.
     */
    private void attemptAutoLogin() {
        String username = userPreferences.getUsername();
        String password = userPreferences.getPassword();

        // Check if username and password are stored
        if (username == null || password == null) {
            Log.d(TAG, "No username or password found in preferences");
            userPreferences.logout();
            isAuthComplete = true;
            return;
        }

        String url = AppConfig.BASE_URL
                + "/auth/login?username=" + username
                + "&password=" + password;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                this::handleLoginResponse,
                this::handleLoginError) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Handle the response from the login request.
     * @param response The response from the login request.
     */
    private void handleLoginResponse(JSONObject response) {
        try {
            String responseUsername = response.getString("username");
            String responseRole = response.getString("role");
            String responseEmail = response.getString("email");

            // Check if role is valid
            if (responseRole.equals(AppConfig.UserType.STUDENT) || responseRole.equals(AppConfig.UserType.TEACHER) || responseRole.equals(AppConfig.UserType.STANDARD)) {
                // Verify response data matches stored preferences
                if (responseUsername.equals(userPreferences.getUsername()) &&
                        responseRole.equals(userPreferences.getUserType()) &&
                        responseEmail.equals(userPreferences.getEmail())) {
                    Log.d(TAG, "Login successful");
                    navigateToMain();
                } else {
                // User type mismatch
                handleInvalidLogin("User type mismatch");
                }
            } else {
                // Invalid response
                handleInvalidLogin("Invalid response from server: " +response);
            }
        } catch (JSONException e) {
            handleInvalidLogin("Error parsing login response");
        }
    }

    /**
     * Handle the error from the login request.
     * @param error The error from the login request.
     */
    private void handleLoginError(VolleyError error) {
        NetworkResponse response = error.networkResponse;
        String message;
        if (response != null && response.statusCode == 401) {
            message = new String(response.data).trim();
        } else {
            Log.e(TAG, "Login error", error);
            message = "Network error";
        }
        handleInvalidLogin(message);
    }

    private void handleInvalidLogin(String message){
        Log.d(TAG, "Invalid login: " + message);
        userPreferences.logout();
        isAuthComplete = true;
    }

    /**
     * Navigate to MainActivity after successful authentication.
     * Can be called from Fragments after successful login/registration.
     */
    public void navigateToMain() {
        Log.d(TAG, "Navigating to MainActivity");
        isAuthComplete = true;
        Intent intent = new Intent(this, MainActivity.class);
        // Clear back stack to prevent navigation back to auth screens
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}