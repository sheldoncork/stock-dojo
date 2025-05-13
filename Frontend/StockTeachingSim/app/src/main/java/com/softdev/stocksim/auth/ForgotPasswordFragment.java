package com.softdev.stocksim.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.BaseLoadingFragment;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.api.VolleySingleton;


/**
 * Fragment that handles the password recovery process.
 *
 * @author Blake Nelson
 */
public class ForgotPasswordFragment extends BaseLoadingFragment {
    private static final String TAG = "ForgotPasswordFragment";

    // Views
    private TextView title;
    private View initialLayout;
    private View resetLayout;

    private TextInputLayout emailInputLayout;
    private TextInputLayout codeInputLayout;
    private TextInputLayout newPasswordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;

    private TextInputEditText emailInput;
    private TextInputEditText codeInput;
    private TextInputEditText newPasswordInput;
    private TextInputEditText confirmPasswordInput;

    private Button sendCodeButton;
    private Button resendButton;
    private Button resetButton;

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater,
                                       @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {

         // Hide initial loading state
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        initializeListeners();
        parseArguments();
        hideLoading();
    }

    /**
     * Sets all views in the activity
     */
    private void initializeViews(View view) {
        title = view.findViewById(R.id.forgot_title);
        initialLayout = view.findViewById(R.id.forgot_initial_layout);
        resetLayout = view.findViewById(R.id.forgot_reset_layout);

        emailInputLayout = view.findViewById(R.id.forgot_email_layout);
        emailInput = view.findViewById(R.id.forgot_email_input);
        codeInputLayout = view.findViewById(R.id.forgot_code_layout);
        codeInput = view.findViewById(R.id.forgot_code_input);
        newPasswordInputLayout = view.findViewById(R.id.forgot_new_password_layout);
        newPasswordInput = view.findViewById(R.id.forgot_new_password_input);
        confirmPasswordInputLayout = view.findViewById(R.id.forgot_confirm_password_layout);
        confirmPasswordInput = view.findViewById(R.id.forgot_confirm_password_input);
        sendCodeButton = view.findViewById(R.id.forgot_send_code_button);
        resendButton = view.findViewById(R.id.forgot_resend_button);
        resetButton = view.findViewById(R.id.forgot_reset_button);

        // hide reset layout
        resetLayout.setVisibility(View.GONE);
    }

    /**
     * Sets all listeners in the activity
     */
    private void initializeListeners() {
        sendCodeButton.setOnClickListener(v -> handleSendCode());
        resendButton.setOnClickListener(v -> handleSendCode());
        resetButton.setOnClickListener(v -> handleResetPassword());
    }

    /**
     * Parses arguments passed to the fragment.
     */
    private void parseArguments() {
        ForgotPasswordFragmentArgs args = ForgotPasswordFragmentArgs.fromBundle(requireArguments());
        if (args.getIsLoggedIn()) {
            title.setVisibility(View.GONE);
        }
    }

    /**
     * Sends a verification code to the user's email.
     */
    private void handleSendCode() {
        if (emailInput.getText() == null || emailInput.getText().toString().isEmpty()) {
            emailInputLayout.setError("Email cannot be empty");
            emailInput.requestFocus();
            return;
        }

        String email = emailInput.getText().toString().toLowerCase().trim();

        showLoading();

        StringRequest request = new StringRequest(
                Request.Method.GET,
                AppConfig.BASE_URL + "/auth/forgot-password?email=" + email,
                response -> {
                    hideLoading();
                    if (response.equals("Email sent")) {
                        showResetLayout();
                        Toast.makeText(requireContext(), "Verification code sent to your email", Toast.LENGTH_SHORT).show();
                    } else {
                        if (response.equals("User not found")) {
                            emailInputLayout.setError("User not found");
                            emailInputLayout.requestFocus();
                        } else {
                            Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                error -> {
                    hideLoading();
                    Toast.makeText(requireContext(), "Error: Unable to send verification code", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error: Unable to send verification code", error);
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     *  Handles the password reset request.
     *  Validates all inputs and sends reset request to server.
     */
    private void handleResetPassword() {

        boolean isValid = true;

        if (confirmPasswordInput.getText() == null || confirmPasswordInput.getText().toString().trim().isEmpty()) {
            confirmPasswordInputLayout.setError("Please confirm your new password");
            confirmPasswordInput.requestFocus();
            isValid = false;
        }

        if (newPasswordInput.getText() == null || newPasswordInput.getText().toString().trim().isEmpty()) {
            newPasswordInputLayout.setError("Please enter a new password");
            newPasswordInput.requestFocus();
            isValid = false;
        }

        if (codeInput.getText() == null || codeInput.getText().toString().trim().isEmpty()) {
            codeInputLayout.setError("Please enter the verification code");
            codeInput.requestFocus();
            isValid = false;
        }

        if (!isValid) {
            hideLoading();
            return;

        } else {
            if (confirmPasswordInput.getText().toString().trim().equals(newPasswordInput.getText().toString().trim())) {
                confirmPasswordInputLayout.setError(null);
                newPasswordInputLayout.setError(null);
                codeInputLayout.setError(null);
            } else {
                confirmPasswordInputLayout.setError("Passwords do not match");
                newPasswordInput.setError("Passwords do not match");
                return;
            }
        }

        String code = codeInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString().trim();

        showLoading();
        StringRequest request = new StringRequest(
                Request.Method.POST,
                AppConfig.BASE_URL + "/auth/forgot-password?token=" + code + "&newPassword=" + newPassword,
                response -> {
                    hideLoading();
                    if (response.equals("Password updated successfully")) {
                        Toast.makeText(requireContext(), "Password successfully reset", Toast.LENGTH_SHORT).show();

                        // Go back to login or Settings fragment
                        Navigation.findNavController(requireView()).navigateUp();
                    } else {
                        Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    hideLoading();
                    if (error.networkResponse != null && error.networkResponse.statusCode == 403) {
                            codeInputLayout.setError("Invalid or expired token");
                            codeInputLayout.requestFocus();
                    } else {
                        Toast.makeText(requireContext(), "Error: Unable to reset password", Toast.LENGTH_LONG).show();

                    }
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Transitions UI from initial email input to reset password state.
     * Hides email input layout and shows reset password layout.
     */
    private void showResetLayout() {
        initialLayout.setVisibility(View.GONE);
        resetLayout.setVisibility(View.VISIBLE);
    }
}