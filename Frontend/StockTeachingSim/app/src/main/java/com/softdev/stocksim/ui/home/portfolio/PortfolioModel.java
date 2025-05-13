package com.softdev.stocksim.ui.home.portfolio;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.softdev.stocksim.api.VolleySingleton;
import com.softdev.stocksim.ui.home.stock.Stock;
import com.softdev.stocksim.utils.AppConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a portfolio model containing an ID, name, cash, and a list of stocks.
 * Provides methods for handling stock transactions and updating the portfolio.
 * @author Sheldon
 */
public class PortfolioModel implements Parcelable {
    private final int id;
    private String name;
    private double cash;
    private double value;
    private ArrayList<Stock> stocks;

    /**
     * Constructs a PortfolioModel with a JSON array of stocks.
     *
     * @param id       the portfolio ID
     * @param name     the portfolio name
     * @param cash     the cash amount in the portfolio
     * @param stocks   the JSON array representing stocks
     */
    public PortfolioModel(int id, String name, double cash, double value, JSONArray stocks) {
        this.id = id;
        this.name = name;
        this.cash = cash;
        this.value = value;
        this.stocks = parseStocks(stocks);
    }

    /**
     * Converts a JSONArray of stock data to an ArrayList of Stock objects.
     *
     * @param stocks JSON array of stock data
     * @return ArrayList of Stock objects, or null if input is empty
     */
    public ArrayList<Stock> parseStocks(JSONArray stocks) {
        if (stocks != null) {
            ArrayList<Stock> stockArray = new ArrayList<>();
            for (int i = 0; i < stocks.length(); i++) {
                try {
                    JSONObject stockObject = stocks.getJSONObject(i);
                    int id = stockObject.getInt("id");
                    String stockName = stockObject.getString("ticker");
                    int quantity = stockObject.getInt("shares");
                    stockArray.add(new Stock(id, stockName, quantity));
                } catch (Exception e) {
                    Log.e("PortfolioModel", "Error parsing stock JSON: ", e);
                }
            }
            return stockArray;
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public double getValue(){
        BigDecimal bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the cash amount in the portfolio, rounded to two decimal places.
     *
     * @return the cash amount
     */
    public double getCash() {
        BigDecimal bd = BigDecimal.valueOf(cash).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    public void setValue(double newValue) {
        this.value = newValue;
    }

    public ArrayList<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(ArrayList<Stock> stocks) {
        this.stocks = stocks;
    }

    /**
     * Checks if the portfolio contains a stock with the specified ticker.
     *
     * @param ticker the stock ticker
     * @return quantity if found, 0 if not found
     */
    public int containsStock(String ticker) {
        for (Stock stock : stocks) {
            if (stock.getName().equals(ticker)) {
                return stock.getQuantity();
            }
        }
        return 0;
    }

    /**
     * Executes a stock purchase by sending a POST request to the backend.
     *
     * @param context       the application context
     * @param ticker        the stock ticker
     * @param currentPrice  the current price of the stock
     * @param quantity      the quantity to buy
     * @param callback      the callback to execute upon completion
     */
    public void buyStock(Context context, String ticker, double currentPrice, int quantity, PortfolioCallback callback) {
        double totalCost = currentPrice * quantity;  // Calculate total cost

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                AppConfig.BASE_URL + "/stock/buy?portfolioId=" + getId(),
                response -> {
                    if (response.equals("Purchase successful")) {
                        setCash(cash - totalCost);
                        addStock(ticker, quantity);
                        callback.onUpdateComplete();
                    }
                },
                error -> Log.e("PortfolioModel:", error.toString())
        ) {
            @Override
            public byte[] getBody() {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("portfolioId", getId());
                    jsonBody.put("ticker", ticker);
                    jsonBody.put("price", currentPrice);
                    jsonBody.put("shares", quantity);
                    return jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    Log.e("PortfolioModel", "Failed to create request body", e);
                    return null;
                }
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
    }

    /**
     * Executes a stock sale by sending a POST request to the backend.
     *
     * @param context       the application context
     * @param ticker        the stock ticker
     * @param currentPrice  the current price of the stock
     * @param quantity      the quantity to sell
     * @param callback      the callback to execute upon completion
     */
    public void sellStock(Context context, String ticker, double currentPrice, int quantity, PortfolioCallback callback) {
        double totalValue = currentPrice * quantity;  // Calculate total value

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                AppConfig.BASE_URL + "/stock/sell?portfolioId=" + getId(),
                response -> {
                    if (response.equals("Sale successful")) {
                        setCash(cash + totalValue);
                        removeStock(ticker, quantity);
                        callback.onUpdateComplete();
                    }
                },
                error -> Log.e("PortfolioModel:", error.toString())
        ) {
            @Override
            public byte[] getBody() {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("portfolioId", getId());
                    jsonBody.put("ticker", ticker);
                    jsonBody.put("price", currentPrice);
                    jsonBody.put("shares", quantity);
                    return jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    Log.e("PortfolioModel", "Failed to create request body", e);
                    return null;
                }
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
    }

    /**
     * Adds a specified quantity of stock to the portfolio, or creates a new stock if it does not exist.
     *
     * @param name     the stock name
     * @param quantity the quantity to add (must be > 0)
     */
    public void addStock(String name, int quantity) {
        if (quantity <= 0) return;
        for (Stock stock : stocks) {
            if (stock.getName().equals(name)) {
                stock.addQuantity(quantity);
                return;
            }
        }
        stocks.add(new Stock(name, quantity));
    }

    /**
     * Removes a specified quantity of stock from the portfolio.
     * If quantity falls to 0 or less, removes the stock entirely.
     *
     * @param name     the stock name
     * @param quantity the quantity to remove (must be > 0)
     */
    public void removeStock(String name, int quantity) {
        if (quantity <= 0) return;
        for (Stock stock : stocks) {
            if (stock.getName().equals(name)) {
                stock.removeQuantity(quantity);
                if (stock.getQuantity() <= 0) {
                    stocks.remove(stock);
                }
                return;
            }
        }
    }

    /**
     * Callback interface for updating results after Volley requests complete.
     */
    public interface PortfolioCallback {
        void onUpdateComplete();
    }

    // Parceable implementation

    protected PortfolioModel(Parcel in) {
        id = in.readInt();
        name = in.readString();
        cash = in.readDouble();
        value = in.readDouble();
        stocks = in.createTypedArrayList(Stock.CREATOR); // Reading the list of Stock objects
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeDouble(cash);
        dest.writeDouble(value);
        dest.writeTypedList(stocks); // Writing the list of Stock objects
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PortfolioModel> CREATOR = new Creator<PortfolioModel>() {
        @Override
        public PortfolioModel createFromParcel(Parcel in) {
            return new PortfolioModel(in);
        }

        @Override
        public PortfolioModel[] newArray(int size) {
            return new PortfolioModel[size];
        }
    };
}
