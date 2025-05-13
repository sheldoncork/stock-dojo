package com.softdev.stocksim.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.softdev.stocksim.api.WebSocketListener;
import com.softdev.stocksim.api.WebSocketManager;
import com.softdev.stocksim.data.UserPreferences;
import com.softdev.stocksim.ui.BaseFragment;
import com.softdev.stocksim.ui.home.portfolio.PortfolioModel;
import com.softdev.stocksim.ui.home.portfolio.PortfolioRecyclerAdapter;
import com.softdev.stocksim.utils.AppConfig;
import com.softdev.stocksim.R;
import com.softdev.stocksim.api.VolleySingleton;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author Sheldon
 */
public class HomeFragment extends BaseFragment implements WebSocketListener {

    // Constants
    private static final String TAG = "HomeFragment";

    // UI components
    Button transactionHistoryBTN, marketNewsBTN;
    private RecyclerView portfolioRecycler;

    // Data
    private final ArrayList<PortfolioModel> portfolios = new ArrayList<>();

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeUIElements(view);

        // Wait a little to setup toolbar (fixes + not showing on initial after app startup
        view.post(this::setupToolbar);
        fetchPortfolioData();
    }

    private void initializeUIElements(View view) {
        portfolioRecycler = view.findViewById(R.id.landing_portfolio_recycler);
        transactionHistoryBTN = view.findViewById(R.id.all_transaction_history);
        marketNewsBTN = view.findViewById(R.id.market_news);

        transactionHistoryBTN.setOnClickListener(v -> {
            navigateToTransactionHistory();
        });

        marketNewsBTN.setOnClickListener(v -> {
            navigateToMarketNews();
        });
    }

    private void navigateToTransactionHistory() {
        showLoading();
        HomeFragmentDirections.ActionHomeFragmentToTransactionHistoryFragment action =
                HomeFragmentDirections.actionHomeFragmentToTransactionHistoryFragment(
                        true,
                        -1,
                        null
                );
        navController.navigate(action);
    }

    private void navigateToMarketNews(){
        showLoading();
        navController.navigate(R.id.action_homeFragment_to_newsFragment);
    }

    /**
     * Set up the toolbar
     */
    private void setupToolbar() {
        // Inflate default menu
        inflateToolbarMenu(R.menu.top_add_menu);

        // Set up menu click listener
        setToolbarMenuClickListener(item -> {
            if (item.getItemId() == R.id.action_add) {
                navigateToPortfolioManager();
                return true;
            }
            return false;
        });
    }

    private void navigateToPortfolioManager() {
        navController.navigate(R.id.action_homeFragment_to_portfolioManagerFragment);
    }

    private void navigateToStocks(Bundle args){
        // Get the portfolio from the bundle using the new API
        PortfolioModel portfolio = args.getParcelable("PORTFOLIO", PortfolioModel.class);

        // Create the action with the required portfolio argument
        HomeFragmentDirections.ActionHomeFragmentToPortfolioViewFragment action =
                HomeFragmentDirections.actionHomeFragmentToPortfolioViewFragment(portfolio);

        navController.navigate(action);
    }

    /**
     * Fetch portfolio data from the server
     */
    private void fetchPortfolioData() {
        String url = AppConfig.BASE_URL + "/portfolio/all";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                this::handlePortfolioResponse,
                error ->{
            Log.e(TAG, "Error fetching portfolio data", error);
            showToast("Error fetching portfolio data. Try again soon.");
        });


        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * Handle the portfolio data response
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
            if(portfolios.isEmpty()){
                PortfolioModel portfolio = new PortfolioModel(
                        -1, "No portfolios found", 0, 0, null);
                portfolios.add(portfolio);
            }
            setupRecyclerView();
            hideLoading();
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing portfolio JSON ", e);
            Toast.makeText(requireContext(), "Error parsing portfolio JSON", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Set up the RecyclerView with the adapter and layout manager
     */
    private void setupRecyclerView() {
        PortfolioRecyclerAdapter adapter = new PortfolioRecyclerAdapter(requireActivity(), portfolios);
        adapter.setOnClickListener((position, portfolio) -> {
            Bundle args = new Bundle();
            args.putParcelable("PORTFOLIO", portfolio);
            navigateToStocks(args);
        });
        portfolioRecycler.setAdapter(adapter);
        portfolioRecycler.setLayoutManager(new LinearLayoutManager(requireActivity()));
    }

    public void connect(){
        WebSocketManager.getInstance().disconnectWebSocket();
        WebSocketManager.getInstance().removeWebSocketListener();

        UserPreferences userPreferences = UserPreferences.getInstance(requireContext());
        String username = userPreferences.getUsername();
        String password = userPreferences.getPassword();

        String serverUrl = AppConfig.PORTFOLIO_WEBSOCKET_URL + "?username=" + username + "&password=" + password;
        WebSocketManager.getInstance().setWebSocketListener(this);
        WebSocketManager.getInstance().connectWebSocket(serverUrl);
    }

    @Override
    public void onWebSocketOpen(ServerHandshake handshakedata) {
        Log.d(TAG, "Connection opened");
    }

    @Override
    public void onWebSocketMessage(String message) {
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = new JSONArray(message);
        }
        catch (JSONException e){
            Log.e(TAG, "Error parsing JSON WebSocket message", e);
        }
        JSONArray finalJsonArray = jsonArray;
        requireActivity().runOnUiThread(() -> {
            handlePortfolioResponse(finalJsonArray);
            setupRecyclerView();
        });
    }

    @Override
    public void onWebSocketClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Connection closed. Code: " + code + ", Reason: " + reason);
    }

    @Override
    public void onWebSocketError(Exception ex) {
        Log.e(TAG, "WebSocket error", ex);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Close websocket when fragment is not visible
        WebSocketManager.getInstance().disconnectWebSocket();
        WebSocketManager.getInstance().removeWebSocketListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reconnect websocket when fragment becomes visible again
        connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Final cleanup
        WebSocketManager.getInstance().disconnectWebSocket();
        WebSocketManager.getInstance().removeWebSocketListener();
    }
}