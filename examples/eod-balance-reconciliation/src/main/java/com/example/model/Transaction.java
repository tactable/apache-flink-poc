package com.example.model;

public class Transaction {
    private String transactionId;
    private String accountId;
    private String lineItem;
    private double amount;
    private String transactionDate;
    
    public Transaction(String transactionId, String accountId, String lineItem, double amount, String transactionDate) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.lineItem = lineItem;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }
    
    // Getters
    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public String getLineItem() { return lineItem; }
    public double getAmount() { return amount; }
    public String getTransactionDate() { return transactionDate; }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", lineItem='" + lineItem + '\'' +
                ", amount=" + amount +
                ", transactionDate='" + transactionDate + '\'' +
                '}';
    }
} 