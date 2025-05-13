package com.softdev.stocksim.ui.home.portfolio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.softdev.stocksim.ui.BaseFragment;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.api.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages a portfolio by creating, updating, and deleting it.
 *
 * @author Sheldon
 */
public class PortfolioManagerFragment extends BaseFragment {
    private static final String TAG = "PortfolioManagerFrag";

    private Button saveBTN, deleteBTN;
    private EditText nameTXT, cashTXT;
    private int portfolioId = -1; // Default to new portfolio
    private PortfolioModel portfolio;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            portfolio = getArguments().getParcelable("portfolio", PortfolioModel.class);
        }
    }

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_portfolio_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);

        if(portfolio != null){
            portfolioId = portfolio.getId();
            nameTXT.setText(portfolio.getName());
            cashTXT.setText(String.valueOf(portfolio.getCash()));
            saveBTN.setText("Update");
            deleteBTN.setVisibility(View.VISIBLE);
        }

        setupClickListeners();
        hideLoading();
    }

    /**
     * Initializes the views for the activity.
     */
    private void initializeViews(View view){
        saveBTN = view.findViewById(R.id.pmanage_save_btn);
        nameTXT = view.findViewById(R.id.pmanage_name_edit);
        cashTXT = view.findViewById(R.id.pmanage_cash_edit);
        deleteBTN = view.findViewById(R.id.pmanage_delete_btn);
    }

    /**
     * Sets up click listeners
     */
    private void setupClickListeners(){

        saveBTN.setOnClickListener(v -> {
            if (!nameTXT.getText().toString().isEmpty() && !cashTXT.getText().toString().isEmpty()) {
                if (portfolioId == -1) {
                    newPortfolioReq();
                } else {
                    updatePortfolioReq();
                }
            } else {
                showToast("Please fill out all fields");
            }
        });

        deleteBTN.setOnClickListener(v -> {
            deletePortfolioReq();
        });
    }

    /**
     * Tells the backend to delete the portfolio
     */
    private void deletePortfolioReq() {
        StringRequest stringRequest = new StringRequest(
                Request.Method.DELETE,
                AppConfig.BASE_URL + "/portfolio?id=" + portfolioId,
                response -> {
                    showToast("Portfolio deleted!");
                    navController.navigate(R.id.action_portfolioManagerFragment_to_homeFragment);
                },
                error -> {
                    Log.e("volley error: ", error.toString());
                    showError("An error occurred while deleting the portfolio.");
                }
        );
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest);
    }

    /**
     * Updates the current portfolio with new information.
     * Sends the user back to MainActivity
     */
    private void updatePortfolioReq() {
        String name = nameTXT.getText().toString();
        String cash = cashTXT.getText().toString();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("id", portfolio.getId());
            jsonBody.put("name", name);
            jsonBody.put("cash", Double.parseDouble(cash));
        } catch (JSONException e) {
            Log.e("JSON error: ", e.toString());
        }

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(
                Request.Method.PUT,
                AppConfig.BASE_URL + "/portfolio",
                response -> {
                    navigateBack();
                    showToast("Portfolio updated!");
                },
                error -> {
                    showToast(error.toString());
                    Log.e("Volley error: ", error.toString());
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                return jsonBody.toString().getBytes(StandardCharsets.UTF_8);
            }
        };

        // Adding request to request queue
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest);
    }

    /**
     * Creates a new portfolio with name and cash given.
     * Sends the user back
     */
    private void newPortfolioReq() {
        // Convert input to JSONObject
        String name = nameTXT.getText().toString();
        String cash = cashTXT.getText().toString();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("name", name);
            jsonBody.put("cash", Double.parseDouble(cash));
        } catch (JSONException e) {
            Log.e("JSON error: ", e.toString());
        }

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                AppConfig.BASE_URL + "/portfolio/create",
                response -> {
                    showToast("Portfolio created!");
                    navigateBack();
                },
                error -> {
                    showToast("An error occurred while creating the portfolio.");
                    Log.e("Volley error: ", error.toString());
                }) {

            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                return jsonBody.toString().getBytes(StandardCharsets.UTF_8);
            }
        };

        // Adding request to request queue
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest);
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

}