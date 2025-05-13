package com.softdev.stocksim.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.BaseLoadingFragment;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.data.UserPreferences;
import com.softdev.stocksim.api.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Fragment responsible for handling user login functionality.
 *
 * @author Blake Nelson
 */
public class LoginFragment extends BaseLoadingFragment {
    private static final String TAG = "LoginFragment";

    TextInputLayout usernameLayout;
    TextInputLayout passwordLayout;
    private TextInputEditText usernameEdit;
    private TextInputEditText passwordEdit;
    private Button loginButton;
    private Button signupButton;
    private Button forgotPasswordButton;
    private UserPreferences userPreferences;
    private String password;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userPreferences = UserPreferences.getInstance(requireContext());
    }

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupClickListeners();
        hideLoading();
    }

    /**
     * Initializes view components.
     */
    private void initializeViews(View view) {
        usernameLayout = view.findViewById(R.id.login_username_layout);
        passwordLayout = view.findViewById(R.id.login_password_layout);
        usernameEdit = view.findViewById(R.id.login_username_input);
        passwordEdit = view.findViewById(R.id.login_password_input);
        loginButton = view.findViewById(R.id.login_login_btn);
        signupButton = view.findViewById(R.id.login_signup_btn);
        forgotPasswordButton = view.findViewById(R.id.forgot_password_button);
    }

    /**
     * Sets up click listeners for buttons.
     */
    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        signupButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_loginFragment_to_registrationFragment);
        });

        forgotPasswordButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_loginFragment_to_forgotPasswordFragment);
        });
    }

    /**
     * Attempts to log in the user.
     */
    private void attemptLogin() {
        showLoading();

        boolean inputNotNull = true;

        if (passwordEdit.getText() == null) {
            passwordLayout.setError("Password is required");
            passwordEdit.requestFocus();
            Log.d("LoginFragment", "Password edit is null");
            inputNotNull = false;
        }

        if (usernameEdit.getText() == null) {
            usernameLayout.setError("Username is required");
            usernameEdit.requestFocus();
            Log.d("LoginFragment", "Username edit is null");
            inputNotNull = false;
        }

        if (!inputNotNull) {
            hideLoading();
            return;
        }

        Log.d("LoginFragment", "Attempting login");
        Log.d("LoginFragment", "Username: " + usernameEdit.getText().toString());
        Log.d("LoginFragment", "Password: " + passwordEdit.getText().toString());

        String username = usernameEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        boolean isValid = true;

        if (password.isEmpty()) {
            isValid = false;
            passwordLayout.setError("Password is required");
            passwordEdit.requestFocus();
        } else {
            Log.d("LoginFragment", "Password is: '" + password + "'");
            passwordLayout.setError(null);
        }

        if (username.isEmpty()) {
            isValid = false;
            usernameLayout.setError("Username is required");
            usernameEdit.requestFocus();
        } else {
            usernameLayout.setError(null);
        }

        if (isValid) {
            sendLoginRequest(username, password);
        } else {
            Log.d("LoginFragment", "Login failed");
            hideLoading();
        }
    }

    /**
     * Sends a login request to the server.
     */
    private void sendLoginRequest(String username, String password) {
        this.password = password;
        String url = AppConfig.BASE_URL + "/auth/login?username=" + username + "&password=" + password;

        Log.d("LoginFragment", "Sending login request to: " + url);
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
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Handles the server's response to a login request.
     */
    private void handleLoginResponse(JSONObject response) {
        Log.d("LoginFragment", "Login response received: " + response);

        try {
            String responseUsername = response.getString("username");
            String responseRole = response.getString("role");
            String responseEmail = response.getString("email");

            // Save user data to shared preferences if role is valid
            if (responseRole.equals(AppConfig.UserType.STUDENT) || responseRole.equals(AppConfig.UserType.TEACHER) || responseRole.equals(AppConfig.UserType.STANDARD)) {
                userPreferences.saveUserData(responseUsername, password, responseRole, responseEmail);
                AuthActivity activity = (AuthActivity) requireActivity();
                activity.navigateToMain();
            } else {
                Log.e(TAG, "Invalid role received: " + responseRole);
                VolleyLog.d("LoginFragment", "Error: Invalid role received (status 200): " + response);
                Toast.makeText(requireContext(), "Invalid role received", Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing login response", e);
            Toast.makeText(requireContext(), "Error processing login response", Toast.LENGTH_SHORT).show();
            hideLoading();
        }
    }

    /**
     * Handles errors that occur during the login process.
     */
    private void handleLoginError(VolleyError error) {
        password = null;
        NetworkResponse response = error.networkResponse;
        hideLoading();
        if (response != null && response.statusCode == 401) {
            String responseBody = new String(response.data, StandardCharsets.UTF_8).trim();
            if (responseBody.equals("Authentication failed")) {
                Toast.makeText(requireContext(), "Invalid Login", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Unexpected response (status 401): " + responseBody);
                Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (response != null) {
                Log.e(TAG, "Unhandled response status (status " + response.statusCode + "): " + error.getMessage());
            } else {
                Log.e(TAG, "Error (response is null): " + error.getMessage());
            }
            Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show();
        }
    }
}