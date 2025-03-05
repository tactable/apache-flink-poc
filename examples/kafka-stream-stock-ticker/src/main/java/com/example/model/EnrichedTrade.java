package com.example.model;

public class EnrichedTrade {
    private String symbol;
    private double price;
    private int quantity;
    private String tradeId;
    private long timestamp;
    private double totalValue;
    private String tradingSession;

    private static final String HEADER = "symbol,price,quantity,tradeId,timestamp,totalValue,tradingSession";
    private static boolean headerWritten = false;

    public EnrichedTrade(TradeEvent trade) {
        this.symbol = trade.getSymbol();
        this.price = trade.getPrice();
        this.quantity = trade.getQuantity();
        this.tradeId = trade.getTradeId();
        this.timestamp = trade.getTimestamp();
        this.totalValue = trade.getPrice() * trade.getQuantity();
        this.tradingSession = determineTradingSession(trade.getTimestamp());
    }

    private String determineTradingSession(long timestamp) {
        // Simple logic to determine trading session based on hour
        int hour = java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .getHour();
        
        if (hour >= 9 && hour < 12) return "MORNING";
        else if (hour >= 12 && hour < 16) return "AFTERNOON";
        else return "AFTER_HOURS";
    }

    @Override
    public String toString() {
        if (!headerWritten) {
            headerWritten = true;
            return HEADER + "\n" + toCSV();
        }
        return toCSV();
    }

    private String toCSV() {
        return String.join(",", 
            symbol,
            String.valueOf(price),
            String.valueOf(quantity),
            tradeId,
            String.valueOf(timestamp),
            String.valueOf(totalValue),
            tradingSession
        );
    }
} 