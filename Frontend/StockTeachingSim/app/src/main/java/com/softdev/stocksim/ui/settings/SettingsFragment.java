package com.softdev.stocksim.ui.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.MainActivity;
import com.softdev.stocksim.auth.AuthActivity;
import com.softdev.stocksim.auth.InputValidator;
import com.softdev.stocksim.ui.BaseFragment;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.data.UserPreferences;
import com.softdev.stocksim.api.VolleySingleton;

import java.util.Objects;

/**
 * Fragment for managing user settings and preferences.
 * Handles theme selection, contrast settings, and account management operations.
 *
 * @author Blake Nelson
 */
public class SettingsFragment extends BaseFragment {
    private static final String TAG = "SettingsFragment";

    private UserPreferences userPreferences;

    // Theme UI elements
    private RadioGroup themeRadioGroup;
    private RadioButton lightThemeRadio;
    private RadioButton darkThemeRadio;
    private RadioButton systemThemeRadio;

    private RadioGroup contrastRadioGroup;
    private RadioButton lowContrastRadio;
    private RadioButton mediumContrastRadio;
    private RadioButton highContrastRadio;

    private TextView usernameText;

    // Account UI elements
    private Button changeUsernameButton;
    private Button changePasswordButton;
    private Button deleteAccountButton;
    private Button signOutButton;

    /**
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userPreferences = UserPreferences.getInstance(this.requireContext());
    }

    /**
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return View
     */
    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    /**
     * Setups buttons and text views
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);

        //Get USERNAME & set text
        userPreferences = UserPreferences.getInstance(this.requireContext());
        usernameText.setText(userPreferences.getUsername());
        setupThemeControls();
        setupContrastControls();
        setupClickListeners();
        hideLoading();
    }

    /**
     * Initializes UI elements
     *
     */
    private void initializeViews(View view) {
        usernameText = view.findViewById(R.id.username_text);

        // Account UI elements
        changeUsernameButton = view.findViewById(R.id.change_username_button);
        changePasswordButton = view.findViewById(R.id.settings_change_password_button);
        deleteAccountButton = view.findViewById(R.id.settings_delete_account_button);
        signOutButton = view.findViewById(R.id.sign_out_button);

        // Theme UI elements
        themeRadioGroup = view.findViewById(R.id.theme_radio_group);
        systemThemeRadio = view.findViewById(R.id.system_theme_radio);
        lightThemeRadio = view.findViewById(R.id.light_theme_radio);
        darkThemeRadio = view.findViewById(R.id.dark_theme_radio);

        // Contrast UI elements
        contrastRadioGroup = view.findViewById(R.id.contrast_radio_group);
        lowContrastRadio = view.findViewById(R.id.low_contrast_radio);
        mediumContrastRadio = view.findViewById(R.id.medium_contrast_radio);
        highContrastRadio = view.findViewById(R.id.high_contrast_radio);
    }

    /**
     * Sets up theme controls and listeners.
     * Initializes radio buttons according to current theme setting
     * and configures listeners to handle theme changes.
     */
    private void setupThemeControls() {
        // Get current theme
        int currentTheme = userPreferences.getTheme();

        // Set radio button based on current theme
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            lightThemeRadio.setChecked(true);
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            darkThemeRadio.setChecked(true);
        } else {
            systemThemeRadio.setChecked(true);
        }

