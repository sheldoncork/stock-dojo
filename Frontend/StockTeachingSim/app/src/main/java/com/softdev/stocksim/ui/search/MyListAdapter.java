package com.softdev.stocksim.ui.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.softdev.stocksim.ui.home.stock.Stock;

import java.util.List;

/**
 * @author Sheldon
 */
public class MyListAdapter extends BaseAdapter implements Filterable {

    private final LayoutInflater inflater;

    private final List<Stock> stocks;

    /**
     * @param context Context of the fragment
     * @param stocks  List of stocks
     */
    public MyListAdapter(Context context, List<Stock> stocks) {
        this.stocks = stocks;
        inflater = LayoutInflater.from(context);
    }

    /**
     * @return size of stocks list
     */
    @Override
    public int getCount() {
        return stocks.size();
    }

    /**
     * @return stock at position
     */
    @Override
    public Object getItem(int position) {
        return stocks.get(position);
    }

    /**
     * @return position
     */
    @Override
    public long getItemId(int position) {
        return stocks.get(position).getId();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return null;
    }

    @Override
    public Filter getFilter() {
        return null;
    }
}
