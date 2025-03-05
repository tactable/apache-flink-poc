package com.example.model;

import java.util.ArrayList;
import java.util.List;

public class ReconcileReport {
    private List<DailyReport> dailyReports;
    private Boolean hasHeader;

    public ReconcileReport() {
        this.dailyReports = new ArrayList<>();
        this.hasHeader = false;
    }

    public void addDailyReport(DailyReport dailyReport) {
        this.dailyReports.add(dailyReport);
    }

    public List<DailyReport> getDailyReports() {
        return dailyReports;
    }

    public String toCsv() {
        StringBuilder csvBuilder = new StringBuilder();
        if (!hasHeader) {
            csvBuilder.append(DailyReport.getHeader()).append("\n");
            hasHeader = true;
        }
        for (DailyReport report : dailyReports) {
            csvBuilder.append(report.toCsv()).append("\n");
        }
        return csvBuilder.toString();
    }

    @Override
    public String toString() {
        return "ReconcileReport{" +
                "dailyReports=" + dailyReports +
                '}';
    }
}
