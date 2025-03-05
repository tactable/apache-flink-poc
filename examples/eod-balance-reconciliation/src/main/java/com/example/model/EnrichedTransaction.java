package com.example.model;

public class EnrichedTransaction {
    private String transactionId;
    private String accountId;
    private String lineItem;
    private double amount;
    private String transactionDate;
    private String department;
    private String category;
    
    public EnrichedTransaction(String transactionId, String accountId, String lineItem, 
                              double amount, String transactionDate, 
                              String department, String category) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.lineItem = lineItem;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.department = department;
        this.category = category;
    }
    
    // Getters
    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public String getLineItem() { return lineItem; }
    public double getAmount() { return amount; }
    public String getTransactionDate() { return transactionDate; }
    public String getDepartment() { return department; }
    public String getCategory() { return category; }
    
    @Override
    public String toString() {
        return "EnrichedTransaction{" +
                "transactionId='" + transactionId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", lineItem='" + lineItem + '\'' +
                ", amount=" + amount +
                ", transactionDate='" + transactionDate + '\'' +
                ", department='" + department + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
} 