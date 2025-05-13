package com.softdev.stocksim.ui.home.history;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single transaction in the database.
 *
 * @author Blake Nelson
 */
public class Transaction {
    private final long id;
    private final String ticker;
    private final double price;
    private final int shares;
    private final LocalDateTime transactionDate;

    /**
     * Constructs a new Transaction object.
     *
     */
    public Transaction(long id, String ticker, double price, int shares, String transactionDate) {
        this.id = id;
        this.ticker = ticker;
        this.price = price;
        this.shares = shares;
        this.transactionDate = LocalDateTime.parse(transactionDate, DateTimeFormatter.ISO_DATE_TIME);
    }

    // Getters
    public long getId() { return id; }
    public String getTicker() { return ticker; }
    public double getPrice() { return price; }
    public int getShares() { return shares; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
}