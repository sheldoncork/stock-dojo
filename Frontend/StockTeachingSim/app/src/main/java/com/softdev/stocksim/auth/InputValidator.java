package com.softdev.stocksim.auth;

import android.content.Context;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.utils.AppConfig;

/**
 *  Utility class providing input validation for user authentication fields.
 *  Handles validation of email addresses, usernames, passwords, and user types
 *
 * @author Blake Nelson
 */
public class InputValidator {

    /**
     * Validates an email address.
     */
    public static boolean isValidEmail(TextInputEditText emailEdit, TextInputLayout emailLayout) {
        if (emailEdit == null || emailEdit.getText() == null) {
            return false;
        }

        String email = emailEdit.getText().toString();

        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            emailLayout.requestFocus();
            return false;
        }

        if (AppConfig.DEBUG_MODE) {
            emailLayout.setError(null);
            return true;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email format");
            emailLayout.requestFocus();
            return false;
        }

        emailLayout.setError(null);
        return true;
    }

    /**
     * Validates a username input.
     */
    public static boolean isValidUsername(TextInputEditText usernameEdit, TextInputLayout usernameLayout) {
        if (usernameEdit == null || usernameEdit.getText() == null) {
            return false;
        }

        String username = usernameEdit.getText().toString();

        if (username.isEmpty()) {
            usernameLayout.setError("Username is required");
            usernameLayout.requestFocus();
            return false;
        }

        if (AppConfig.DEBUG_MODE) {
            usernameLayout.setError(null);
            return true;
        }

        if (username.length() < 3) {
            usernameLayout.setError("Username must be 3 characters or longer");
            usernameLayout.requestFocus();
            return false;
        }

        if (!username.matches("^[a-zA-Z0-9]+$")) {
            usernameLayout.setError("Username can only contain letters and numbers");
            usernameLayout.requestFocus();
            return false;
        }

        usernameLayout.setError(null);
        return true;
    }

/**
 * Validates a password input against security requirements.
 */
 public static boolean isValidPassword(TextInputEditText passwordEdit, TextInputLayout passwordLayout) {
        if (passwordEdit == null || passwordEdit.getText() == null) {
            return false;
        }

        String password = passwordEdit.getText().toString();

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            passwordLayout.requestFocus();
            return false;
        }

        if (AppConfig.DEBUG_MODE) {
            passwordLayout.setError(null);
            return true;
        }

        if (password.length() < 8) {
            passwordLayout.setError("Must be 8 characters or longer");
            passwordLayout.requestFocus();
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            passwordLayout.setError("Must contain at least one uppercase letter");
            passwordLayout.requestFocus();
            return false;
        }

        if (!password.matches(".*[a-z].*")) {
            passwordLayout.setError("Must contain at least one lowercase letter");
            passwordLayout.requestFocus();
            return false;
        }

        if (!password.matches(".*\\d.*")) {
            passwordLayout.setError("Must contain at least one number");
            passwordLayout.requestFocus();
            return false;
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            passwordLayout.setError("Must contain at least one special character");
            passwordLayout.requestFocus();
            return false;
        }

        passwordLayout.setError(null);
        return true;
    }

    /**
     * Validates the user type selection.
     */
    protected static boolean isValidUserType(String userType, Context context) {
        if (userType == null || userType.isEmpty()) {
            showToast("User type is required", context);
            return false;
        }

        if (!userType.equals("STUDENT") && !userType.equals("TEACHER") && !userType.equals("STANDARD")) {
            showToast("invalid user type", context);
            return false;
        }

        return true;
    }

    /**
     * Helper method to display toast messages.
     */
    private static void showToast(String message, Context context) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
