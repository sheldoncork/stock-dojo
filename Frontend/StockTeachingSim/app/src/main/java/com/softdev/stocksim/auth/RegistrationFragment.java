package com.softdev.stocksim.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.BaseLoadingFragment;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.data.UserPreferences;
import com.softdev.stocksim.api.VolleySingleton;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fragment responsible for handling new user registration.
 *
 * @author Blake Nelson
 */
public class RegistrationFragment extends BaseLoadingFragment {
    private static final String TAG = "RegistrationFragment";
    private static final String REGISTRATION_TAG = "Registration";
    private static final String LOGIN_TAG = "Login";

    TextInputLayout emailLayout;
    TextInputLayout usernameLayout;
    TextInputLayout passwordLayout;
    TextInputLayout confirmPasswordLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private Button loginButton, registerButton;
    private RadioGroup radioGroup;

    private String email, username, password;
    private String role = AppConfig.UserType.STANDARD; //STANDARD is checked so set it to default

    private UserPreferences userPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userPreferences = UserPreferences.getInstance(requireContext());
    }

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @androidx.annotation.Nullable ViewGroup container, @androidx.annotation.Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupClickListeners(view);
        hideLoading();
    }

    /**
     * Initializes view components.
     */
    private void initializeViews(View view) {
        emailLayout = view.findViewById(R.id.registration_email_layout);
        usernameLayout = view.findViewById(R.id.registration_username_layout);
        passwordLayout = view.findViewById(R.id.registration_password_layout);
        confirmPasswordLayout = view.findViewById(R.id.registration_confirm_password_layout);
        emailEditText = view.findViewById(R.id.registration_email_input);
        usernameEditText = view.findViewById(R.id.registration_username_input);
        passwordEditText = view.findViewById(R.id.registration_password_input);
        confirmPasswordEditText = view.findViewById(R.id.registration_confirm_password_input);
        radioGroup = view.findViewById(R.id.radio_user_type);

        registerButton = view.findViewById(R.id.registration_register_register_btn);
        loginButton = view.findViewById(R.id.registration_register_login_btn);
    }

    /**
     * Sets up click listeners for buttons.
     */
    private void setupClickListeners(View view) {
        loginButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_registrationFragment_to_loginFragment);
        });

        registerButton.setOnClickListener(v -> validateAndRegister());

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = view.findViewById(checkedId);
            role = radioButton.getText().toString().trim().toUpperCase();
        });
    }

    private void validateAndRegister() {
        boolean extrasVerified = true;

        if (confirmPasswordEditText.getText() == null) {
            confirmPasswordLayout.setError("Confirm password is required");
            extrasVerified = false;
        }
        if (role == null) {
            Toast.makeText(requireContext(), "User type is required", Toast.LENGTH_SHORT).show();
            extrasVerified = false;
        }

        boolean isVerified = InputValidator.isValidPassword(passwordEditText, passwordLayout) &&
                InputValidator.isValidUsername(usernameEditText, usernameLayout) &&
                InputValidator.isValidEmail(emailEditText, emailLayout) &&
                InputValidator.isValidUserType(role, requireContext()) &&
                !confirmPasswordEditText.getText().toString().isEmpty() &&
                extrasVerified;

        // Validate email, username, password, and user type
        if (isVerified) {
            // Trim and convert email and username to lowercase
            emailEditText.setText(Objects.requireNonNull(emailEditText.getText()).toString().trim());
            usernameEditText.setText(Objects.requireNonNull(usernameEditText.getText()).toString().trim());
            passwordEditText.setText(Objects.requireNonNull(passwordEditText.getText()).toString().trim());
            confirmPasswordEditText.setText(confirmPasswordEditText.getText().toString().trim());

            // Check if passwords match
            if (passwordEditText.getText().toString().equals(confirmPasswordEditText.getText().toString())) {

                // All fields are valid, proceed with registration
                email = emailEditText.getText().toString();
                username = usernameEditText.getText().toString();
                password = passwordEditText.getText().toString();
                sendRegistrationRequest();
            } else {
                passwordLayout.setError(" ");
                confirmPasswordLayout.setError("Passwords do not match");
                confirmPasswordEditText.clearFocus();
                passwordEditText.clearFocus();

                // Makes it so if it is error user can still see password after editing again
                confirmPasswordEditText.setOnFocusChangeListener( (v, hasFocus) ->{
                    if (hasFocus){
                        passwordLayout.setError(null);
                        confirmPasswordLayout.setError(null);
                    }
                });

                passwordEditText.setOnFocusChangeListener( (v, hasFocus) ->{
                    if (hasFocus){
                        passwordLayout.setError(null);
                        confirmPasswordLayout.setError(null);
                    }
                });

            }
        } else {
            if (confirmPasswordEditText.getText().toString().isEmpty()) {
                confirmPasswordLayout.setError("Confirm password is required");
                confirmPasswordLayout.requestFocus();
            }
            if (!InputValidator.isValidPassword(passwordEditText, passwordLayout)) {
                confirmPasswordLayout.setError(" ");
            }
//            // Reverse input so top invalid input shows is focused first
            InputValidator.isValidUsername(usernameEditText, usernameLayout);
            InputValidator.isValidEmail(emailEditText, emailLayout);
            InputValidator.isValidUserType(role, requireContext());
        }
    }

    /**
     * Sends registration request to the server.
     */
    private void sendRegistrationRequest() {
        showLoading();
        JSONObject jsonBody = new JSONObject();

        try {
            jsonBody.put("username", username.toLowerCase());
            jsonBody.put("password", password);
            jsonBody.put("role", role);
            jsonBody.put("email", email);

            String url = AppConfig.BASE_URL + "/auth/register";
            Log.d(TAG, "Preparing registration request to: " + url);
            Log.d(TAG, "Request body: " + jsonBody);

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    this::handleRegistrationResponse,
                    this::handleRegistrationError) {

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    return jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                }

                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Accept", "text/plain");
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            Log.d(TAG, "Adding request to queue");
            VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest);
            Log.d(TAG, "Request added to queue");

        } catch (JSONException e) {
            hideLoading();
            Log.e(TAG, "Error creating JSON body", e);
            showError("Error creating registration request", REGISTRATION_TAG);
        }
    }

    /**
     * Handles the server's response to a registration request.
     */
    private void handleRegistrationResponse(String response) {
        response = response.trim();
        Log.d(TAG, "Registration response: " + response);
        hideLoading();
        switch (response) {
            case "Username is already in use":
                usernameEditText.setError("Username is already in use");
                usernameEditText.requestFocus();
                break;

            case "Email is already in use":
                emailEditText.setError("Email is already in use");
                emailEditText.requestFocus();
                break;

            case "User registered successfully":
                Log.d(TAG, "Registration successful. Attempting to Login...");
                Toast.makeText(requireContext(), "Registration Successful. Attempting to Login....", Toast.LENGTH_LONG).show();
                sendLoginRequest();
                break;
            default:
                Log.w(TAG, "Unexpected registration response: " + response);
                showError("Registration failed", REGISTRATION_TAG);
                break;
        }
    }

    /**
     * Handles errors that occur during the registration process.
     *
     * @param error VolleyError containing error details
     */
    private void handleRegistrationError(VolleyError error) {
        NetworkResponse networkResponse = error.networkResponse;
        if (networkResponse != null && networkResponse.data != null) {
            String errorMessage = new String(networkResponse.data, StandardCharsets.UTF_8);
            Log.d(TAG, "Status code: " + networkResponse.statusCode);
            Log.e(TAG, "Registration error: " + errorMessage);
            if (networkResponse.statusCode == 400) {
                showError(errorMessage, REGISTRATION_TAG); // Show the specific error message from the server
            } else {
                showError("Registration failed: " + errorMessage, REGISTRATION_TAG);
            }
        } else {
            Log.e(TAG, "Registration error", error);
            showError("Network error during registration", REGISTRATION_TAG);
        }
    }


    /**
     * Sends a login request to the server.
     */
    private void sendLoginRequest() {
        showLoading();
        String url = AppConfig.BASE_URL + "/auth/login?username=" + username + "&password=" + password;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                this::handleLoginResponse,
                this::handleLoginError) {
            @Override
            public String getBodyContentType() {
                return "text/plain; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                return url.getBytes(StandardCharsets.UTF_8);
            }
        };
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Handles the server's response to a login request.
     */
    private void handleLoginResponse(JSONObject response) {
        try {
            String responseUsername = response.getString("username");
            String responseRole = response.getString("role");
            String responseEmail = response.getString("email");

            if (responseRole.equals(AppConfig.UserType.STUDENT) || responseRole.equals(AppConfig.UserType.TEACHER) || responseRole.equals(AppConfig.UserType.STANDARD)) {
                if (!responseRole.equals(role)) {
                    Log.e(TAG, "Error: Role mismatch - Registered as: " + role +
                            " but received: " + responseRole);
                    showError("Invalid information for user", LOGIN_TAG);
                    deleteRequest();
                    hideLoading();
                } else {
                    userPreferences.saveUserData(responseUsername, password, responseRole, responseEmail);
                    ((AuthActivity) requireActivity()).navigateToMain();
                }
            } else {
                hideLoading();
                Log.d(TAG, "Error: Invalid role received (status 200): " + response);
                showError("Invalid information for user", LOGIN_TAG);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing login response", e);
            showError("Error processing login response", LOGIN_TAG);
            hideLoading();
        }
    }

    /**
     * Handles errors that occur during the login process.
     */
    private void handleLoginError(VolleyError error) {
        hideLoading();
        NetworkResponse response = error.networkResponse;
        if (response != null && response.statusCode == 401) {
            String responseBody = new String(response.data).trim();
            if (responseBody.equals("Authentication failed")) {
                showError("Invalid Login", LOGIN_TAG);
            } else {
                VolleyLog.d("LoginFragment", "Error Unexpected Response (status 401): " + responseBody);
            }
        } else {
            if (response != null) {
                VolleyLog.d("LoginFragment", "Error Unhandled response status (status" + response.statusCode + "): " + error.getMessage());
            } else {
                VolleyLog.d("LoginFragment", "Error (response is null): " + error.getMessage());
            }
        }
    }

    private void showError(String message, String functionCall) {
        Log.d(TAG, message);
        if (getView() != null) {
            if (functionCall.equals(REGISTRATION_TAG)) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> sendRegistrationRequest())
                        .show();
            } else if (functionCall.equals(LOGIN_TAG)) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> sendLoginRequest())
                        .show();
            }

        } else {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sends a DELETE request to the server to delete the user
     */
    private void deleteRequest() {
        StringRequest stringRequest = new StringRequest(
                Request.Method.DELETE,
                AppConfig.BASE_URL + "/user/delete",
                response -> {
                },
                error -> Log.d(TAG, "Error deleting account after registering a user with incorrect data (Most likely role from register and login don't match): " + error.getMessage())
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest);
    }
}