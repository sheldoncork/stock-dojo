package com.softdev.stocksim.ui.home.portfolio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.softdev.stocksim.R;

import java.util.ArrayList;

/**
 * Adapter for displaying PortfolioModels data in a RecyclerView.
 * @author Sheldon
 */
public class PortfolioRecyclerAdapter extends RecyclerView.Adapter<PortfolioRecyclerAdapter.MyViewHolder> {

    private final Context context;
    private final ArrayList<PortfolioModel> portfolios;
    private OnClickListener clickListener;

    /**
     * Constructs a PortfolioRecyclerAdapter with the specified context and portfolio list.
     *
     * @param context    the context for accessing resources
     * @param portfolios the list of PortfolioModel objects to display
     */
    public PortfolioRecyclerAdapter(Context context, ArrayList<PortfolioModel> portfolios) {
        this.context = context;
        this.portfolios = portfolios;
    }

    @NonNull
    @Override
    public PortfolioRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_portfolio, parent, false);
        return new PortfolioRecyclerAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PortfolioRecyclerAdapter.MyViewHolder holder, int position) {
        PortfolioModel portfolio = portfolios.get(position);
        holder.name.setText(portfolio.getName());
        String strCash = "$" + portfolio.getValue();
        holder.cash.setText(strCash);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(position, portfolio);
            }
        });
    }

    @Override
    public int getItemCount() {
        return portfolios.size();
    }

    /**
     * Sets a click listener for handling item clicks.
     *
     * @param clickListener the listener for click events
     */
    public void setOnClickListener(OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /**
     * Listener interface for handling click events on portfolio items.
     */
    public interface OnClickListener {
        /**
         * Called when a portfolio item is clicked.
         */
        void onClick(int position, PortfolioModel portfolio);
    }

    /**
     * ViewHolder class for holding and recycling views within the RecyclerView.
     */
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name, cash;

        /**
         * Initializes the ViewHolder with the specified item view.
         *
         * @param itemView the view of the individual item in the RecyclerView
         */
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.portfolio_name);
            cash = itemView.findViewById(R.id.portfolio_value);
        }
    }
}