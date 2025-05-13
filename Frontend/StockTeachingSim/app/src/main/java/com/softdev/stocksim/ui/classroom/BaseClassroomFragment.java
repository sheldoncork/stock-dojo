package com.softdev.stocksim.ui.classroom;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.softdev.stocksim.R;
import com.softdev.stocksim.ui.BaseFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Base fragment for classroom-related functionality.
 * Handles classroom-specific navigation and back press behavior.
 *
 * @author Blake Nelson
 */
public abstract class BaseClassroomFragment extends BaseFragment {
    private boolean isOnlyClassroom;

    /**
     * Sets up back press handling for classroom navigation.
     *
     * @param savedInstanceState Previous state if being recreated
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup back press handling
        requireActivity().getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBackNavigation();
                    }
                });
    }

    /**
     * Configures toolbar specifically for classroom navigation.
     * Sets up navigation with classroom-specific destinations.
     * */
    @Override
    protected void configureToolbar(@NonNull MaterialToolbar toolbar) {
        // Define top-level destinations within classroom flow
        Set<Integer> classroomTopLevelDestinations = new HashSet<>(Arrays.asList(
                R.id.classroomListFragment,
                R.id.classroomDetailsFragment
        ));

        // Configure navigation with classroom-specific destinations
        AppBarConfiguration classroomAppBarConfig = new AppBarConfiguration.Builder(classroomTopLevelDestinations)
                .setFallbackOnNavigateUpListener(this::handleBackNavigation)
                .build();

        // Set up the toolbar with classroom-specific navigation
        NavigationUI.setupWithNavController(toolbar, navController, classroomAppBarConfig);

        // Override the default navigation click listener to use custom back handling
        toolbar.setNavigationOnClickListener(v -> handleBackNavigation());
    }

    /**
     * Handles back navigation consistently for both toolbar and system back
     * @return true if navigation was handled
     */
    protected boolean handleBackNavigation() {
        if (navController.getCurrentDestination() != null) {
            int currentDestId = navController.getCurrentDestination().getId();

            // Handle navigation based on current destination
            if (currentDestId == R.id.classroomListFragment) {
                // From classroom list -> main navigation (home)
                NavController mainNav = Navigation.findNavController(
                        requireActivity(),
                        R.id.nav_host_fragment
                );
                return mainNav.navigateUp();
            } else if (currentDestId == R.id.classroomDetailsFragment) {
                // If only one classroom -> home
                // Otherwise -> classroom list
                if (isOnlyClassroom) {
                    NavController mainNav = Navigation.findNavController(
                            requireActivity(),
                            R.id.nav_host_fragment
                    );
                    return mainNav.navigateUp();
                } else {
                    return navController.popBackStack();
                }
            } else {
                return navController.navigateUp();
            }
        }
        return false;
    }

    /**
     * Sets whether this fragment is only for a single classroom.
     * @param isOnlyClassroom True if only one classroom
     */
    protected void setIsOnlyClassroom(boolean isOnlyClassroom) {
        this.isOnlyClassroom = isOnlyClassroom;
    }

    /**
     * Gets whether this fragment is only for a single classroom.
     * @return True if only one classroom
     */
    protected boolean getIsOnlyClassroom() {
        return isOnlyClassroom;
    }
}