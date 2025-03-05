package com.example.model;

public class DailyReport {
    private String lineItem;
    private double totalAmount;
    private String department;
    private String category;
    private String reportDate;
    
    private static final String HEADER = "line_item,total_amount,department,category,report_date";
    
    public DailyReport(String lineItem, double totalAmount, String department, String category, String reportDate) {
        this.lineItem = lineItem;
        this.totalAmount = totalAmount;
        this.department = department;
        this.category = category;
        this.reportDate = reportDate;
    }
    
    // Getters
    public String getLineItem() { return lineItem; }
    public double getTotalAmount() { return totalAmount; }
    public String getDepartment() { return department; }
    public String getCategory() { return category; }
    public String getReportDate() { return reportDate; }
    
    public String toCsv() {
        return toCSVLine();
    }
    
    public static String getHeader() {
        return HEADER;
    }
    
    private String toCSVLine() {
        return String.join(",",
                lineItem,
                String.valueOf(totalAmount),
                department,
                category,
                reportDate
        );
    }
    
    @Override
    public String toString() {
        return "DailyReport{" +
                "lineItem='" + lineItem + '\'' +
                ", totalAmount=" + totalAmount +
                ", department='" + department + '\'' +
                ", category='" + category + '\'' +
                ", reportDate='" + reportDate + '\'' +
                '}';
    }
} 