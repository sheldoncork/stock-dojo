package com.softdev.stocksim.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.softdev.stocksim.R;

/**
 * The WelcomeFragment class represents the welcome screen of the application.
 *  It provides options for users to either log in to an existing account or create a new one.
 *
 * @author Blake Nelson
 */
public class WelcomeFragment extends Fragment {
    private Button startButton;
    private LinearLayout loginSignupLayout;
    private OnBackPressedCallback onBackPressedCallback;

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    /**
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the callback
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // If start button is not visible, show it and hide login layout
                if (startButton != null && startButton.getVisibility() != View.VISIBLE) {
                    startButton.setVisibility(View.VISIBLE);
                    loginSignupLayout.setVisibility(View.GONE);
                }
                // If start button is visible, do nothing (consume the back press)
            }
        };

        // Add the callback to the activity
        requireActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    /**
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
    }

    /**
     *  Initializes and configures all view components.
     */
    private void initializeViews(View view) {
        startButton = view.findViewById(R.id.welcome_start_btn);
        Button loginButton = view.findViewById(R.id.welcome_login_btn);
        Button signupButton = view.findViewById(R.id.welcome_signup_btn);
        loginSignupLayout = view.findViewById(R.id.welcome_login_signup_layout);

        startButton.setOnClickListener(v -> {
            startButton.setVisibility(View.GONE);
            loginSignupLayout.setVisibility(View.VISIBLE);
        });

        loginButton.setOnClickListener(v -> navigateToLogin(view));

        signupButton.setOnClickListener(v -> navigateToRegister(view));
    }

    /**
     * Navigates to the login screen when the login button is clicked.
     * Uses the NavController to navigate via the action defined in the navigation graph.
     **/
    private void navigateToLogin(View view){
        NavController navController = Navigation.findNavController(view);
        navController.navigate(R.id.action_welcomeFragment_to_loginFragment);
    }

    /**
     * Navigates to the registration screen when the signup button is clicked.
     * Uses the NavController to navigate via the action defined in the navigation graph.
     **/
    private void navigateToRegister(View view){
        NavController navController = Navigation.findNavController(view);
        navController.navigate(R.id.action_welcomeFragment_to_registrationFragment);
    }

    /**
     * Called when the fragment's view has been created.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the callback when the view is destroyed
        onBackPressedCallback.remove();
    }
}