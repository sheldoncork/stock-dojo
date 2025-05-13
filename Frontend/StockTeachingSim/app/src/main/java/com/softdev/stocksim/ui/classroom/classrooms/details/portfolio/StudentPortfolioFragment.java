package com.softdev.stocksim.ui.classroom.classrooms.details.portfolio;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.softdev.stocksim.R;
import com.softdev.stocksim.api.VolleySingleton;
import com.softdev.stocksim.ui.classroom.BaseClassroomFragment;
import com.softdev.stocksim.ui.home.portfolio.PortfolioModel;
import com.softdev.stocksim.ui.home.stock.StockRecyclerAdapter;
import com.softdev.stocksim.utils.AppConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Fragment that displays a student's portfolio.
 *
 * @author Blake Nelson
 */
public class StudentPortfolioFragment extends BaseClassroomFragment {

    private TextView noStocks;
    private TextView cashText;
    private TextView valueText;
    private RecyclerView stocksRecyclerView;
    private StockRecyclerAdapter adapter;

    private String studentName;
    private int portfolioId;

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_portfolio, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get arguments passed from classroom details
        StudentPortfolioFragmentArgs args = StudentPortfolioFragmentArgs.fromBundle(requireArguments());
        studentName = args.getStudentName();
        portfolioId = args.getPortfolioId();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        fetchPortfolioData();
    }

    private void initializeViews(View view) {
        setToolbarTitle(studentName + "'s Portfolio");
        noStocks = view.findViewById(R.id.no_socks_text);
        cashText = view.findViewById(R.id.student_portfolio_cash);
        valueText = view.findViewById(R.id.student_portfolio_value);
        stocksRecyclerView = view.findViewById(R.id.student_portfolio_stocks);

        stocksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StockRecyclerAdapter(requireContext(), new ArrayList<>());
        stocksRecyclerView.setAdapter(adapter);
    }

    private void fetchPortfolioData() {
        showLoading();
        String url = AppConfig.BASE_URL + "/portfolio?id=" + portfolioId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        updatePortfolioUI(response);
                        hideLoading();
                    } catch (JSONException e) {
                        showError("Error parsing portfolio data");
                    }
                },
                error -> {
                    hideLoading();
                    showError("Failed to fetch portfolio data");
                });

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    private void updatePortfolioUI(JSONObject response) throws JSONException {
        int id = response.getInt("id");
        String name = response.getString("name");
        double cash = response.getDouble("cash");
        double value = response.getDouble("value");
        JSONArray stocks = response.getJSONArray("stocks");

        // Create portfolio model using existing class
        PortfolioModel portfolio = new PortfolioModel(id, name, cash, value, stocks);

        // Update UI
        cashText.setText(String.format("Cash: $%.2f", portfolio.getCash()));
        valueText.setText(String.format("Total Value: $%.2f", portfolio.getValue()));

        if (stocks.length() == 0)  {
            stocksRecyclerView.setVisibility(View.GONE);
            noStocks.setVisibility(View.VISIBLE);
        }

        // Update recycler view using existing adapter
        if (portfolio.getStocks() != null) {
            adapter = new StockRecyclerAdapter(requireContext(), portfolio.getStocks());
            stocksRecyclerView.setAdapter(adapter);
        }
    }
}