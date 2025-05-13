package com.softdev.stocksim.ui.classroom.announcements;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for displaying a list of announcements.
 * Can show either all announcements across classrooms or announcements for a specific classroom.
 * Handles announcement creation for teachers.
 *
 * @author Blake Nelson
 */
public class AnnouncementListFragment extends BaseClassroomFragment {
    private static final String TAG = "AnnouncementListFragment";

    // UI Components
    private RecyclerView announcementsRecyclerView;
    private View noAnnouncementsView;
    private AnnouncementAdapter adapter;
    private final List<AnnouncementItem> announcements = new ArrayList<>();

    // State
    String userType;
    boolean fetchAll;
    String classroomId;
    String classroomName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnnouncementListFragmentArgs args = AnnouncementListFragmentArgs.fromBundle(requireArguments());
        fetchAll = args.getFetchAll();
        classroomId = args.getClassroomId();
        classroomName = args.getClassName();

        UserPreferences userPreferences = UserPreferences.getInstance(requireContext());
        userType = userPreferences.getUserType();
    }

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classroom_announcement_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRecyclerView();
        fetchAnnouncements();
    }

    /**
     * Initializes view references from the layout.
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        setToolbarTitle(fetchAll ? "All Announcements" : classroomName + " announcements");

        if (!fetchAll && userType.equals(AppConfig.UserType.TEACHER)) {
            inflateToolbarMenu(R.menu.top_add_menu);
            setToolbarMenuClickListener(item -> {
                if (item.getItemId() == R.id.action_add) {
                    showAddAnnouncementDialog();
                    return true;
                }
                return false;
            });
        } else {
            inflateToolbarMenu(R.menu.top_menu);
        }

        announcementsRecyclerView = view.findViewById(R.id.announcements_recycler_view);
        noAnnouncementsView = view.findViewById(R.id.no_announcements_layout);
    }

    /**
     * Sets up the RecyclerView with layout manager and adapter.
     */
    private void setupRecyclerView() {
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AnnouncementAdapter(fetchAll, this::onAnnouncementClick);
        announcementsRecyclerView.setAdapter(adapter);
    }

    /**
     * Handles click events on announcement items by navigating to detail view.
     * @param announcement The announcement item that was clicked
     */
    private void onAnnouncementClick(AnnouncementItem announcement) {
        AnnouncementListFragmentDirections.ActionToAnnouncementDetail action =
                AnnouncementListFragmentDirections.actionToAnnouncementDetail(announcement.getId());
        navController.navigate(action);
    }

    /**
     * Fetches announcements from the server based on fragment configuration.
     * Uses different endpoints for all-announcements vs classroom-specific views.
     */
    private void fetchAnnouncements() {
        String url;
        if (fetchAll) {
            url = AppConfig.BASE_URL + "/announcements/all";
        } else {
            url = AppConfig.BASE_URL + "/announcements/classroom?classroomId=" + classroomId;
        }
        Log.d(TAG, "Fetching announcements from: " + url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET,
                url,
                null,
                response -> {
                    announcements.clear();
                    boolean isTeacher = userType.equals(AppConfig.UserType.TEACHER);
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            announcements.add(new AnnouncementItem(
                                    obj.getLong("id"),
                                    obj.getString("title"),
                                    obj.getString("content"),
                                    obj.getString("postDate"),
                                    obj.getBoolean("edited"),
                                    obj.optString("editedDate", ""),
                                    obj.getString("classroomName"),
                                    !isTeacher && obj.getBoolean("notViewed")
                            ));
                        }
                        if (announcements.isEmpty()) {
                            showNoAnnouncements();
                            hideLoading();
                        } else {
                            showAnnouncements();
                            hideLoading();
                        }
                    } catch (JSONException e) {
                        hideLoading();
                        Log.e(TAG, "Error parsing announcements", e);
                        showError("Failed to parse announcements");
                    }
                },
                error -> {
                    hideLoading();
                    Log.e(TAG, "Error fetching announcements", error);

                    if (error.networkResponse != null) {
                        switch (error.networkResponse.statusCode) {
                            case 403:
                                showError(fetchAll ?
                                        "You don't have permission to view announcements" :
                                        "You don't have access to this classroom");
                                break;
                            case 404:
                                showError(fetchAll ?
                                        "No announcements found" :
                                        "Classroom not found");
                                break;
                            default:
                                showError("Failed to fetch announcements");
                                break;
                        }
                    } else {
                        showError("Network error. Please check your connection.");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    
    /**
     * Posts a new announcement to the server.
     * @param title The title of the announcement
     * @param content The content of the announcement
     */
    private void postAnnouncement(String title, String content) {
        showLoading();
        String url = AppConfig.BASE_URL + "/announcements?classroomId=" + classroomId;

        Log.d(TAG, "Posting announcement to URL: " + url);
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("title", title);
            jsonBody.put("content", content);
        } catch (JSONException e) {
            hideLoading();
            Log.e(TAG, "Error creating JSON body", e);
            showError("Failed to create announcement");
            return;
        }

        // Create StringRequest with the JSON body
        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    // Success - server returns "Announcement created"
                    Toast.makeText(getContext(), "Announcement posted", Toast.LENGTH_SHORT).show();
                    fetchAnnouncements(); // Refresh the list
                },
                error -> {
                    hideLoading();
                    Log.e(TAG, "Error posting announcement", error);
                    if (error.networkResponse != null) {
                        String errorMessage;
                        errorMessage = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        switch (error.networkResponse.statusCode) {
                            case 403:
                                showError("You are not the teacher of this classroom");
                                break;
                            case 404:
                                showError("Classroom not found");
                                break;
                            default:
                                showError("Failed to create announcement" + errorMessage);
                                break;
                        }
                    } else {
                        showError("Network error. Please check your connection.");
                    }
                }
        ) {
            @Override
            public byte[] getBody() {
                return jsonBody.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
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
     * Shows empty state when no announcements are available.
     * Displays different messages for teachers and students.
     */
    private void showNoAnnouncements() {
        announcementsRecyclerView.setVisibility(View.GONE);
        noAnnouncementsView.setVisibility(View.VISIBLE);

        TextView subtitle = noAnnouncementsView.findViewById(R.id.no_announcements_subtitle);

        subtitle.setText(userType.equals(AppConfig.UserType.TEACHER) ?
                R.string.no_announcements_teacher_subtitle :
                R.string.no_announcements_student_subtitle);
    }

    /**
     * Updates UI to show list of announcements.
     */
    private void showAnnouncements() {
        // Hide other views
        noAnnouncementsView.setVisibility(View.GONE);
        announcementsRecyclerView.setVisibility(View.VISIBLE);
        adapter.updateAnnouncements(announcements);
    }

    /**
     * Shows dialog for creating a new announcement.
     */
    private void showAddAnnouncementDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_announcement, null);

        TextInputLayout titleLayout = dialogView.findViewById(R.id.announcement_title_layout);
        TextInputLayout contentLayout = dialogView.findViewById(R.id.announcement_content_layout);
        TextInputEditText titleInput = dialogView.findViewById(R.id.announcement_title_input);
        TextInputEditText contentInput = dialogView.findViewById(R.id.announcement_content_input);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("New Announcement")
                .setView(dialogView)
                .setPositiveButton("Post", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                titleLayout.setError(null);
                contentLayout.setError(null);

                if (titleInput.getText() == null || contentInput.getText() == null) {
                    if (titleInput.getText() == null) {
                        titleLayout.setError("Title cannot be empty");
                    }
                    if (contentInput.getText() == null) {
                        contentLayout.setError("Content cannot be empty");
                    }
                } else if (titleInput.getText().toString().trim().isEmpty() || contentInput.getText().toString().trim().isEmpty()) {
                    if (titleInput.getText().toString().trim().isEmpty()) {
                        titleLayout.setError("Title cannot be empty");
                    }
                    if (contentInput.getText().toString().trim().isEmpty()) {
                        contentLayout.setError("Content cannot be empty");
                    }
                } else {
                    String title = titleInput.getText().toString().trim();
                    String content = contentInput.getText().toString().trim();
                    postAnnouncement(title, content);
                    dialog.dismiss();
                }
            });
        });
        dialog.show();
    }
}