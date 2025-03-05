package com.example.model;


public class TradeEvent {
    private String symbol;
    private double price;
    private int quantity;
    private String tradeId;
    private long timestamp;

    // Default constructor for deserialization
    public TradeEvent() {}

    public TradeEvent(String symbol, double price, int quantity, String tradeId, long timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.tradeId = tradeId;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 