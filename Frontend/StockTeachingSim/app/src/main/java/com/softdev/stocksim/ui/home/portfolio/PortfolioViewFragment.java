package com.softdev.stocksim.ui.home.portfolio;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.softdev.stocksim.R;
import com.softdev.stocksim.ui.BaseFragment;
import com.softdev.stocksim.ui.home.stock.Stock;
import com.softdev.stocksim.ui.home.stock.StockRecyclerAdapter;

import java.util.ArrayList;

/**
 * @author Sheldon
 */
public class PortfolioViewFragment extends BaseFragment {
    private final String TAG = "PortfolioViewFragment";

    Button transactionHistoryBTN;
    private TextView stockViewTV;
    private RecyclerView stockRecycler;
    private PortfolioModel portfolio;

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_portfolio_view, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseArguments();
    }

    private void parseArguments() {
        PortfolioViewFragmentArgs args = PortfolioViewFragmentArgs.fromBundle(requireArguments());
        portfolio = args.getPortfolio();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeUIElements(view);
        setupToolbar();
        if (portfolio != null) setStockView(portfolio);
        hideLoading();
    }

    private void initializeUIElements(View view) {
        transactionHistoryBTN = view.findViewById(R.id.portfolio_transaction_history);
        stockRecycler = view.findViewById(R.id.stock_recycler);
        stockViewTV = view.findViewById(R.id.stock_view_tv);

        transactionHistoryBTN.setOnClickListener(v -> {
            navigateToTransactionHistory();
        });
    }

    private void navigateToTransactionHistory() {
        PortfolioViewFragmentDirections.ActionPortfolioViewFragmentToTransactionHistoryFragment action =
                PortfolioViewFragmentDirections.actionPortfolioViewFragmentToTransactionHistoryFragment(
                        false,
                        portfolio.getId(),
                        portfolio.getName()
                );
        navController.navigate(action);
    }

    private void setupToolbar() {
        inflateToolbarMenu(R.menu.top_settings_menu);
        setToolbarTitle(portfolio.getName());
        setToolbarMenuClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_settings) {
                Bundle portfolioBundle = new Bundle();
                portfolioBundle.putParcelable("portfolio", portfolio);
                navigateToPortfolioManager(portfolioBundle);
                return true;
            }
            return false;
        });
    }

    /**
     * Set up the stock recycler view
     */
    private void setStockView(PortfolioModel portfolio) {
        ArrayList<Stock> stocks = portfolio.getStocks();
        if (stocks == null || stocks.isEmpty()) {
            stockViewTV.setText("No stocks found");
            stockRecycler.setVisibility(View.GONE);
            return;
        }
        stockViewTV.setText("Cash Available: $" + portfolio.getCash());

        // Setup stock recycler
        Log.d(TAG, stocks.toString());
        StockRecyclerAdapter adapter = new StockRecyclerAdapter(requireActivity(), stocks);
        stockRecycler.setAdapter(adapter);
        adapter.setOnClickListener((position, stock) -> {
            Bundle bundle = new Bundle();
            bundle.putString("stockSymbol", stock.getName());
            bundle.putParcelable("portfolio", portfolio);
            navigateToResults(bundle);
        });
        stockRecycler.setLayoutManager(new LinearLayoutManager(requireActivity()));
    }

    private void navigateToPortfolioManager(Bundle args) {
        navController.navigate(R.id.action_portfolioViewFragment_to_portfolioManagerFragment, args);
    }

    private void navigateToResults(Bundle args) {
        navController.navigate(R.id.action_portfolioViewFragment_to_resultsFragment, args);
    }
}