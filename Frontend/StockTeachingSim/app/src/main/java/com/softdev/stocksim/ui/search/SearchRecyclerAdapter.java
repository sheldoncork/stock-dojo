package com.softdev.stocksim.ui.search;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.softdev.stocksim.R;

import java.util.List;

/**
 * @author Sheldon
 */
public class SearchRecyclerAdapter extends RecyclerView.Adapter<SearchRecyclerAdapter.MyViewHolder> {

    private final Context context;
    private final List<Pair<String, String>> stocks;
    private OnClickListener clickListener;

    /**
     * @param context Context of the fragment
     * @param stocks List of stocks to display
     */
    public SearchRecyclerAdapter(Context context, List<Pair<String, String>> stocks) {
        this.context = context;
        this.stocks = stocks;
    }

    /**
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new viewholder for the adapter
     */
    @NonNull
    @Override
    public SearchRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_searched_item, parent, false);
        return new SearchRecyclerAdapter.MyViewHolder(view);
    }

    /**
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull SearchRecyclerAdapter.MyViewHolder holder, int position) {
        holder.name.setText(stocks.get(position).first);
        holder.ticker.setText(stocks.get(position).second);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(position, stocks.get(position).second);
            }
        });
    }

    /**
     * @return size of the stocks list
     */
    @Override
    public int getItemCount() {
        return stocks.size();
    }

    /**
     * @param clickListener sets the click listener for the adapter
     */
    public void setOnClickListener(OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface OnClickListener {
        /**
         * @param position current position of the item
         * @param stockTicker the stock's ticker
         */
        void onClick(int position, String stockTicker);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name, ticker;

        /**
         * @param itemView current view textViews
         */
        //Basically onCreate
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.search_stock_name);
            ticker = itemView.findViewById(R.id.search_stock_ticker);
        }
    }

}
