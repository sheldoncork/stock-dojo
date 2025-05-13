package com.softdev.stocksim.ui.classroom.classrooms.list;

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
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavDirections;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Fragment for displaying and managing the list of classrooms.
 * Provides different functionality for teachers and students.
 *
 * @author Blake Nelson
 */
public class ClassroomListFragment extends BaseClassroomFragment {
    private static final String TAG = "ClassroomListFragment";

    // UI Components
    private RecyclerView recyclerView;
    private View noClassroomsLayout;
    private Button announcementsButton;
    private View newAnnouncementIndicator;
    private ClassroomAdapter adapter;


    // State
    private ArrayList<ClassroomItem> classrooms;
    private String userType;
    private boolean isOnlyClassroom;

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutResId = userType.equals(AppConfig.UserType.STANDARD) ?
                R.layout.layout_classroom_standard :
                R.layout.fragment_classroom_list;

        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeUserData();
    }

    private void initializeUserData() {
        UserPreferences userPreferences = UserPreferences.getInstance(requireContext());
        userType = userPreferences.getUserType();

        if (userPreferences.getUsername() == null || userType == null) {
            throw new IllegalStateException("User not properly initialized");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setToolbarTitle("Classrooms");

        if (userType.equals(AppConfig.UserType.STANDARD)) {
            Log.d(TAG, "Standard user type, hiding loading");
            hideLoading();
            return;
        }

        navController.getCurrentBackStackEntry()
                .getSavedStateHandle()
                .getLiveData("refresh", false)
                .observe(getViewLifecycleOwner(), needsRefresh -> {
                    if (needsRefresh) {
                        fetchClassrooms();
                        navController.getCurrentBackStackEntry()
                                .getSavedStateHandle()
                                .set("refresh", false);
                    }
                });

        initializeViews(view);
        setupRecyclerView();
        fetchClassrooms();
    }

    private void initializeViews(View view) {

        // Get the announcement button and notification indicator from parent layout
        View announcementButtonLayout = view.findViewById(R.id.list_announcement_button_layout);
        announcementsButton = announcementButtonLayout.findViewById(R.id.announcements_button);
        newAnnouncementIndicator = announcementButtonLayout.findViewById(R.id.new_announcement_indicator);

        // Set click listener for the announcements button
        announcementsButton.setOnClickListener(v -> openAnnouncements());

        // Initialize the rest of UI components
        recyclerView = view.findViewById(R.id.classroom_list_recycler_view);
        noClassroomsLayout = view.findViewById(R.id.list_no_classrooms_layout);

        // Initialize state
        classrooms = new ArrayList<>();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassroomAdapter(this::onClassroomClick);
        recyclerView.setAdapter(adapter);
    }

    private void fetchClassrooms() {
        Log.d(TAG, "Fetching classrooms");
        String url = AppConfig.BASE_URL + "/classroom/classes";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Response: " + response.toString());
                    classrooms.clear();
                    if (response.length() == 0) {
                        isOnlyClassroom = false;
                        showNoClassrooms();
                        hideLoading();
                        return;
                    }

                    try {
                        isOnlyClassroom = response.length() == 1;
                        boolean hasNewAnnouncements = false;

                        // Always process the classrooms, regardless of count
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject classroom = response.getJSONObject(i);
                            classrooms.add(new ClassroomItem(
                                    classroom.getString("classId"),
                                    classroom.getString("className"),
                                    classroom.optString("classCode"),
                                    userType.equals(AppConfig.UserType.TEACHER) ?
                                            classroom.getInt("numStudents") + " students" :
                                            classroom.getString("teacherName")
                            ));
                            if (classroom.getBoolean("hasNewAnnouncements")) {
                                hasNewAnnouncements = true;
                            }
                        }

                        // Handle navigation only if this is the first load and there's exactly one classroom
                        if (isOnlyClassroom && classrooms.size() == 1 &&
                                navController.getCurrentDestination().getId() != R.id.classroomDetailsFragment) {
                            ClassroomItem classroom = classrooms.get(0);
                            NavDirections action = ClassroomListFragmentDirections
                                    .actionToClassroomDetails(
                                            classroom.getClassId(),
                                            classroom.getClassName(),
                                            classroom.getJoinCode(),
                                            true
                                    );
                            navController.navigate(action);
                        } else {
                            showClassrooms(hasNewAnnouncements);
                        }
                        hideLoading();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing classrooms", e);
                        hideLoading();
                        showError("Failed to load classrooms");
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching classrooms", error);
                    hideLoading();
                    showError("Failed to fetch classrooms");
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Shows the list of classrooms
     */
    private void showClassrooms(boolean hasNewAnnouncements) {
        newAnnouncementIndicator.setVisibility(hasNewAnnouncements ? View.VISIBLE : View.INVISIBLE);
        announcementsButton.setVisibility(View.VISIBLE);
        announcementsButton.setText(R.string.classroom_list_announcements_button_text);

        setToolbarTitle("Classrooms");
        inflateToolbarMenu(R.menu.top_add_menu);
        setToolbarMenuClickListener(item -> {
            if (item.getItemId() == R.id.action_add) {
                if (userType.equals(AppConfig.UserType.TEACHER)) {
                    showCreateClassroomDialog();
                } else {
                    showJoinClassroomDialog();
                }
                return true;
            }
            return false;
        });

        recyclerView.setVisibility(View.VISIBLE);
        noClassroomsLayout.setVisibility(View.GONE);
        announcementsButton.setVisibility(View.VISIBLE);

        // Create a new list to force DiffUtil to detect the change
        ArrayList<ClassroomItem> newList = new ArrayList<>(classrooms);
        adapter.submitList(null);
        adapter.submitList(newList);

        adapter.submitList(newList);
    }

    private void showCreateClassroomDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_classroom, null);

        TextInputLayout classNameLayout = dialogView.findViewById(R.id.classroom_name_layout);
        TextInputLayout balanceLayout = dialogView.findViewById(R.id.start_balance_layout);
        TextInputEditText classNameInput = dialogView.findViewById(R.id.classroom_name_input);
        TextInputEditText startBalanceInput = dialogView.findViewById(R.id.start_balance_input);

        AlertDialog dialog = builder.setTitle("Create Classroom")
                .setView(dialogView)
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (classNameInput.getText() == null || startBalanceInput.getText() == null) {
                showToast("Please fill in all fields");
                return;
            }

            String className = classNameInput.getText().toString().trim();
            String startBalance = startBalanceInput.getText().toString().trim();

            classNameLayout.setError(null);
            balanceLayout.setError(null);

            if (className.isEmpty()) {
                classNameLayout.setError("Please enter a classroom name");
                return;
            }

            if (!startBalance.isEmpty()) {
                try {
                    double startBalanceDouble = Double.parseDouble(startBalance);
                    if (startBalanceDouble < 0) {
                        balanceLayout.setError("Starting balance cannot be negative");
                        return;
                    }
                } catch (NumberFormatException e) {
                    balanceLayout.setError("Invalid starting balance");
                    return;
                }
            }

            showLoading();
            StringBuilder urlBuilder = new StringBuilder(AppConfig.BASE_URL + "/classroom/create");
            urlBuilder.append("?name=").append(className);

            if (!startBalance.isEmpty()) {
                urlBuilder.append("&startingAmount=").append(startBalance.replace("$", ""));
            }

            StringRequest stringRequest = new StringRequest(Request.Method.POST, urlBuilder.toString(),
                    response -> {
                        dialog.dismiss();
                        showToast("Classroom created successfully");
                        fetchClassrooms();  // Directly fetch instead of using SavedStateHandle
                    },
                    error -> {
                        hideLoading();
                        showError("Failed to create classroom");
                    });

            VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest);
        }));

        dialog.show();
    }

    /**
     * Shows empty state
     */
    private void showNoClassrooms() {
        clearToolbarMenu();
        announcementsButton.setVisibility(View.GONE);
        newAnnouncementIndicator.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        noClassroomsLayout.setVisibility(View.VISIBLE);

        TextView noClassroomsTitle = noClassroomsLayout.findViewById(R.id.no_classrooms_title);
        TextView noClassroomsSubTitle = noClassroomsLayout.findViewById(R.id.no_classrooms_sub_title);
        TextInputLayout studentCodeInputLayout = noClassroomsLayout.findViewById(R.id.student_code_input_layout);
        Button noClassroomsButton = noClassroomsLayout.findViewById(R.id.no_classrooms_button);

        if (userType.equals(AppConfig.UserType.TEACHER)) {
            setupTeacherEmptyState(noClassroomsTitle, noClassroomsSubTitle, studentCodeInputLayout, noClassroomsButton);
        } else {
            setupStudentEmptyState(noClassroomsTitle, noClassroomsSubTitle, studentCodeInputLayout, noClassroomsButton);
        }
    }

    private void setupTeacherEmptyState(TextView title, TextView subtitle, TextInputLayout codeInput, Button button) {
        setToolbarTitle("Classrooms");
        inflateToolbarMenu(R.menu.top_menu);
        title.setText(R.string.no_classrooms_teacher_title);
        subtitle.setText(R.string.no_classrooms_teacher_subtitle);
        codeInput.setVisibility(View.GONE);
        button.setText(R.string.create_classroom_button);
        button.setOnClickListener(v -> showCreateClassroomDialog());
    }

    private void setupStudentEmptyState(TextView title, TextView subtitle, TextInputLayout codeInput, Button button) {
        setToolbarTitle("Join a classroom");
        inflateToolbarMenu(R.menu.top_menu);
        title.setText(R.string.no_classroom_student_title);
        subtitle.setText(R.string.no_classroom_student_subtitle);
        codeInput.setVisibility(View.VISIBLE);
        button.setText(R.string.no_classrooms_student_button);

        TextInputLayout joinCodeInput = codeInput.findViewById(R.id.student_code_input_layout);
        button.setOnClickListener(v -> {
            if (joinCodeInput.getEditText() == null || joinCodeInput.getEditText().getText().toString().isEmpty()) {
                joinCodeInput.setError("Please enter a classroom code");
                joinCodeInput.requestFocus();
                return;
            }
            joinClassroom(joinCodeInput, null);
        });
    }

    private void onClassroomClick(ClassroomItem classroom) {
        NavDirections action = ClassroomListFragmentDirections
                .actionToClassroomDetails(
                        classroom.getClassId(),
                        classroom.getClassName(),
                        classroom.getJoinCode(),
                        isOnlyClassroom
                );
        navController.navigate(action);
    }

    private void openAnnouncements() {
        NavDirections action = ClassroomListFragmentDirections
                .actionToAnnouncementList(
                        true,  // fetchAll
                        null,  // classroomId
                        null   // className
                );
        navController.navigate(action);
    }

    // 977879
    private void showJoinClassroomDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_join_classroom, null);
        TextInputLayout joinCodeLayout = dialogView.findViewById(R.id.join_classroom_code_layout);
        TextInputEditText joinCodeInput = dialogView.findViewById(R.id.join_classroom_code_input);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Join Classroom")
                .setView(dialogView)
                .setPositiveButton("Join", null) // Set to null initially
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        // Show dialog
        dialog.show();

        // Set positive button click listener after dialog is shown
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (joinCodeInput.getText() == null || joinCodeInput.getText().toString().isEmpty()) {
                joinCodeLayout.setError("Please enter a join code");
                return;
            }

            String code = joinCodeInput.getText().toString().trim();

            if (code.isEmpty()) {
                joinCodeLayout.setError("Please enter a join code");
                return;
            }

            // Clear any previous errors
            joinCodeLayout.setError(null);

            // Join classroom
            joinClassroom(joinCodeLayout, dialog);

        });
    }


    private void joinClassroom(TextInputLayout joinCodeLayout, AlertDialog dialog) {
        if (joinCodeLayout.getEditText() == null || joinCodeLayout.getEditText().getText().toString().isEmpty()) {
            joinCodeLayout.setError("Please enter a classroom code");
            Toast.makeText(getContext(), "Please enter a classroom code", Toast.LENGTH_SHORT).show();
            return;
        }

        String joinCode = joinCodeLayout.getEditText().getText().toString().trim();

        String url = AppConfig.BASE_URL + "/classroom/join-classroom?joinCode=" + joinCode;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    showToast("Successfully joined classroom");
                    if (dialog != null){
                        dialog.dismiss();
                    }
                    fetchClassrooms();
                },
                error -> {
                    String errorMessage;
                    if (error.networkResponse != null) {
                        errorMessage = new String(error.networkResponse.data, StandardCharsets.UTF_8);

                        if (error.networkResponse.statusCode == 404 && errorMessage.trim().equals("Classroom not found")) {
                            joinCodeLayout.setError("Classroom not found");
                        } else {
                            showError("Failed to join classroom");
                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    } else {
                        showError("Failed to join classroom");
                        Toast.makeText(getContext(), "Network error occurred", Toast.LENGTH_LONG).show();
                    }
                });

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }
}