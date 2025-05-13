package com.softdev.stocksim.ui.classroom.classrooms.details;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.ui.classroom.BaseClassroomFragment;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.data.UserPreferences;
import com.softdev.stocksim.api.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Fragment displaying classroom details including student list and classroom management options.
 * Provides different functionality for teachers and students.
 *
 * @author Blake Nelson
 */
public class ClassroomDetailsFragment extends BaseClassroomFragment {
    private static final String TAG = "ClassroomDetailsFragment";

    // UI Components
    private TextView joinCodeText;
    private View announcementsButtonLayout;
    private Button announcementsButton;
    private View newAnnouncementsIndicator;

    private RecyclerView recyclerView;
    private ExtendedFloatingActionButton chatFab;
    private View emptyClassroomLayout;

    // State
    private StudentAdapter adapter;
    private ArrayList<StudentItem> students;
    private String classroomId;
    private String className;
    private String joinCode;
    private String userType;
    private String username;

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classroom_details, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeUserData();
        parseArguments();
    }

    /**
     * Initializes user data from UserPreferences.
     */
    private void initializeUserData() {
        UserPreferences userPreferences = UserPreferences.getInstance(requireContext());
        username = userPreferences.getUsername();
        userType = userPreferences.getUserType();

        if (username == null || userType == null) {
            throw new IllegalStateException("User not properly initialized");
        }
    }

    private void parseArguments() {
        ClassroomDetailsFragmentArgs args = ClassroomDetailsFragmentArgs.fromBundle(requireArguments());
        classroomId = args.getClassroomId();
        className = args.getClassName();
        joinCode = args.getJoinCode();
        setIsOnlyClassroom(args.getIsOnlyClassroom());
    }

    @Override
    protected void configureToolbar(@NonNull MaterialToolbar toolbar) {
        setToolbarTitle(className);

        if (!getIsOnlyClassroom()) {
            // Show back arrow that returns to list
            toolbar.setNavigationIcon(R.drawable.ic_back);
            toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        } else {
            // No back arrow for single classroom
            toolbar.setNavigationIcon(null);
        }

        // Rest of your toolbar configuration (menus etc)
        if (userType.equals(AppConfig.UserType.TEACHER)) {
            inflateToolbarMenu(getIsOnlyClassroom() ?
                    R.menu.top_settings_add_menu :
                    R.menu.top_settings_menu);
            setToolbarMenuClickListener(this::handleTeacherMenuClick);
        } else {
            inflateToolbarMenu(getIsOnlyClassroom() ?
                    R.menu.top_minus_add_menu :
                    R.menu.top_minus_menu);
            setToolbarMenuClickListener(this::handleStudentMenuClick);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRecyclerView();

        // Observe saved state handle for refresh triggers
        navController.getCurrentBackStackEntry()
                .getSavedStateHandle()
                .getLiveData("refresh", false)
                .observe(getViewLifecycleOwner(), shouldRefresh -> {
                    if (shouldRefresh) {
                        fetchStudents();
                        // Reset the flag
                        navController.getCurrentBackStackEntry()
                                .getSavedStateHandle()
                                .set("refresh", false);
                    }
                });

        // Observe class name updates
        navController.getCurrentBackStackEntry()
                .getSavedStateHandle()
                .getLiveData("updatedClassName", null)
                .observe(getViewLifecycleOwner(), newClassName -> {
                    if (newClassName != null) {
                        className = (String) newClassName;
                        setToolbarTitle(className);

                        // Reset the class name update flag
                        navController.getCurrentBackStackEntry()
                                .getSavedStateHandle()
                                .remove("updatedClassName");
                    }
                });

        fetchStudents();
    }

    /**
     * Initializes views and sets up click listeners.
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        setToolbarTitle(className);

        joinCodeText = view.findViewById(R.id.join_code_text);
        announcementsButtonLayout = view.findViewById(R.id.details_announcement_button_layout);
        announcementsButton = announcementsButtonLayout.findViewById(R.id.announcements_button);
        newAnnouncementsIndicator = announcementsButtonLayout.findViewById(R.id.new_announcement_indicator);

        recyclerView = view.findViewById(R.id.classroom_recycler_view);
        chatFab = view.findViewById(R.id.chat_fab);
        emptyClassroomLayout = view.findViewById(R.id.details_empty_classroom_layout);

        students = new ArrayList<>();

        // Set up click listeners
        announcementsButton.setOnClickListener(v -> openAnnouncements());
        chatFab.setOnClickListener(v -> openChat());

        // Show join code for teacher
        TextView joinCodeText = view.findViewById(R.id.join_code_text);

        if (joinCodeText != null) {
            switch (userType) {
                case AppConfig.UserType.TEACHER:
                    String joinCodeString = joinCode != null ? "Join Code: " + joinCode : "";
                    joinCodeText.setText(joinCodeString);
                    joinCodeText.setVisibility(View.VISIBLE);
                    break;
                case AppConfig.UserType.STUDENT:
                    joinCodeText.setVisibility(View.GONE);
                    break;
            }
        }

    }

    /**
     * Sets up the RecyclerView for displaying students.
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set click listener for student item only if the current user is a Teacher
        StudentAdapter.OnStudentClickListener clickListener =
                userType.equals(AppConfig.UserType.TEACHER) ? this::onTeacherStudentClick : null;

        adapter = new StudentAdapter(clickListener);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Fetches student information from the server.
     */
    private void fetchStudents() {
        String url = AppConfig.BASE_URL + "/classroom/classInfo" + "?classId=" + classroomId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    students.clear();
                    int studentCount = 0;

                    try {
                        boolean hasNewAnnouncements = response.getBoolean("hasNewAnnouncements");
                        JSONArray studentsArray = response.getJSONArray("students");

                        for (int i = 0; i < studentsArray.length(); i++) {
                            JSONObject student = studentsArray.getJSONObject(i);
                            String studentUsername = student.getString("studentUsername");
                            int portfolioId;
                            if (userType.equals(AppConfig.UserType.TEACHER)) {
                                portfolioId = student.getInt("portfolioId");
                            } else {
                                portfolioId = 0;
                            }

                            // Skip current user if student
                            if (userType.equals(AppConfig.UserType.STUDENT) && studentUsername.equals(username)) {
                                continue;
                            }
                            double portfolioStatus = student.getDouble("portfolioStatus");
                            String portfolioStatusString = formatPortfolioStatus(portfolioStatus);
                            students.add(new StudentItem(studentUsername, portfolioStatusString, portfolioId));
                            studentCount++;
                        }

                        if (studentCount == 0) {
                            showEmptyClassroom(hasNewAnnouncements);
                            hideLoading();
                        } else {
                            showStudentList(hasNewAnnouncements);
                            hideLoading();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing students", e);
                        showError("Failed to load students");
                    }
                },
                error -> {
                    showError("Failed to fetch students");
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Formats the portfolio status for display.
     *
     * @param status The portfolio status
     * @return The formatted portfolio status
     */
    private String formatPortfolioStatus(double status) {
        if (status == 0) {
            return "$0.00";
        }

        String prefix = status > 0 ? "+" : "";
        return String.format("%s$%.2f", prefix, status);
    }

    /**
     * Shows an empty classroom layout.
     */
    private void showEmptyClassroom(boolean hasNewAnnouncements) {
        joinCodeText.setVisibility(View.GONE);
        announcementsButtonLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        chatFab.setVisibility(View.GONE);
        emptyClassroomLayout.setVisibility(View.VISIBLE);

        setupToolbarForEmptyClassroom();
        setupViewsForEmptyClassroom(hasNewAnnouncements);
    }

    private void setupToolbarForEmptyClassroom() {
        if (userType.equals(AppConfig.UserType.TEACHER)) {
            inflateToolbarMenu(getIsOnlyClassroom() ?
                    R.menu.top_settings_add_menu :
                    R.menu.top_settings_menu);
            setToolbarMenuClickListener(this::handleTeacherMenuClick);
        } else {
            inflateToolbarMenu(getIsOnlyClassroom() ?
                    R.menu.top_minus_add_menu :
                    R.menu.top_minus_menu);
            setToolbarMenuClickListener(this::handleStudentMenuClick);
        }
    }

    private void setupViewsForEmptyClassroom(boolean hasNewAnnouncements) {
        TextView emptyTitle = emptyClassroomLayout.findViewById(R.id.empty_classroom_title);
        TextView emptySubTitle = emptyClassroomLayout.findViewById(R.id.empty_classroom_subtitle);
        TextView emptyJoinCodeText = emptyClassroomLayout.findViewById(R.id.empty_classroom_join_code_text);
        MaterialCardView joinCodeCard = emptyClassroomLayout.findViewById(R.id.empty_classroom_join_code_card);

        if (userType.equals(AppConfig.UserType.TEACHER)) {
            emptyTitle.setText(R.string.empty_classroom_teacher_title);
            emptySubTitle.setText(R.string.empty_classroom_teacher_subtitle);
            if (emptyJoinCodeText != null) {
                String joinCodeString = joinCode != null ? "Join Code: " + joinCode : "Unknown";
                emptyJoinCodeText.setText(joinCodeString);
                emptyJoinCodeText.setVisibility(View.VISIBLE);
            }
        } else {
            View studentAnnouncementButtonLayout = emptyClassroomLayout.findViewById(R.id.empty_announcement_button_layout);
            studentAnnouncementButtonLayout.setVisibility(View.VISIBLE);
            Button studentAnnouncementsButton = studentAnnouncementButtonLayout.findViewById(R.id.announcements_button);
            View studentNewAnnouncementIndicator = studentAnnouncementButtonLayout.findViewById(R.id.new_announcement_indicator);
            studentAnnouncementsButton.setText(R.string.classroom_details_announcements_button_text);
            studentNewAnnouncementIndicator.setVisibility(hasNewAnnouncements ? View.VISIBLE : View.GONE);

            studentAnnouncementsButton.setOnClickListener(view -> openAnnouncements());

            emptyTitle.setText(R.string.empty_classroom_student_title);
            emptySubTitle.setText(R.string.empty_classroom_student_subtitle);
            joinCodeCard.setVisibility(View.GONE);
            ExtendedFloatingActionButton studentChatFab = emptyClassroomLayout.findViewById(R.id.student_chat_fab);
            studentChatFab.setVisibility(View.VISIBLE);

            studentChatFab.setOnClickListener(v -> openChat());
        }
    }

    private void showStudentList(boolean hasNewAnnouncements) {
        announcementsButtonLayout.setVisibility(View.VISIBLE);
        newAnnouncementsIndicator.setVisibility(hasNewAnnouncements ? View.VISIBLE : View.GONE);
        announcementsButton.setText(R.string.classroom_details_announcements_button_text);
        recyclerView.setVisibility(View.VISIBLE);
        chatFab.setVisibility(View.VISIBLE);
        emptyClassroomLayout.setVisibility(View.GONE);

        setupToolbarForStudentList();
        adapter.submitList(students);
    }

    private void setupToolbarForStudentList() {
        setToolbarTitle(className);

        if (userType.equals(AppConfig.UserType.TEACHER)) {
            inflateToolbarMenu(getIsOnlyClassroom() ?
                    R.menu.top_settings_add_menu :
                    R.menu.top_settings_menu);
            setToolbarMenuClickListener(this::handleTeacherMenuClick);
        } else {
            inflateToolbarMenu(getIsOnlyClassroom() ?
                    R.menu.top_minus_add_menu :
                    R.menu.top_minus_menu);
            setToolbarMenuClickListener(this::handleStudentMenuClick);
        }
    }

    private boolean handleTeacherMenuClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            openManageClassroom();
            return true;
        } else if (itemId == R.id.action_add) {
            openCreateClassroomDialog();
            return true;
        }
        return false;
    }

    private boolean handleStudentMenuClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_delete) {
            confirmLeaveClassroom();
            return true;
        } else if (itemId == R.id.action_add) {
            showJoinClassroomDialog();
            return true;
        }
        return false;
    }

    /**
     * Handles click on the delete button in the top app bar.
     */
    public void confirmLeaveClassroom() {
        if (userType.equals(AppConfig.UserType.STUDENT)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Leave Classroom")
                    .setMessage("Are you sure you want to leave this classroom?")
                    .setPositiveButton("Leave", (dialog, which) -> leaveClassroom())
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    /**
     * Leaves the current classroom.
     */
    private void leaveClassroom() {
        String url = AppConfig.BASE_URL + "/classroom/leave-classroom?classroomId=" + classroomId;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(getContext(), "Left classroom", Toast.LENGTH_SHORT).show();

                    // Pop back to classroom list
                    navController.popBackStack();
                    // Set refresh flag on current entry (classroom list) to trigger refresh
                    navController.getCurrentBackStackEntry()
                            .getSavedStateHandle()
                            .set("refresh", true);

                },
                error -> {
            showError("Failed to leave classroom");
            Log.e(TAG, "Error leaving classroom", error);
            Log.e(TAG, "Status code: " + error.networkResponse.statusCode);
            Log.e(TAG, "Response: " + new String(error.networkResponse.data));
                }
        );
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Opens the announcements list for the classroom.
     */
    private void openAnnouncements() {
        showLoading();
        ClassroomDetailsFragmentDirections.ActionToAnnouncementList action =
                ClassroomDetailsFragmentDirections.actionToAnnouncementList(
                        false,
                        classroomId,
                        className
                );
        navController.navigate(action);
    }

    /**
     * Opens the classroom chat.
     */
    private void openChat() {
        ClassroomDetailsFragmentDirections.ActionToClassroomChat action =
                ClassroomDetailsFragmentDirections.actionToClassroomChat(
                        classroomId,
                        className
                );
        navController.navigate(action);
    }

    /**
     * Opens the manage classroom screen.
     */
    private void openManageClassroom() {
        ClassroomDetailsFragmentDirections.ActionToManageClassroom action =
                ClassroomDetailsFragmentDirections.actionToManageClassroom(
                        classroomId,
                        className,
                        joinCode,
                        getIsOnlyClassroom()
                );
        navController.navigate(action);
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
                    Toast.makeText(getContext(), "Successfully joined classroom", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    navController.popBackStack();
                },
                error -> {
                    String errorMessage;
                    if (error.networkResponse != null) {
                        errorMessage = new String(error.networkResponse.data, StandardCharsets.UTF_8);

                        if (error.networkResponse.statusCode == 404 && errorMessage.trim().equals("Classroom not found")) {
                            joinCodeLayout.setError("Classroom not found");
                        } else {
                            showError("Failed to join classroom");
                            Log.e(TAG, "Error joining classroom", error);
                            Log.e(TAG, "Status code: " + error.networkResponse.statusCode);
                            Log.e(TAG, "Response: " + errorMessage);
                            dialog.dismiss();
                        }
                    } else {
                        showError("Failed to join classroom");
                        Toast.makeText(getContext(), "Network error occurred", Toast.LENGTH_LONG).show();
                    }
                });

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Handles click on a student in the list.
     *
     * @param student The clicked student
     */
    private void onTeacherStudentClick(StudentItem student) {
        Bundle args = new Bundle();
        args.putString("studentName", student.getStudentName());
        args.putInt("portfolioId", student.getPortfolioId());
        navController.navigate(R.id.action_to_student_portfolio, args);
    }

    /**
     * Handles click on the add button in the top app bar.
     */
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

    private void openCreateClassroomDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_classroom, null);

        TextInputLayout classNameLayout = dialogView.findViewById(R.id.classroom_name_layout);
        TextInputLayout balanceLayout = dialogView.findViewById(R.id.start_balance_layout);
        TextInputEditText classNameInput = dialogView.findViewById(R.id.classroom_name_input);
        TextInputEditText startBalanceInput = dialogView.findViewById(R.id.start_balance_input);

        // Build dialog
        AlertDialog dialog = builder.setTitle("Create Classroom")
                .setView(dialogView)
                .setPositiveButton("Create", null) // Set to null initially
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            if (classNameInput.getText() == null) {
                showToast("Please enter a classroom name");
                return;
            }

            String className = classNameInput.getText().toString().trim();
            String startBalance = Objects.requireNonNull(startBalanceInput.getText()).toString().trim();

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

            // Build URL
            StringBuilder urlBuilder = new StringBuilder(AppConfig.BASE_URL + "/classroom/create");
            urlBuilder.append("?name=").append(className);

            if (!startBalance.isEmpty()) {
                urlBuilder.append("&startingAmount=").append(startBalance.replace("$", ""));
            }

            // Create request
            StringRequest stringRequest = new StringRequest(Request.Method.POST, urlBuilder.toString(),
                    response -> {
                        showToast("Classroom created successfully");
                        dialog.dismiss();

                        navController.popBackStack();
                        // refresh classroom list after pop back to classroom list
                        Objects.requireNonNull(navController.getCurrentBackStackEntry())
                                .getSavedStateHandle()
                                .set("refresh", true);
                    },
                    error -> {
                        showError("Failed to create classroom");
                        Log.e(TAG, "Error creating classroom", error);
                    }
            );

            // Add request to queue
            VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest);
        }));

        dialog.show();
    }
//    @Override
//    public void onSaveInstanceState(@NonNull Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putParcelableArrayList("students", students);
//    }
}