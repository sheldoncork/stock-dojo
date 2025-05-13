package com.softdev.stocksim.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.softdev.stocksim.BaseLoadingFragment;
import com.softdev.stocksim.R;

/**
 * Base fragment that provides common functionality for all fragments.
 *  Handles toolbar configuration, navigation, and error display.
 * - Toolbar management
 * - Loading state handling
 * - Navigation setup
 * - Error handling
 *
 * @author Blake Nelson
 */
public abstract class BaseFragment extends BaseLoadingFragment {
    private static final String TAG = "BaseFragment";

    protected NavController navController;
    private MaterialToolbar toolbar;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupNavigation();
        setupToolbar();
    }

    /**
     * Sets up the navigation controller
     */
    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getParentFragment();
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        }
    }

    /**
     * Sets up the toolbar for this fragment
     * - clears toolbar
     * - configures toolbar
     */
    private void setupToolbar() {
        toolbar = requireActivity().findViewById(R.id.topAppBar);
        if (toolbar != null) {
            // Clear previous menu items
            toolbar.getMenu().clear();

            // Let subclasses configure the toolbar
            configureToolbar(toolbar);
        }
    }

    /**
     * Configure toolbar for this specific fragment.
     * Override this in subclasses to customize toolbar behavior.
     */
    protected void configureToolbar(@NonNull MaterialToolbar toolbar) {
        // Let NavigationUI handle the navigation icon
        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
                R.id.homeFragment,
                R.id.searchFragment,
                R.id.classroomContainerFragment,
                R.id.settingsFragment
        ).build();

        NavigationUI.setupWithNavController(toolbar, navController, appBarConfig);
    }

    /**
     * Sets the toolbar title
     * @param title The title to set
     */
    protected void setToolbarTitle(String title) {
        if (toolbar != null) {
            toolbar.setTitle(title);
        }
    }

    /**
     * Clears the toolbar menu
     */
    protected void clearToolbarMenu() {
        if (toolbar != null) {
            toolbar.getMenu().clear();
        }
    }

    /**
     * Inflates a menu resource into the toolbar
     * @param menuResId The resource ID of the menu to inflate
     */
    protected void inflateToolbarMenu(@MenuRes int menuResId) {
        if (toolbar != null) {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(menuResId);
        }
    }

    /**
     * Sets a click listener for toolbar menu items
     * @param listener The listener to set
     */
    protected void setToolbarMenuClickListener(MaterialToolbar.OnMenuItemClickListener listener) {
        if (toolbar != null) {
            toolbar.setOnMenuItemClickListener(listener);
        }
    }

    /**
     * Sets a click listener for the toolbar navigation icon
     * - Need to set back nav Icon manually in order to override set navigation back press
     * @param listener The listener to set
     */
    protected void setNavigationOnClickListener(View.OnClickListener listener) {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(listener);
        }
    }

    /**
     * Shows an error message with a retry option
     * @param message The error message to display
     */
    protected void showError(String message) {
        Log.d(getClass().getSimpleName(), message);
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> onRetry())
                    .show();
        } else {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Shows a toast message
     * @param message The message to display
     */
    protected void showToast(String message) {
        if (getView() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when user taps retry on error message.
     * Override in subclasses to implement retry behavior.
     */
    protected void onRetry() {
        // Override in subclasses for retry functionality
    }
}