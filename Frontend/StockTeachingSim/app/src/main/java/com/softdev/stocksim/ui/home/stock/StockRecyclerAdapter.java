package com.softdev.stocksim.ui.home.stock;

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
 * @author Sheldon
 */
public class StockRecyclerAdapter extends RecyclerView.Adapter<StockRecyclerAdapter.MyViewHolder> {

    private final Context context;
    private final ArrayList<Stock> stocks;
    private OnClickListener clickListener;

    public StockRecyclerAdapter(Context context, ArrayList<Stock> stocks) {
        this.context = context;
        this.stocks = stocks;
    }

    /**
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     */
    @NonNull
    @Override
    public StockRecyclerAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.item_stock, parent, false);
        return new StockRecyclerAdapter.MyViewHolder(view);
    }

    /**
     * Sets the ViewHolder contents and click listeners
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull StockRecyclerAdapter.MyViewHolder holder, int position) {
        Stock stock = stocks.get(position);
        holder.name.setText(stock.getName());

        double currentPrice = stock.getStockPrice();
        String strCash = "$" + currentPrice;
        holder.price.setText(strCash);

        double value = currentPrice * stock.getQuantity();
        value = Math.round(value * 100.00 / 100.00);
        String strValue = "$" + value;
        holder.quantity.setText(strValue);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(position, stock);
            }
        });
    }

    /**
     * @return stocks list size
     */
    @Override
    public int getItemCount() {
        return stocks.size();
    }

    public void setOnClickListener(OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface OnClickListener {
        /**
         * @param position current int position
         * @param stock    current stock
         */
        void onClick(int position, Stock stock);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name, quantity, price;

        /**
         * Sets up views for the adapter
         * @param itemView View of the item
         */
        //Basically onCreate
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.stock_name);
            quantity = itemView.findViewById(R.id.stock_quantity);
            price = itemView.findViewById(R.id.stock_value);
        }
    }

}
