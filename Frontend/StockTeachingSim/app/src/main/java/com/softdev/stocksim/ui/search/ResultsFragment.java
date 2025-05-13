package com.softdev.stocksim.ui.search;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.highsoft.highcharts.common.HIColor;
import com.highsoft.highcharts.common.hichartsclasses.*;
import com.highsoft.highcharts.core.*;
import com.google.android.material.textfield.TextInputLayout;
import com.softdev.stocksim.ui.BaseFragment;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.api.VolleySingleton;
import com.softdev.stocksim.ui.home.portfolio.PortfolioModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Displays stock results
 *
 * @author Sheldon
 */
public class ResultsFragment extends BaseFragment {

    // Constants
    private final String TAG = "ResultsFragment";
    private static final int MAX_SHARES = 100;
    private static final int MIN_SHARES = 1;

    // View Variables
    private TextView stockNameTV, descriptionTV, currentValueTV, portfolioCashTV, stockQuantityTV, totalValueTV;
    private TextView maxPurchaseTV;
    private AutoCompleteTextView dropdown;
    private MaterialButtonToggleGroup buySellToggle;
    private MaterialButton buyButton, sellButton, actionButton, newsButton;
    private NumberPicker numberPicker;
    private View buyContainer, sellContainer;
    private HIChartView chartView;
    private ProgressBar progressBar;
    private TextInputLayout dropdownLayout;

    // Data Variables
    private String stockSymbol;
    private double currentPrice;
    private double yearHigh, yearLow;
    private final ArrayList<PortfolioModel> portfolios = new ArrayList<>();
    private PortfolioModel selectedPortfolio;

