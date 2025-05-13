package com.softdev.stocksim;

import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import com.softdev.stocksim.auth.AuthActivity;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class BlakeSystemTest {

    String email = "blake.testing@blake.blake";
    String usernameAndPassword = "blaketestingpassword";
    @Rule
    public ActivityScenarioRule<AuthActivity> activityScenarioRule
            = new ActivityScenarioRule<>(AuthActivity.class);

    @Test
    public void test1_register() {
        // click on the Dashboard navigation item
        onView(withId(R.id.welcome_start_btn))
                .perform(click());

        onView(withId(R.id.welcome_signup_btn))
                .perform(click());

        onView(withId(R.id.registration_email_input))
                .perform(typeText(email));

        onView(withId(R.id.registration_username_input))
                .perform(typeText(usernameAndPassword));

        onView(withId(R.id.registration_password_input))
                .perform(typeText(usernameAndPassword));

        onView(withId(R.id.registration_confirm_password_input))
                .perform(typeText(usernameAndPassword));


        onView(withId(R.id.teacher_radio_button))
                .perform(click());

        closeSoftKeyboard();

        onView(withId(R.id.registration_register_register_btn))
                .perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {}

        onView(withId(R.id.landing_portfolio_recycler))
                .check(matches(isDisplayed()));
    }

    @Test
    public void test2_teacherCreateClassroom() {
        // Navigate to classroom section
        onView(withId(R.id.classroomContainerFragment))
                .perform(click());

        // Click create classroom button
        onView(withId(R.id.no_classrooms_button)).perform(click());

        // Fill in classroom details
        onView(withId(R.id.classroom_name_input)).perform(typeText("Test Class"));
        onView(withId(R.id.start_balance_input)).perform(typeText("10000"));

        // Create the classroom
        onView(withText("Create")).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        onView(withId(R.id.empty_classroom_layout))
                .check(matches(isDisplayed()));
    }

    @Test
    public void test3_changeClassroomName() {

        String newClassName = "New name";
        // Navigate to classroom section
        onView(withId(R.id.classroomContainerFragment)).perform(click());

        // Click join classroom button
        onView(withId(R.id.action_settings)).perform(click());

        // Clear text
        onView(withId(R.id.change_classroom_name_input))
                .perform(clearText());

        // Enter new class name
        onView(withId(R.id.change_classroom_name_input)).perform(typeText(newClassName));

        closeSoftKeyboard();

        onView(withId(R.id.change_classroom_name_button))
                .perform(click());

        onView(withText("Change"))
                .perform(click());

        // Controlled swipe with specific parameters
        onView(withId(android.R.id.content))
                .perform(new GeneralSwipeAction(
                        Swipe.FAST,
                        GeneralLocation.CENTER_LEFT,
                        GeneralLocation.CENTER_RIGHT,
                        Press.FINGER));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        // Check if top app bar had its name set to the new name
        onView(withId(R.id.topAppBar))
                .check(matches(hasDescendant(withText(newClassName))));
    }

    @Test
    public void test_4deleteAccount() throws InterruptedException {
        Thread.sleep(1000);

        onView(withId(R.id.settingsFragment))
                .perform(click());

        onView(withId(R.id.settings_delete_account_button))
                .perform(click());

        onView(withId(R.id.password_edit))
                .perform(typeText(usernameAndPassword));

        onView(withText("Permanently Delete"))
                .perform(click());

        Thread.sleep(500);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {}

        onView(withId(R.id.welcome_start_btn))
                .check(matches(isDisplayed()));
    }

}
