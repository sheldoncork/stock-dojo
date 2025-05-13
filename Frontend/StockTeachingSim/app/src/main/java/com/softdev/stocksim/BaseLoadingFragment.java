package com.softdev.stocksim;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Base fragment class providing loading state management functionality.
 * Implements a loading overlay pattern that can be shown during async operations.
 *
 * @author Blake Nelson
 */
public abstract class BaseLoadingFragment extends Fragment {
    private static final String TAG = "BaseLoadingFragment";

    private View loadingView;
    private View contentView;
    private boolean isLoading;


    @NonNull
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View baseView = inflater.inflate(R.layout.fragment_base_loading, container, false);
        FrameLayout contentContainer = baseView.findViewById(R.id.content_container);
        loadingView = baseView.findViewById(R.id.layout_loading);
        contentView = onCreateContentView(inflater, contentContainer, savedInstanceState);

        if (contentView != null) {
            contentContainer.addView(contentView);
        }

        showLoading();
        return baseView;
    }

    /**
     * Abstract method to be implemented by subclasses to provide their content view.
     * Called during onCreateView to get the fragment-specific content.
     * @param inflater The LayoutInflater object that can be used to inflate
     * @param container Parent container for the content
     * @param savedInstanceState Previous state if being recreated
     * @return The content view
     */
    protected abstract View onCreateContentView(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState);

    /**
     * Shows the loading overlay and disables content interaction.
     * Safe to call multiple times - will only update if changing state.
     */
    protected void showLoading() {
        Log.d(TAG, "Show loading called");
        if (!isLoading){
            isLoading = true;
            Log.d("Loading", "Updating loading state to VISIBLE");
            updateLoadingState();
        }
    }

    /**
     * Hides the loading overlay and enables content interaction.
     * Safe to call multiple times - will only update if changing state.
     */
    protected void hideLoading() {
        Log.d(TAG, "Hide loading called");
        if (isLoading){
            isLoading = false;
            Log.d("Loading", "Updating loading state to GONE");
            updateLoadingState();
        }
    }

    /**
     * Updates the visibility and interaction state of views based on loading state.
     */
    private void updateLoadingState() {
        if (loadingView == null || contentView == null || !isAdded()) {
            return;
        }

        loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        contentView.setEnabled(!isLoading);
        contentView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}
