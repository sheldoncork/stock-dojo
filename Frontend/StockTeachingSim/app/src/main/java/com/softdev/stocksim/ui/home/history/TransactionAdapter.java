package com.softdev.stocksim.ui.home.history;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.softdev.stocksim.R;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Represents a single transaction in the database.
 *
 * @author Blake Nelson
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<Transaction> transactions = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(transactions.get(position));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void submitList(List<Transaction> newTransactions) {
        transactions = newTransactions;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView transactionCard;
        private final TextView tickerText;
        private final TextView dateText;
        private final TextView sharesText;
        private final TextView priceText;
        private final TextView totalText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionCard = itemView.findViewById(R.id.transaction_card);
            tickerText = itemView.findViewById(R.id.ticker_text);
            dateText = itemView.findViewById(R.id.date_text);
            sharesText = itemView.findViewById(R.id.shares_text);
            priceText = itemView.findViewById(R.id.price_text);
            totalText = itemView.findViewById(R.id.total_text);
        }

        public void bind(Transaction transaction) {

            // Get theme colors using TypedValue
            TypedValue typedValue = new TypedValue();
            Context context = itemView.getContext();
            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
            int gainColor = typedValue.data;

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, typedValue, true);
            int lossColor = typedValue.data;

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnError, typedValue, true);
            int gainText = typedValue.data;

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
            int lossText = typedValue.data;

            if (transaction.getShares() < 0) {
                transactionCard.setCardBackgroundColor(lossColor);
                tickerText.setTextColor(lossText);
                dateText.setTextColor(lossText);
                sharesText.setTextColor(lossText);
                priceText.setTextColor(lossText);
                totalText.setTextColor(lossText);
            } else if (transaction.getShares() > 0){
                transactionCard.setCardBackgroundColor(gainColor);
                tickerText.setTextColor(gainText);
                dateText.setTextColor(gainText);
                sharesText.setTextColor(gainText);
                priceText.setTextColor(gainText);
                totalText.setTextColor(gainText);
            }

            tickerText.setText(transaction.getTicker());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            dateText.setText(transaction.getTransactionDate().format(formatter));
            sharesText.setText(String.format("%d shares", transaction.getShares()));
            priceText.setText(String.format("$%.2f", transaction.getPrice()));
            totalText.setText(String.format("Total: $%.2f",
                    transaction.getPrice() * transaction.getShares()));
        }
    }
}
