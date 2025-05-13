package com.softdev.stocksim.ui.classroom.announcements;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.ui.classroom.BaseClassroomFragment;
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
 * Fragment for displaying, editing, and deleting announcement details.
 * Provides additional editing capabilities for teachers.
 *
 * @author Blake Nelson
 */
public class AnnouncementDetailFragment extends BaseClassroomFragment {
    private static final String TAG = "AnnouncementDetailFragment";

    // State
    private long announcementId;
    private String userType;
    private AnnouncementItem announcement;

    // Views
    private TextView titleView;
    private TextView contentView;
    private TextView datetimeView;
    private TextView editedDatetimeView;
    private TextView classroomNameView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get arguments using Safe Args
        AnnouncementDetailFragmentArgs args = AnnouncementDetailFragmentArgs.fromBundle(requireArguments());
        announcementId = args.getAnnouncementId();

        // Get user type
        UserPreferences userPreferences = UserPreferences.getInstance(requireContext());
        userType = userPreferences.getUserType();
    }

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classroom_announcement_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        fetchAnnouncementDetails();
    }

    /**
     * Initializes view references and sets up click listeners.
     * Shows/hides teacher actions based on user role.
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        setToolbarTitle("Announcement Details");

        // Initialize views
        titleView = view.findViewById(R.id.announcement_title);
        contentView = view.findViewById(R.id.announcement_content);
        datetimeView = view.findViewById(R.id.announcement_datetime);
        editedDatetimeView = view.findViewById(R.id.announcement_edited_datetime);
        classroomNameView = view.findViewById(R.id.announcement_classroomName);
        View teacherActions = view.findViewById(R.id.teacher_buttons);

        // Show/hide teacher actions based on user role
        if (userType.equals(AppConfig.UserType.TEACHER)) {
            teacherActions.setVisibility(View.VISIBLE);
            Button editButton = view.findViewById(R.id.edit_button);
            Button deleteButton = view.findViewById(R.id.delete_button);
            editButton.setOnClickListener(v -> showEditDialog());
            deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        } else {
            teacherActions.setVisibility(View.GONE);
        }
    }

    /**
     * Fetches detailed announcement information from the server.
     */
    private void fetchAnnouncementDetails() {
        String url = AppConfig.BASE_URL + "/announcements?announcementId=" + announcementId;
        Log.d(TAG, "Fetching announcement from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        announcement = new AnnouncementItem(
                                response.getLong("id"),
                                response.getString("title"),
                                response.getString("content"),
                                response.getString("postDate"),
                                response.getBoolean("edited"),
                                response.optString("editedDate", ""),
                                response.getString("classroomName"),
                                false
                        );
                        updateUI();
                        hideLoading();
                    } catch (JSONException e) {
                        hideLoading();
                        Log.e(TAG, "Error parsing announcement", e);
                        showError("Failed to parse announcement");
                    }
                },
                error -> {
                    hideLoading();
                    Log.e(TAG, "Error fetching announcement details", error);
                    if (error.networkResponse != null) {
                        switch (error.networkResponse.statusCode) {
                            case 403:
                                showError("You don't have permission to view this announcement");
                                break;
                            case 404:
                                showError("Announcement not found");
                                break;
                            default:
                                showError("Failed to fetch announcement");
                                break;
                        }
                    } else {
                        showError("Network error. Please check your connection.");
                    }
                });

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Updates an existing announcement on the server.
     * @param title New title for the announcement
     * @param content New content for the announcement
     */
    private void updateAnnouncement(String title, String content) {
        showLoading();
        String url = AppConfig.BASE_URL + "/announcements";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("id", announcement.getId());
            jsonBody.put("title", title);
            jsonBody.put("content", content);
        } catch (JSONException e) {
            hideLoading();
            Log.e(TAG, "Error creating JSON body", e);
            showError("Failed to update announcement");
            return;
        }

        Log.d(TAG, "Updating announcement: " + jsonBody);

        // Use StringRequest since response is a string
        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    Toast.makeText(getContext(), "Announcement updated", Toast.LENGTH_SHORT).show();
                    fetchAnnouncementDetails(); // Refresh the details
                },
                error -> {
                    Log.e(TAG, "Error updating announcement", error);
                    if (error.networkResponse != null) {
                        String errorMessage;
                        errorMessage = new String(error.networkResponse.data, StandardCharsets.UTF_8);

                        switch (error.networkResponse.statusCode) {
                            case 403:
                                showError("You cannot edit this announcement");
                                break;
                            case 404:
                                showError("Announcement not found");
                                break;
                            default:
                                showError("Failed to update announcement: " + errorMessage);
                                break;
                        }
                    } else {
                        showError("Network error. Please check your connection.");
                    }
                    hideLoading();
                }
        ) {
            @Override
            public byte[] getBody() {
                return jsonBody.toString().getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Deletes the current announcement.
     * Only available to teachers.
     */
    private void deleteAnnouncement() {
        showLoading();
        String url = AppConfig.BASE_URL + "/announcements?announcementId=" + announcement.getId();
        Log.d(TAG, "Deleting announcement from: " + url);

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Toast.makeText(getContext(), "Announcement deleted", Toast.LENGTH_SHORT).show();
                    navController.popBackStack(); // Go back to list
                },
                error -> {
                    hideLoading();
                    Log.e(TAG, "Error deleting announcement", error);
                    if (error.networkResponse != null) {
                        String errorMessage;
                        errorMessage = new String(error.networkResponse.data, StandardCharsets.UTF_8);

                        switch (error.networkResponse.statusCode) {
                            case 403:
                                showError("You cannot delete this announcement");
                                break;
                            case 404:
                                showError("Announcement not found");
                                break;
                            default:
                                showError("Failed to delete announcement: " + errorMessage);
                                break;
                        }
                    } else {
                        showError("Network error. Please check your connection.");
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "text/plain");
                return headers;
            }
        };

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    // UI Methods
    private void updateUI() {
        if (announcement != null) {
            titleView.setText(announcement.getTitle());
            contentView.setText(announcement.getContent());
            datetimeView.setText(announcement.getFormattedDateTime());
            classroomNameView.setText(announcement.getClassroomName());

            if (announcement.isEdited()) {
                editedDatetimeView.setVisibility(View.VISIBLE);
                editedDatetimeView.setText(announcement.getFormattedEditedDateTime());
            } else {
                editedDatetimeView.setVisibility(View.GONE);
            }
        }
    }

    private void showEditDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_announcement, null);

        TextInputLayout titleLayout = dialogView.findViewById(R.id.announcement_title_layout);
        TextInputLayout contentLayout = dialogView.findViewById(R.id.announcement_content_layout);
        TextInputEditText titleInput = dialogView.findViewById(R.id.announcement_title_input);
        TextInputEditText contentInput = dialogView.findViewById(R.id.announcement_content_input);

        // Pre-fill existing content
        titleInput.setText(announcement.getTitle());
        contentInput.setText(announcement.getContent());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Announcement")
                .setView(dialogView)
                .setPositiveButton("Save", null) // Set to null initially to prevent auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Clear previous errors
            titleLayout.setError(null);
            contentLayout.setError(null);

            if (titleInput.getText() == null || titleInput.getText().toString().trim().isEmpty()) {
                titleLayout.setError("Please enter a title");
                return;
            }
            if (contentInput.getText() == null || contentInput.getText().toString().trim().isEmpty()) {
                contentLayout.setError("Please enter content");
                return;
            }

            String title = titleInput.getText().toString().trim();
            String content = contentInput.getText().toString().trim();

            boolean isValid = true;

            if (title.isEmpty()) {
                titleLayout.setError("Title is required");
                isValid = false;
            }
            if (content.isEmpty()) {
                contentLayout.setError("Content is required");
                isValid = false;
            }

            if (isValid) {
                updateAnnouncement(title, content);
                dialog.dismiss();
            }
        }));

        dialog.show();
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Announcement")
                .setMessage("Are you sure you want to delete this announcement?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAnnouncement())
                .setNegativeButton("Cancel", null)
                .show();
    }
}