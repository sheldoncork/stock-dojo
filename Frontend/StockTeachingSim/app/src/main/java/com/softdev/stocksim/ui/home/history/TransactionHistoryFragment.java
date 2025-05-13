package com.softdev.stocksim.ui.home.history;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.softdev.stocksim.R;
import com.softdev.stocksim.api.VolleySingleton;
import com.softdev.stocksim.ui.BaseFragment;
import com.softdev.stocksim.utils.AppConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single transaction in the database.
 *
 * @author Blake Nelson
 */
public class TransactionHistoryFragment extends BaseFragment {

    private View transactionsLayout;
    private View emptyTransactionsLayout;
    private TextView portfolioTitle;
    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter adapter;

    boolean fetchAll;
    private int portfolioId;
    private String portfolioName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TransactionHistoryFragmentArgs args = TransactionHistoryFragmentArgs.fromBundle(requireArguments());

        fetchAll = args.getFetchAll();
        portfolioId = args.getPortfolioId();
        portfolioName = args.getPortfolioName();
    }

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater,
                                       @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeUI(view);
        setupRecyclerView();
        loadTransactions();
    }

    private void initializeUI(View view) {
        transactionsLayout = view.findViewById(R.id.transaction_history_view);
        emptyTransactionsLayout = view.findViewById(R.id.empty_transactions_layout);
        portfolioTitle = view.findViewById(R.id.portfolio_title);
        if (fetchAll) {
            setToolbarTitle("All Transactions");
        } else {
            setToolbarTitle("Transaction History");
            portfolioTitle = view.findViewById(R.id.portfolio_title);
            portfolioTitle.setText(portfolioName);
            portfolioTitle.setVisibility(View.VISIBLE);
        }

        transactionsRecyclerView = view.findViewById(R.id.transactions_recycler_view);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        transactionsRecyclerView.setAdapter(adapter);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void loadTransactions() {
        showLoading();
        String url;
        if (fetchAll) {
            url = AppConfig.BASE_URL + "/transaction/all";
        } else {
            url = AppConfig.BASE_URL + "/transaction?portfolioId=" + portfolioId;
        }

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
            if (response.length() == 0) {
                emptyTransactionsLayout.setVisibility(View.VISIBLE);
                transactionsLayout.setVisibility(View.GONE);
                hideLoading();
                return;
            }
                    List<Transaction> transactions = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            Transaction transaction = new Transaction(
                                    obj.getLong("id"),
                                    obj.getString("ticker"),
                                    obj.getDouble("price"),
                                    obj.getInt("shares"),
                                    obj.getString("transactionDate")
                            );
                            transactions.add(transaction);
                        } catch (JSONException e) {
                            showError("Error parsing transaction data");
                            return;
                        }
                    }
                    adapter.submitList(transactions);
                    hideLoading();
                },
                error -> {
                    if (error.networkResponse.statusCode == 403) {
                        Log.e("TransactionHistoryFragment", "Error loading transactions: " + error.getMessage());
                        showError("Failed to load transactions");
                    } else if (error.networkResponse.statusCode == 404) {
                        Log.e("TransactionHistoryFragment", "Error loading transactions: " + error.getMessage());
                        transactionsLayout.setVisibility(View.GONE);
                        emptyTransactionsLayout.setVisibility(View.VISIBLE);
                    } else {
                        Log.e("TransactionHistoryFragment", "Error loading transactions: " + error.getMessage());
                        showError("Error loading transactions");
                    }
                    hideLoading();
                });

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }
}

