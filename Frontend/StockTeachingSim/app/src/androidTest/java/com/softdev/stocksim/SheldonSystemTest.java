package com.softdev.stocksim;

import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.assertion.ViewAssertions;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.doubleClick;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.view.KeyEvent;

import com.softdev.stocksim.auth.AuthActivity;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class SheldonSystemTest {

    @Rule
    public ActivityScenarioRule<AuthActivity> activityScenarioRule
            = new ActivityScenarioRule<>(AuthActivity.class);

    @Test
    public void test1Login() {
        // click on the Dashboard navigation item
        onView(withId(R.id.welcome_start_btn))
                .perform(click());

        onView(withId(R.id.welcome_login_btn))
                .perform(click());

        onView(withId(R.id.login_username_input))
                .perform(typeText("sheldon"));

        onView(withId(R.id.login_password_input))
                .perform(typeText("password"));

        onView(withId(R.id.login_login_btn))
                .perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        onView(withId(R.id.landing_portfolio_recycler))
                .check(ViewAssertions.matches(isDisplayed()));
    }

    @Test
    public void test2Portfolios() throws InterruptedException {
        Thread.sleep(1000);
        onView(withId(R.id.landing_portfolio_recycler))
                .perform(click());

        Thread.sleep(200);
        onView(withId(R.id.action_settings))
                .check(ViewAssertions.matches(isDisplayed()));
    }

    @Test
    public void test3PortfolioManager() throws InterruptedException {
        Thread.sleep(3000);
        // create a portfolio
        onView(withId(R.id.action_add))
                .perform(click());

        onView(withId(R.id.pmanage_name_edit))
                .perform(typeText("Test Portfolio"));

        Thread.sleep(100);
        onView(withId(R.id.pmanage_cash_edit))
                .perform(typeText("10000"));

        onView(withId(R.id.pmanage_save_btn))
                .perform(click());

        // go to portfolio manager then edit
        Thread.sleep(3000);

        onView(withId(R.id.landing_portfolio_recycler))
                .perform(click());

        onView(withId(R.id.action_settings))
                .perform(click());

        onView(withId(R.id.pmanage_name_edit))
                .perform(typeText("TesT"));

        onView(withId(R.id.pmanage_cash_edit))
                .perform(typeText("500"));

        onView(withId(R.id.pmanage_save_btn))
                .perform(click());

        // go to portfolio manager then delete

        Thread.sleep(500);

        onView(withId(R.id.action_settings))
                .perform(click());

        onView(withId(R.id.pmanage_delete_btn))
                .perform(click());

    }

    @Test
    public void test4Search() throws InterruptedException {
        onView(withId(R.id.searchFragment))
                .perform(click());

        onView(withId(R.id.search_view))
                .perform(typeText("A"))
                .perform(pressKey(KeyEvent.KEYCODE_ENTER));

        Thread.sleep(2000);

        onView(withId(R.id.search_stock_recycler))
                .check(ViewAssertions.matches(isDisplayed()));

        // Thread.sleep(50);

        onView(withId(R.id.search_stock_recycler))
                .perform(doubleClick());

        Thread.sleep(1500);

        onView(withId(R.id.stock_name_tv))
                .check(ViewAssertions.matches(isDisplayed()));
    }

    @Test
    public void test5DeleteAccount() throws InterruptedException {
        Thread.sleep(1000);

        onView(withId(R.id.settingsFragment))
                .perform(click());

        onView(withId(R.id.delete_account_button))
                .perform(click());

        Thread.sleep(500);
        onView(withId(R.id.light_theme_radio))
                .perform(click());

        Thread.sleep(500);
        onView(isAssignableFrom(NestedScrollView.class))
                .perform(swipeUp());
    }

    @Test
    public void test6News() throws InterruptedException {
        onView(withId(R.id.market_news))
                .perform(click());

        Thread.sleep(1000);

        onView(withId(R.id.news_recycler))
                .check(ViewAssertions.matches(isDisplayed()))
                        .perform(pressBack());

        onView(withId(R.id.searchFragment))
                .perform(click());

        onView(withId(R.id.search_view))
                .perform(typeText("AAPL"));

        Thread.sleep(3000);

        onView(withId(R.id.search_list))
                .perform(click());

        Thread.sleep(300);

        onView(withId(R.id.stock_news))
                .perform(click());

        Thread.sleep(500);

        onView(withId(R.id.news_recycler))
                .check(ViewAssertions.matches(isDisplayed()));

    }
}