        // Set up radio button listener
        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int themeMode;
            if (checkedId == R.id.light_theme_radio) {
                themeMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.dark_theme_radio) {
                themeMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            userPreferences.setTheme(themeMode);

            // Apply both theme and current contrast
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).applyThemeAndContrast(
                        themeMode,
                        userPreferences.getContrast()
                );
            }

        });
    }

    private void setupContrastControls() {
        int currentContrast = userPreferences.getContrast();

        // Set radio button based on current contrast
        if (currentContrast == R.style.ThemeOverlay_AppTheme_HighContrast) {
            highContrastRadio.setChecked(true);
        } else if (currentContrast == R.style.ThemeOverlay_AppTheme_MediumContrast) {
            mediumContrastRadio.setChecked(true);
        } else {
            lowContrastRadio.setChecked(true);
        }

        // Set up radio button listener
        contrastRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int contrastMode;
            if (checkedId == R.id.low_contrast_radio) {
                contrastMode = R.style.Theme_StockTeachingSim;
            } else if (checkedId == R.id.high_contrast_radio) {
                contrastMode = R.style.ThemeOverlay_AppTheme_HighContrast;
            } else {
                contrastMode = R.style.ThemeOverlay_AppTheme_MediumContrast;
            }

            // Save the contrast preference
            userPreferences.setContrast(contrastMode);

            // Apply contrast only (pass null for themeMode)
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).applyThemeAndContrast(
                        null,
                        contrastMode
                );
            }
        });
    }

    /**
     * Sets up contrast controls and listeners.
     */
    private void setupClickListeners() {
        changeUsernameButton.setOnClickListener(v -> showChangeUsernameDialog());
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        deleteAccountButton.setOnClickListener(v -> showDeleteConfirmDialog());
        signOutButton.setOnClickListener(v -> signOutRequest());
    }

    /**
     * Shows the change username dialog
     */
    private void showChangeUsernameDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_username, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Username")
                .setView(dialogView)
                .create();

        TextInputLayout newUsernameLayout = dialogView.findViewById(R.id.new_username_layout);
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.password_layout);

        TextInputEditText newUsernameEdit = dialogView.findViewById(R.id.new_username_edit);
        TextInputEditText passwordEdit = dialogView.findViewById(R.id.password_edit);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button changeUsernameButton = dialogView.findViewById(R.id.change_username_button);


        if (newUsernameEdit == null || newUsernameEdit.getText() == null) {
            Log.e(TAG, "Username edit text not found");
            return;
        }
        if (passwordEdit == null || passwordEdit.getText() == null) {
            Log.e(TAG, "Password edit text not found");
            return;
        }

        newUsernameEdit.setText(userPreferences.getUsername());

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        changeUsernameButton.setOnClickListener(v -> {
            boolean isValidInput = true;
            if (passwordEdit.getText().toString().isEmpty()) {
                passwordLayout.setError("Password is required");
                passwordLayout.clearFocus();

                passwordEdit.setOnFocusChangeListener( (view, hasFocus) ->{
                    if (hasFocus) {
                        passwordLayout.setError(null);
                    }
                });
                isValidInput = false;
            }
            if (!InputValidator.isValidUsername(newUsernameEdit, newUsernameLayout)) {
                isValidInput = false;
            }

            if (isValidInput) {
                showConfirmChangeUsernameDialog(newUsernameLayout, passwordLayout, dialog);
            }
        });
        Log.d(TAG, "Showing dialog");
        dialog.show();
    }

    /**
     * Shows the confirm change username dialog
     */
    private void showConfirmChangeUsernameDialog(TextInputLayout newUsernameLayout, TextInputLayout passwordLayout, AlertDialog usernameDialog) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Warning")
                .setMessage("Changing your username will sign you out. Do you want to continue?")
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    usernameDialog.dismiss();
                })
                .setPositiveButton("Continue", (dialog, which) -> {
                    dialog.dismiss();
                    changeUsernameRequest(newUsernameLayout, passwordLayout, usernameDialog);
                })
                .show();
    }

    /**
     * Shows the change password dialog
     */
    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .create();

        TextInputLayout oldPasswordLayout = dialogView.findViewById(R.id.old_password_layout);
        TextInputLayout newPasswordLayout = dialogView.findViewById(R.id.new_password_layout);
        TextInputEditText oldPasswordEdit = dialogView.findViewById(R.id.old_password_edit);
        TextInputEditText newPasswordEdit = dialogView.findViewById(R.id.new_password_edit);
        Button forgotPasswordButton = dialogView.findViewById(R.id.forgot_password_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button changePasswordButton = dialogView.findViewById(R.id.change_password_button);

        // Setup button listeners
        forgotPasswordButton.setOnClickListener(v -> {
            dialog.dismiss();

            // Navigate to forgot password
            navController.navigate(R.id.action_settingsFragment_to_forgotPasswordFragment);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        changePasswordButton.setOnClickListener(v -> {
            if (oldPasswordEdit == null || oldPasswordEdit.getText() == null) {
                Toast.makeText(requireContext(), "Current password is required", Toast.LENGTH_SHORT).show();
            } else if (newPasswordEdit == null || newPasswordEdit.getText() == null) {
                Toast.makeText(requireContext(), "New password is required", Toast.LENGTH_SHORT).show();
            } else {
                boolean isValidInput = true;

                if (oldPasswordEdit.getText().toString().trim().isEmpty()) {
                    oldPasswordLayout.setError("Current password is required");
                    oldPasswordLayout.requestFocus();
                    isValidInput = false;
                }
                if (!InputValidator.isValidPassword(newPasswordEdit, newPasswordLayout)) {
                    isValidInput = false;
                }

                if (isValidInput) {
                    String oldPassword = Objects.requireNonNull(oldPasswordEdit.getText()).toString().trim();
                    String newPassword = Objects.requireNonNull(newPasswordEdit.getText()).toString().trim();
                    changePasswordRequest(oldPassword, newPassword, oldPasswordLayout, dialog);
                }
            }
        });

        dialog.show();
    }

    /**
     * Shows the delete account dialog
     */
    private void showDeleteConfirmDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_account, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? You will not be able to recover your account once it is deleted.")
                .setView(dialogView)
                .create();

        TextInputLayout passwordLayout = dialogView.findViewById(R.id.password_layout);
        TextInputEditText passwordEdit = dialogView.findViewById(R.id.password_edit);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);
        Button deleteAccountButton = dialogView.findViewById(R.id.delete_account_button);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        deleteAccountButton.setOnClickListener(v -> {
            if (passwordEdit == null || passwordEdit.getText() == null) {
                Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show();
            } else {
                String password = passwordEdit.getText().toString().trim();
                deleteRequest(password, passwordLayout, dialog);
            }
        });
        dialog.show();
    }

    /**
     * Sends a PUT request to the server to change the username
     */
    private void changeUsernameRequest(TextInputLayout newUsernameLayout, TextInputLayout passwordLayout, AlertDialog dialog) {
        String password = Objects.requireNonNull(passwordLayout.getEditText()).getText().toString().trim();
        String newUsername = Objects.requireNonNull(newUsernameLayout.getEditText()).getText().toString().trim();

        String url = AppConfig.BASE_URL + "/user/changeUsername" +
                "?newUsername=" + newUsername +
                "&password=" + password;

        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show();
                    signOutRequest();
                },
                error -> {
                    String message;
                    if (error.networkResponse != null) {
                        switch (error.networkResponse.statusCode) {
                            case 403:
                                message = "Old password is incorrect";
                                passwordLayout.setError(message);
                                passwordLayout.getEditText().requestFocus();
                                break;
                            case 404:
                                showError("User not found");
                                break;
                            case 409:
                                message = "Username already exists";
                                newUsernameLayout.setError(message);
                                newUsernameLayout.getEditText().requestFocus();
                                break;
                            default:
                                dialog.dismiss();
                                showError("Failed to change password");
                                break;
                        }
                    } else {
                        showError("Network error. Please try again.");
                    }
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Saves the password and updates it with the backend
     */
    private void changePasswordRequest(String oldPassword, String newPassword, TextInputLayout oldPasswordLayout, AlertDialog dialog) {

        String url = AppConfig.BASE_URL + "/user/change-password" +
                "?oldPassword=" + oldPassword +
                "&newPassword=" + newPassword;

        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                },
                error -> {
                    String message;
                    if (error.networkResponse != null) {
                        switch (error.networkResponse.statusCode) {
                            case 401:
                                message = "Old password is incorrect";
                                oldPasswordLayout.setError(message);
                                oldPasswordLayout.requestFocus();
                                break;
                            case 404:
                                dialog.dismiss();
                                showError("User not found");
                                break;
                            default:
                                dialog.dismiss();
                                showError("Failed to change password");
                                break;
                        }
                    } else {
                        showError("Network error. Please try again.");
                    }
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Sends a DELETE request to the server to delete the user
     */
    private void deleteRequest(String providedPassword, TextInputLayout passwordLayout, AlertDialog dialog) {
        StringRequest stringRequest = new StringRequest(
                Request.Method.DELETE,
                AppConfig.BASE_URL + "/user/delete?password=" + providedPassword,
                response -> {
                    Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    userPreferences.logout();
                    navigateToAuthActivity();
                },
                error -> {
                    if (error.networkResponse != null){
                        if (error.networkResponse.statusCode == 403) {
                            passwordLayout.setError("Incorrect password");
                            passwordLayout.requestFocus();
                        } else {
                            Log.e(TAG, "Failed to delete account", error);
                            showError("Failed to delete account");
                        }
                    } else {
                        Log.e(TAG, "Failed to delete account", error);
                        showError("Failed to delete account");
                    }
                }

        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest);
    }

    /**
     * Signs the user out with a GET request
     */
    private void signOutRequest() {
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                AppConfig.BASE_URL + "/auth/logout",
                response -> {
                    Toast.makeText(requireContext(), response, Toast.LENGTH_SHORT).show();
                    userPreferences.logout();
                    navigateToAuthActivity();
                },
                error -> showError("Failed to sign out")
        );
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest);
    }

    /**
     * Navigates to Welcome fragment
     */
    private void navigateToAuthActivity() {
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        // Clear all previous activities from the stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}