    // Add at the top with other variables
    private boolean stockDataLoaded = false;
    private boolean portfolioDataLoaded = false;

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_results, container, false);
    }

    /**
     * Setups views and click listeners
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeFragment(view);
    }

    // Initialization Methods
    private void initializeFragment(View view) {
        setupToolbar();
        initViews(view);
        initializeData();
        setupListeners();
    }

    private void initializeData() {
        Bundle args = getArguments();
        if (args != null) {
            stockSymbol = args.getString("stockSymbol", "TSLA");
            selectedPortfolio = args.getParcelable("portfolio", PortfolioModel.class);
        }
        if (selectedPortfolio == null) { // null if no portfolio passed in
            fetchPortfolioData();
        } else {
            dropdownLayout.setVisibility(View.GONE);
        }

        fetchGraphData();
        stockNameTV.setText(stockSymbol);
        fetchStock(stockSymbol);

        checkAllDataLoaded();
    }

    private void checkAllDataLoaded() {
        if (stockDataLoaded && selectedPortfolio != null) {
            hideLoading();
                // Update the portfolio display and trading states
                portfolioCashTV.setText(String.format("$%.2f", selectedPortfolio.getCash()));
                updateToggleStates();
        }
    }

    /**
     * Sets up the toolbar
     */
    private void setupToolbar() {
        inflateToolbarMenu(R.menu.top_menu);
    }

    /**
     * Initializes UI elements
     *
     * @param view current view
     */
    private void initViews(View view) {
        // Stock Info Views
        stockNameTV = view.findViewById(R.id.stock_name_tv);
        descriptionTV = view.findViewById(R.id.results_description_tv);
        currentValueTV = view.findViewById(R.id.current_value_tv);
        newsButton = view.findViewById(R.id.stock_news);

        // Portfolio Selection Views
        dropdownLayout = view.findViewById(R.id.dropdown_layout);
        dropdown = view.findViewById(R.id.portfolio_auto_complete);

        // Trading Info Views
        portfolioCashTV = view.findViewById(R.id.results_cash);
        maxPurchaseTV = view.findViewById(R.id.max_purchase_tv);
        stockQuantityTV = view.findViewById(R.id.results_stock_quantity);
        totalValueTV = view.findViewById(R.id.total_value);

        // Trading Control Views
        buyContainer = view.findViewById(R.id.buy_container);
        sellContainer = view.findViewById(R.id.sell_container);
        buySellToggle = view.findViewById(R.id.buy_sell_toggle);
        buyButton = view.findViewById(R.id.toggle_buy);
        sellButton = view.findViewById(R.id.toggle_sell);
        actionButton = view.findViewById(R.id.action_button);

        // Number Picker Setup
        numberPicker = view.findViewById(R.id.quantity_picker);
        numberPicker.setMinValue(MIN_SHARES);
        numberPicker.setMaxValue(MAX_SHARES);
        numberPicker.setValue(MIN_SHARES);

        // Graph
        chartView = view.findViewById(R.id.hchart);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        buySellToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateTradeMode(checkedId == R.id.toggle_buy);
            }
        });

        actionButton.setOnClickListener(v -> {
            int amount = numberPicker.getValue();
            boolean isBuyMode = buySellToggle.getCheckedButtonId() == R.id.toggle_buy;
            handleTrade(amount, isBuyMode);
        });

        numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateTotalValue(newVal));

        newsButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("stockSymbol", stockSymbol);
            navController.navigate(R.id.action_resultsFragment_to_newsFragment, args);
        });
    }

    private void updateToggleStates() {
        boolean canBuy = selectedPortfolio.getCash() >= currentPrice;
        boolean canSell = selectedPortfolio.containsStock(stockSymbol) > 0;

        int currentCheckedId = buySellToggle.getCheckedButtonId();

        portfolioCashTV.setText(String.format("$%.2f", selectedPortfolio.getCash()));

        // If both actions are disabled, clear selection and disable group
        if (!canBuy && !canSell) {
            disableTrading();
            return;
        }

        if (canBuy && !canSell) {
            sellButton.setEnabled(false);
            buyButton.setEnabled(true);
            buySellToggle.check(R.id.toggle_buy);

        } else if (!canBuy) {
            buyButton.setEnabled(false);
            sellButton.setEnabled(true);
            buySellToggle.check(R.id.toggle_sell);
            // can buy and sell and (both buttons got enabled
        } else {
            // Enable/disable buttons based on capabilities
            buyButton.setEnabled(true);
            sellButton.setEnabled(true);
            if (currentCheckedId == R.id.toggle_buy) {
                buySellToggle.check(R.id.toggle_sell);
                buySellToggle.check(R.id.toggle_buy);
            } else {
                buySellToggle.check(R.id.toggle_buy);
                buySellToggle.check(R.id.toggle_sell);
            }
        }
    }

    private void updateTradeMode(boolean isBuyMode) {
        if (selectedPortfolio == null || currentPrice <= 0) {
            disableTrading();
            return;
        }

        if (isBuyMode) {
            buyContainer.setVisibility(View.VISIBLE);
            sellContainer.setVisibility(View.GONE);
            actionButton.setText("Buy");
            int maxAffordableShares = (int) (selectedPortfolio.getCash() / currentPrice);
            maxPurchaseTV.setText(maxAffordableShares + " shares");
        } else {
            buyContainer.setVisibility(View.GONE);
            sellContainer.setVisibility(View.VISIBLE);
            int ownedShares = selectedPortfolio.containsStock(stockSymbol);
            stockQuantityTV.setText(String.format("x%d shares", ownedShares));
            actionButton.setText("Sell");
        }

        // Update appropriate information based on mode
        updateNumberPicker(isBuyMode);
        actionButton.setEnabled(true);
    }

    private void updateNumberPicker(boolean isBuyMode){
        if (currentPrice <= 0) {
            disableTrading();
            return;
        }

        numberPicker.setEnabled(true);
        int maxAffordableShares = (int) (selectedPortfolio.getCash() / currentPrice);
        int ownedShares = selectedPortfolio.containsStock(stockSymbol);

        int max;
        if (isBuyMode) {
            max = Math.min(maxAffordableShares, MAX_SHARES);
        } else {
            max = Math.min(ownedShares, MAX_SHARES);
        }
        numberPicker.setMaxValue(max);
        numberPicker.setValue(MIN_SHARES);
        updateTotalValue(numberPicker.getValue());
    }

    private void updateTotalValue(int quantity) {
        double total = currentPrice * quantity;
        totalValueTV.setText(String.format("$%.2f", total));
    }

    private void disableTrading() {
        buySellToggle.clearChecked();
        buyButton.setEnabled(false);
        sellButton.setEnabled(false);
        actionButton.setEnabled(false);
        numberPicker.setMaxValue(0);
        numberPicker.setEnabled(false);
    }

    // Trading Methods
    private void handleTrade(int amount, boolean isBuyMode) {
        if (isBuyMode) {
            handleBuy(amount);
        } else {
            handleSell(amount);
        }
    }

    private void handleBuy(int amount) {
        double totalCost = currentPrice * amount;
        if (selectedPortfolio.getCash() >= totalCost) {
            selectedPortfolio.buyStock(requireContext(), stockSymbol, currentPrice, amount,
                    () -> {
                        updateToggleStates();
                        showToast(String.format("Successfully bought %d shares of %s", amount, stockSymbol));
                    });
        } else {
            showToast("Insufficient funds for this purchase");
        }
    }

    private void handleSell(int amount) {
        int stockQuantity = selectedPortfolio.containsStock(stockSymbol);
        if (stockQuantity >= amount) {
            selectedPortfolio.sellStock(requireContext(), stockSymbol, currentPrice, amount,
                    () -> {
                        updateToggleStates();
                        showToast(String.format("Successfully sold %d shares of %s", amount, stockSymbol));
                    });
        } else {
            showToast("Insufficient shares for this sale");
        }
    }

    /**
     * GET to fetch stock data from the backend
     *
     * @param stockSymbol - the stock symbol to fetch
     */
    private void fetchStock(String stockSymbol) {
        showLoading();
        String url = AppConfig.BASE_URL + "/stock/info?symbol=" + stockSymbol.trim();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                this::handleStockResponse,
                error -> {
                    Log.e(TAG, error.toString(), error);
                });
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * @param response Takes in a JSONObject containing stock data
     */
    private void handleStockResponse(JSONObject response) {
        Log.d(TAG, "Stock data received: " + response.toString());
        try {
            currentPrice = response.getDouble("currentPrice");
            yearHigh = response.getDouble("yearHigh");
            yearLow = response.getDouble("yearLow");

            descriptionTV.setText(String.format("Year Low: $%.2f - Year High: $%.2f", yearLow, yearHigh));
            currentValueTV.setText("Current Value: $" + currentPrice);

            stockDataLoaded = true;
            checkAllDataLoaded();

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing stock JSON", e);
            showToast("Error processing stock data");
        }
    }

    /**
     * Fetch portfolio data from the server
     * Nearly identical to HomeFragment's fetchPortfolioData() & response
     */
    private void fetchPortfolioData() {
        showLoading();
        String url = AppConfig.BASE_URL + "/portfolio/all";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                this::handlePortfolioResponse,
                error -> {
                    Log.e(TAG, "Error fetching portfolio data", error);
                    showToast("Error fetching portfolio data. Try again soon.");
                });

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Converts the portfolio data into a list of PortfolioModel objects
     */
    private void handlePortfolioResponse(JSONArray response) {
        Log.d(TAG, "Portfolio data received: " + response.toString());

        portfolios.clear();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject jsonObject = response.getJSONObject(i);
                PortfolioModel portfolio = new PortfolioModel(
                        jsonObject.getInt("id"),
                        jsonObject.getString("name"),
                        jsonObject.getDouble("cash"),
                        jsonObject.getDouble("value"),
                        jsonObject.getJSONArray("stocks")
                );
                portfolios.add(portfolio);
            }
            setupDropdown();
            setSelectedPortfolio();

            portfolioDataLoaded = true;
            checkAllDataLoaded();
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing portfolio JSON", e);
            showToast("Error processing portfolio data");
        }
    }

    /**
     * Populates autocomplete with portfolios,
     * checks for ownership of stock then displays sell button
     */
    private void setupDropdown() {
        String[] portfolioNames = new String[portfolios.size()];
        for (int i = 0; i < portfolios.size(); i++) {
            portfolioNames[i] = portfolios.get(i).getName();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_portfolios, portfolioNames);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemClickListener((parent, view1, i, id) -> {
            selectedPortfolio = portfolios.get(i);
            updateToggleStates();
        });
    }

    /**
     * Preset dropdown to portfolio first in list
     */
    private void setSelectedPortfolio(){
        if (!portfolios.isEmpty()) {
            dropdown.setText(portfolios.get(0).getName(), false);
            selectedPortfolio = portfolios.get(0);
        } else {
            showToast("You do not have any portfolios");
            numberPicker.setVisibility(View.GONE);
            dropdown.setVisibility(View.GONE);
            actionButton.setEnabled(false);
            buySellToggle.setEnabled(false);
        }
    }

    // HighChart

    /**
     * Fetches historical data for a stock
     * [{"date": "string","open": 0,"high": 0,"low": 0,"close": 0}]
     */
    private void fetchGraphData() {
        String url = AppConfig.BASE_URL + "/stock/historical?symbol=" + stockSymbol;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                this::handleGraphResponse,
                error -> {
                    Log.e(TAG, "Error fetching historical data", error);
                    progressBar.setVisibility(View.GONE);
                    chartView.setVisibility(View.GONE);
                    showToast("Error fetching graph data.");
                });
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    private void handleGraphResponse(JSONArray response) {
        Log.d(TAG, "Graph data received: " + response.toString());

        ArrayList<String> dates = new ArrayList<>();
        ArrayList<Double> closes = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            try {
                JSONObject jsonObject = response.getJSONObject(i);
                String date = jsonObject.getString("date");
                Double close = jsonObject.getDouble("close");

                dates.add(date);
                closes.add(close);
            }
            catch (JSONException e) {
                Log.e(TAG, "Error parsing graph JSON", e);
                showToast("Error processing graph data");
            }
        }
        createChart(dates, closes);
    }

    private void createChart(ArrayList<String> dates, ArrayList<Double> closes) {
        HIOptions options = new HIOptions();

        // Get theme colors
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
        int backgroundColor = typedValue.data;

        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        int primaryColor = typedValue.data;

        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true);
        int onBackgroundColor = typedValue.data;

        // Convert colors to HIColor and hex string
        HIColor backgroundHiColor = HIColor.initWithRGBA(
                Color.red(backgroundColor),
                Color.green(backgroundColor),
                Color.blue(backgroundColor),
                Color.alpha(backgroundColor)
        );

        HIColor lineHiColor = HIColor.initWithRGBA(
                Color.red(primaryColor),
                Color.green(primaryColor),
                Color.blue(primaryColor),
                255
        );

        // Convert onBackgroundColor to hex string for text
        String textColorHex = String.format("#%06X", (0xFFFFFF & onBackgroundColor));

        // Chart configuration
        HIChart chart = new HIChart();
        chart.setType("line");
        chart.setBackgroundColor(backgroundHiColor);
        options.setChart(chart);

        // Title
        HITitle title = new HITitle();
        title.setText("Stock Price Over Time");
        title.setStyle(new HICSSObject());
        title.getStyle().setColor(textColorHex);
        options.setTitle(title);

        // X-Axis
        HIXAxis xAxis = new HIXAxis();
        xAxis.setType("datetime");
        xAxis.setLabels(new HILabels());
        xAxis.getLabels().setStyle(new HICSSObject());
        xAxis.getLabels().getStyle().setColor(textColorHex);

        // Y-Axis
        HIYAxis yAxis = new HIYAxis();
        HITitle yAxisTitle = new HITitle();
        yAxisTitle.setText("Price");
        yAxisTitle.setStyle(new HICSSObject());
        yAxisTitle.getStyle().setColor(textColorHex);
        yAxis.setTitle(yAxisTitle);
        yAxis.setLabels(new HILabels());
        yAxis.getLabels().setStyle(new HICSSObject());
        yAxis.getLabels().getStyle().setColor(textColorHex);

        // Legend styling
        HILegend legend = new HILegend();
        legend.setItemStyle(new HICSSObject());
        legend.getItemStyle().setColor(textColorHex);
        options.setLegend(legend);

        // Apply axes
        options.setXAxis(new ArrayList<>(Collections.singletonList(xAxis)));
        options.setYAxis(new ArrayList<>(Collections.singletonList(yAxis)));

        // Prepare series data
        ArrayList<HIData> seriesData = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            long timestamp = convertDateToTimestamp(dates.get(i));
            HIData data = new HIData();
            data.setX(timestamp);
            data.setY(closes.get(i));
            seriesData.add(data);
        }

        // Configure series
        HILine series = new HILine();
        series.setData(seriesData);
        series.setName("Stock Closing Price");
        series.setColor(lineHiColor);
        options.setSeries(new ArrayList<>(Collections.singletonList(series)));

        // Disable exporting
        HIExporting exporting = new HIExporting();
        exporting.setEnabled(false);
        options.setExporting(exporting);

        // Apply options and show chart
        chartView.setOptions(options);
        progressBar.setVisibility(View.GONE);
        chartView.setVisibility(View.VISIBLE);
        // updateToggleStates();
    }

    // Helper method to convert date string to milliseconds from UNIX time
    @SuppressLint("SimpleDateFormat")
    private long convertDateToTimestamp(String dateString) {
        long time = 0;
        try {
            time = new SimpleDateFormat("yyyy-MM-dd").parse(dateString).getTime();
        }
        catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
        }
        return time;
    }
}