package com.softdev.stocksim.ui.home.stock;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.softdev.stocksim.api.VolleySingleton;
import com.softdev.stocksim.utils.AppConfig;

import org.json.JSONException;

/**
 * Represents a stock item
 * @author Sheldon
 */
public class Stock implements Parcelable {
    private final String name;
    private int quantity;
    private int id;
    private double currentPrice;
    private Context ctx;

    /**
     * Constructs a Stock with the specified name and quantity.
     * Initializes current price by fetching from the backend.
     *
     * @param name     the name of the stock
     * @param quantity the quantity of the stock
     */
    public Stock(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
        this.currentPrice = getStockPrice();
    }

    /**
     * Constructs a Stock with the specified id, name, and quantity.
     * Initializes current price by fetching from the backend.
     *
     * @param id       the unique identifier of the stock
     * @param name     the name of the stock
     * @param quantity the quantity of the stock
     */
    public Stock(int id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.currentPrice = getStockPrice();
    }

    /**
     * Constructs a Stock with the specified name, quantity, and current price.
     *
     * @param name         the name of the stock
     * @param quantity     the quantity of the stock
     * @param currentPrice the current price of the stock
     */
    public Stock(String name, int quantity, double currentPrice) {
        this.name = name;
        this.quantity = quantity;
        this.currentPrice = currentPrice;
    }

    /**
     * Returns the name of the stock.
     *
     * @return the stock name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the unique identifier of the stock.
     *
     * @return the stock id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the stock quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * @param quantity the new quantity to set
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * @param quantity the quantity of stock to remove
     */
    public void removeQuantity(int quantity) {
        this.quantity -= quantity;
    }

    /**
     * @param quantity the quantity of stock to add
     */
    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }

    /**
     * Fetches the current price of the stock from the backend using a GET request
     *
     * @return the current stock price
     */
    public double getStockPrice(){
        String url = AppConfig.BASE_URL + "/stock/info?symbol=" + name;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        currentPrice = response.getDouble("currentPrice");
                    } catch (JSONException e) {
                        Log.e("STOCK", "Failed to parse current price", e);
                    }
                },
                error -> Log.e("STOCK", "Failed to fetch stock price", error));
        VolleySingleton.getInstance(ctx).addToRequestQueue(request);
        currentPrice = Math.round(currentPrice * 100.0) / 100.0;
        return currentPrice;
    }

    // Parceable Implementation

    protected Stock(Parcel in) {
        id = in.readInt();
        name = in.readString();
        quantity = in.readInt();
        currentPrice = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeInt(quantity);
        dest.writeDouble(currentPrice);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Stock> CREATOR = new Creator<Stock>() {
        @Override
        public Stock createFromParcel(Parcel in) {
            return new Stock(in);
        }

        @Override
        public Stock[] newArray(int size) {
            return new Stock[size];
        }
    };
}
