package com.softdev.stocksim.ui.classroom.management;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.ui.classroom.BaseClassroomFragment;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.api.VolleySingleton;

/**
 * Fragment for managing classroom settings.
 * Provides functionality for:
 * - Changing classroom name
 * - Adding/removing students
 * - Deleting classroom
 *
 * @author Blake Nelson
 */
public class ManageClassroomTeacherFragment extends BaseClassroomFragment {
    private static final String TAG = "ManageClassroomTeacherFragment";

    //State
    private String classroomId;
    private String classroomName;
    private String joinCode;
    private boolean classDetailsHaveChanged = false;

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classroom_manage, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ManageClassroomTeacherFragmentArgs args = ManageClassroomTeacherFragmentArgs
                .fromBundle(requireArguments());
        classroomId = args.getClassroomId();
        classroomName = args.getClassName();
        joinCode = args.getJoinCode();
        setIsOnlyClassroom(args.getIsOnlyClassroom());

        // Setup back press handling
        requireActivity().getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleCustomBackNavigation();
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        hideLoading();
    }

    /**
     * Initializes view references and sets up click listeners.
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        inflateToolbarMenu(R.menu.top_menu);
        // Initialize text input layouts
        TextInputLayout changeNameLayout = view.findViewById(R.id.change_classroom_name_layout);
        TextInputLayout addStudentLayout = view.findViewById(R.id.add_student_layout);
        TextInputLayout removeStudentLayout = view.findViewById(R.id.remove_student_layout);

        // Initialize edit texts
        TextInputEditText changeNameInput = view.findViewById(R.id.change_classroom_name_input);
        TextInputEditText addStudentInput = view.findViewById(R.id.add_student_input);
        TextInputEditText removeStudentInput = view.findViewById(R.id.remove_student_input);

        // Initialize buttons
        Button changeNameButton = view.findViewById(R.id.change_classroom_name_button);
        Button addStudentButton = view.findViewById(R.id.add_student_button);
        Button removeStudentButton = view.findViewById(R.id.remove_student_button);
        Button removeClassroomButton = view.findViewById(R.id.remove_classroom_button);

        changeNameInput.setText(classroomName);

        changeNameButton.setOnClickListener(v -> {
            changeNameLayout.setError(null);

            if (changeNameInput.getText() == null || changeNameInput.getText().toString().trim().isEmpty()) {
                changeNameLayout.setError("Please enter a classroom name");
                return;
            }

            String newClassName = changeNameInput.getText().toString().trim();

            if (newClassName.equals(classroomName)) {
                changeNameLayout.setError("New classroom name cannot be the same as the current name");
                return;
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Change Classroom Name")
                    .setMessage("Are you sure you want to change the classroom name to: "
                            + newClassName + "?")
                    .setPositiveButton("Change", (dialog, which) -> changeClassName(newClassName))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        addStudentButton.setOnClickListener(v -> {

            addStudentLayout.setError(null);

            if (addStudentInput.getText() == null || addStudentInput.getText().toString().trim().isEmpty()) {
                addStudentLayout.setError("Please enter a student username");
                return;
            }

            String studentUsername = addStudentInput.getText().toString().trim();

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add Student")
                    .setMessage("Add student '" + studentUsername + "' to classroom " + classroomName + "?")
                    .setPositiveButton("Add", (dialog, which) -> {
                        addStudent(studentUsername);
                        addStudentInput.setText("");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        removeStudentButton.setOnClickListener(v -> {
            removeStudentLayout.setError(null);

            if (removeStudentInput.getText() == null || removeStudentInput.getText().toString().trim().isEmpty()) {
                removeStudentLayout.setError("Please enter a student username");
                return;
            }

            String studentUsername = removeStudentInput.getText().toString().trim();

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Remove Student")
                    .setMessage("Are you sure you want to remove " + studentUsername + " from classroom "
                            + classroomName + "?")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        removeStudent(studentUsername, classroomId);
                        removeStudentInput.setText("");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        removeClassroomButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Classroom")
                .setMessage("Are you sure you want to delete " + classroomName
                        + "?\n This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteClassroom(classroomId))
                .setNegativeButton("Cancel", null)
                .show());
    }

    /**
     * Change classroom name
     *
     * @param newName The new name for the classroom
     */
    private void changeClassName(String newName) {
        String url = AppConfig.BASE_URL + "/classroom/change-name?classroomId=" + classroomId +
                "&newName=" + newName;

        StringRequest request = new StringRequest(Request.Method.PUT, url,
                response -> {
                    Toast.makeText(getContext(), "Classroom name updated successfully",
                            Toast.LENGTH_SHORT).show();
                    classroomName = newName;
                    classDetailsHaveChanged = true;

                    // Set both refresh flag and new class name in saved state
                    if (navController.getPreviousBackStackEntry() != null) {
                        navController.getPreviousBackStackEntry()
                                .getSavedStateHandle()
                                .set("refresh", true);
                        navController.getPreviousBackStackEntry()
                                .getSavedStateHandle()
                                .set("updatedClassName", newName);
                    }
                },
                error -> {
                    String message;
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        String errorMessage = new String(error.networkResponse.data);
                        if (errorMessage.contains("Teacher not found")) {
                            message = "Teacher not found";
                        } else if (errorMessage.contains("Classroom not found")) {
                            message = "Classroom not found";
                        } else {
                            message = "Error updating classroom name";
                        }
                    } else {
                        message = "Failed to update classroom name";
                    }
                    showError(message);
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Add student to classroom
     *
     * @param studentUsername The username of the student to add
     */
    private void addStudent(String studentUsername) {
        String url = AppConfig.BASE_URL + "/classroom/add-student?joinCode=" + joinCode +
                "&studentName=" + studentUsername;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(getContext(), "Student added successfully", Toast.LENGTH_SHORT).show();
                    classDetailsHaveChanged = true;
                    // Update details screen
                    if (navController.getPreviousBackStackEntry() != null) {
                        navController.getPreviousBackStackEntry()
                                .getSavedStateHandle()
                                .set("refresh", true);
                    }
                },
                error -> {
                    String message;
                    if (error.networkResponse != null) {
                        switch (error.networkResponse.statusCode) {
                            case 404:
                                String errorMessage = new String(error.networkResponse.data);
                                if (errorMessage.contains("Teacher not found")) {
                                    message = "Teacher not found";
                                } else if (errorMessage.contains("Classroom not found")) {
                                    message = "Classroom not found";
                                } else if (errorMessage.contains("Student not found")) {
                                    message = "Student not found";
                                } else {
                                    message = "Error adding student";
                                }
                                break;
                            case 403:
                                message = "You do not own this classroom";
                                break;
                            case 400:
                                message = "Student is already in this classroom";
                                break;
                            default:
                                message = "Failed to add student";
                                break;
                        }
                    } else {
                        message = "Network error occurred";
                    }
                    showError(message);
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Remove student from classroom
     *
     * @param studentUsername The username of the student to remove
     * @param classroomId The ID of the classroom to remove the student from
     */
    private void removeStudent(String studentUsername, String classroomId) {
        String url = AppConfig.BASE_URL + "/classroom/delete-student-by-teacher?studentName=" + studentUsername + "&classroomId=" + classroomId;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(getContext(), "Student removed successfully", Toast.LENGTH_SHORT).show();
                    classDetailsHaveChanged = true;
                    // Update details screen
                    if (navController.getPreviousBackStackEntry() != null) {
                        navController.getPreviousBackStackEntry()
                                .getSavedStateHandle()
                                .set("refresh", true);
                    }

                },
                error -> {
                    String message;
                    if (error.networkResponse != null) {
                        switch (error.networkResponse.statusCode) {
                            case 404:
                                // Parse the error message from response to determine which 404 case it is
                                String errorMessage = new String(error.networkResponse.data);
                                if (errorMessage.contains("Teacher not found")) {
                                    message = "Teacher not found";
                                } else if (errorMessage.contains("Classroom not found")) {
                                    message = "Classroom not found for this teacher";
                                } else if (errorMessage.contains("Student not found")) {
                                    message = "Student not found";
                                } else {
                                    message = "Error removing student";
                                }
                                break;
                            case 400:
                                message = "Student is not in the classroom";
                                break;
                            default:
                                Log.e(TAG, "Error removing student: " + error.networkResponse.statusCode);
                                Log.e(TAG, "Error message: " + new String(error.networkResponse.data));
                                message = "Failed to remove student";
                                break;
                        }
                    } else {
                        message = "Network error occurred";
                    }
                    showError(message);
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Delete classroom
     *
     * @param classroomId The ID of the classroom to delete
     */
    private void deleteClassroom(String classroomId) {
        String url = AppConfig.BASE_URL + "/classroom/delete?classroomId=" + classroomId;

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(getContext(), "Classroom deleted successfully", Toast.LENGTH_SHORT).show();
                    navController.popBackStack(R.id.classroomListFragment, true);
                    navController.navigate(R.id.classroomListFragment);

                    // Then clear the entire back stack up to classroom list
                    navController.popBackStack(R.id.classroomListFragment, true);

                    // Navigate to classroom list as a new destination
                    navController.navigate(R.id.classroomListFragment);
                },
                error -> {
                    String message = "Failed to delete classroom";
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        message = "Classroom not found";
                    }
                    showError(message);
                }
        );

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    private void handleCustomBackNavigation() {
        if (classDetailsHaveChanged && navController.getPreviousBackStackEntry() != null) {
            navController.getPreviousBackStackEntry()
                    .getSavedStateHandle()
                    .set("refresh", true);
        }
        handleBackNavigation();
    }

    /**
     * Clears navigation state and triggers a refresh for both current and list fragments
     */
    private void clearNavigationState() {
        // Set refresh flag for current backstack entry
        if (navController.getCurrentBackStackEntry() != null) {
            navController.getCurrentBackStackEntry()
                    .getSavedStateHandle()
                    .set("refresh", true);
        }

        // Set refresh flag for classroom list
        try {
            navController.getBackStackEntry(R.id.classroomListFragment)
                    .getSavedStateHandle()
                    .set("refresh", true);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Classroom list not in back stack");
        }
    }

    // Override handleBackNavigation to include state management
    @Override
    protected boolean handleBackNavigation() {
        if (classDetailsHaveChanged) {
            // If changes were made, clear state before navigating
            clearNavigationState();
        }
        return super.handleBackNavigation();
    }
}