package com.softdev.stocksim.ui.search;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.softdev.stocksim.R;
import com.softdev.stocksim.api.VolleySingleton;
import com.softdev.stocksim.api.WebSocketListener;
import com.softdev.stocksim.api.WebSocketManager;
import com.softdev.stocksim.data.UserPreferences;
import com.softdev.stocksim.ui.BaseFragment;
import com.softdev.stocksim.utils.AppConfig;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sheldon
 */
public class SearchFragment extends BaseFragment implements WebSocketListener {

    private final String TAG = "SearchFragment";
    private SearchView searchView;
    private ListView listView;
    private String username, password;

    private LinearLayout linearLayout;
    private RecyclerView stockRecycler;
    private boolean inStockView;
    private final List<String> searchSuggestions = new ArrayList<>();
    private List<Pair<String, String>> results;

    /**
     * Required empty public constructor
     */
    public SearchFragment() {
    }

    /**
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserPreferences userPreferences = UserPreferences.getInstance(requireContext());
        username = userPreferences.getUsername();
        password = userPreferences.getPassword();
        connect();
    }


    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initFunctions();
        searchView.setQuery("", false);
        hideLoading();

        // Setup back press handling
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (inStockView) {
                            stockView(false);
                        }
                    }
                });
    }

    /**
     * Initializes UI elements
     *
     * @param view current view
     */
    private void initViews(View view) {
        searchView = view.findViewById(R.id.search_view);
        listView = view.findViewById(R.id.search_list);
        stockRecycler = view.findViewById(R.id.search_stock_recycler);
        linearLayout = view.findViewById(R.id.linear_search);
    }

    /**
     * Sets up the search view's functionality
     */
    private void initFunctions() {
        searchView.setIconifiedByDefault(false);
        //Sets up the results after you search and sends data to websocket
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchResults(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                sendQuery(s);
                return false;
            }
        });
    }

    /**
     * Gets search results and calls @setupRecyclerView()
     *
     * @param query the search query
     */
    private void searchResults(String query) {
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET,
                AppConfig.BASE_URL + "/stock/search?query=" + query.trim().toLowerCase(),
                null,
                response -> {
                    results = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject jsonObject = response.getJSONObject(i);
                            String description = jsonObject.getString("description");
                            String symbol = jsonObject.getString("symble");
                            results.add(new Pair<>(description, symbol));
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing portfolio JSON ", e);
                        }
                    }
                    if (!results.isEmpty()) {
                        setupRecyclerView(results);
                    } else {
                        showError("No results found");
                    }
                },
                error -> Log.e(TAG, "Error fetching stock data", error)
        );
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    /**
     * @param suggestions List of suggestions to display in the ListView
     */
    private void setupListView(List<String> suggestions) {
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, suggestions);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedStock = suggestions.get(position);
            Bundle args = new Bundle();
            args.putString("stockSymbol", selectedStock);
            navigateToResults(args);
        });
        listView.setVisibility(View.VISIBLE);
    }

    /**
     * Set up the RecyclerView with the  and layout manager
     */
    private void setupRecyclerView(List<Pair<String, String>> results) {
        SearchRecyclerAdapter adapter = new SearchRecyclerAdapter(requireActivity(), results);
        adapter.setOnClickListener(
                (position, stockTicker) -> {
                    Bundle args = new Bundle();
                    args.putString("stockSymbol", stockTicker);
                    navigateToResults(args);
                }
        );
        stockRecycler.setLayoutManager(new LinearLayoutManager(requireActivity()));
        stockRecycler.setAdapter(adapter);
        stockView(true);

        setToolbarTitle("Search results for " + searchView.getQuery());
        setNavigationOnClickListener(v -> {
            stockView(false);
        });
    }

    private void stockView(boolean inStockView){
        if(inStockView) {
            this.inStockView = true;
            linearLayout.setVisibility(View.INVISIBLE);
            stockRecycler.setVisibility(View.VISIBLE);
        } else {
            this.inStockView = false;
            setToolbarTitle("Search");
            if (!searchSuggestions.isEmpty())
                setupListView(searchSuggestions);
            linearLayout.setVisibility(View.VISIBLE);
            stockRecycler.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * @param args Bundle to be passed to results
     */
    private void navigateToResults(Bundle args) {
        navController.navigate(R.id.action_searchFragment_to_resultsFragment, args);
    }

    /* ----------- WEBSOCKET -------------*/

    /**
     * Connects to the websocket
     */
    private void connect() {
        WebSocketManager.getInstance().disconnectWebSocket();
        WebSocketManager.getInstance().removeWebSocketListener();

        String serverUrl = AppConfig.SEARCH_WEBSOCKET_URL + "?username=" + username + "&password=" + password;
        WebSocketManager.getInstance().setWebSocketListener(this);
        WebSocketManager.getInstance().connectWebSocket(serverUrl);
    }

    /**
     * @param query Search to send to the websocket
     */
    private void sendQuery(String query) {
        WebSocketManager.getInstance().sendMessage(query);
    }

    /**
     * @param handshakedata Information about the server handshake.
     */
    @Override
    public void onWebSocketOpen(ServerHandshake handshakedata) {
    }

    /**
     * Adds the received message to a list of suggestions and calls setupListView()
     *
     * @param message The received WebSocket message.
     */
    @Override
    public void onWebSocketMessage(String message) {
        Log.d(TAG, "message: " + message);
        requireActivity().runOnUiThread(() -> {
            try {
                searchSuggestions.clear();
                JSONArray jsonArray = new JSONArray(message);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String symbol = jsonObject.getString("symbol");
                    searchSuggestions.add(symbol);
                }
                Log.d(TAG, "Search suggestions: " + searchSuggestions);
                setupListView(searchSuggestions);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
            }
        });
    }

    /**
     * @param code   The status code indicating the reason for closure.
     * @param reason A human-readable explanation for the closure.
     * @param remote Indicates whether the closure was initiated by the remote endpoint.
     */
    @Override
    public void onWebSocketClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Connection closed. Code: " + code + ", Reason: " + reason);
    }

    /**
     * @param ex The exception that describes the error.
     */
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
        if (!searchSuggestions.isEmpty())
            setupListView(searchSuggestions);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Final cleanup
        WebSocketManager.getInstance().disconnectWebSocket();
        WebSocketManager.getInstance().removeWebSocketListener();
    }
}