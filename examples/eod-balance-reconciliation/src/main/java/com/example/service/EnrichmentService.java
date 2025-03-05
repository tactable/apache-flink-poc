package com.example.service;

import com.example.model.Transaction;
import com.example.model.EnrichedTransaction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class EnrichmentService implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // JSONPlaceholder API endpoint for mock data
    private static final String JSON_PLACEHOLDER_API = "https://jsonplaceholder.typicode.com";
    
    // Mark non-serializable fields as transient
    private transient ObjectMapper objectMapper;
    private transient Map<String, Map<String, String>> cache;
    
    public EnrichmentService() {
        initializeTransientFields();
    }
    
    // Initialize transient fields after deserialization
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        initializeTransientFields();
    }
    
    private void initializeTransientFields() {
        this.objectMapper = new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
    }
    
    public EnrichedTransaction enrichTransaction(Transaction transaction) {
        try {
            // Check cache first
            String lineItem = transaction.getLineItem();
            if (!cache.containsKey(lineItem)) {
                // Make an actual HTTP call to JSONPlaceholder API
                Map<String, String> enrichmentData = fetchEnrichmentDataFromAPI(lineItem);
                cache.put(lineItem, enrichmentData);
            }
            
            Map<String, String> enrichmentData = cache.get(lineItem);
            
            return new EnrichedTransaction(
                    transaction.getTransactionId(),
                    transaction.getAccountId(),
                    transaction.getLineItem(),
                    transaction.getAmount(),
                    transaction.getTransactionDate(),
                    enrichmentData.get("department"),
                    enrichmentData.get("category")
            );
        } catch (Exception e) {
            // In case of API error, log and return with default values
            System.err.println("Error enriching transaction: " + e.getMessage());
            return new EnrichedTransaction(
                    transaction.getTransactionId(),
                    transaction.getAccountId(),
                    transaction.getLineItem(),
                    transaction.getAmount(),
                    transaction.getTransactionDate(),
                    "UNKNOWN",
                    "UNKNOWN"
            );
        }
    }
    
    private Map<String, String> fetchEnrichmentDataFromAPI(String lineItem) throws Exception {
        // Simulate random API latency between 10-20 seconds
        try {
            long simulatedLatency = 10000 + (long)(Math.random() * 10000);
            Thread.sleep(simulatedLatency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("API simulation was interrupted", e);
        }
        // We'll use JSONPlaceholder's users endpoint to get mock data
        // We'll use the lineItem to determine which user ID to fetch
        int userId = Math.abs(lineItem.hashCode() % 10) + 1; // Generate a user ID between 1-10
        
        // Use older HttpURLConnection instead of HttpClient to avoid module issues
        URL url = new URL(JSON_PLACEHOLDER_API + "/users/" + userId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        
        // Check if the request was successful
        if (responseCode != 200) {
            System.err.println("API request failed with status code: " + responseCode);
            connection.disconnect();
            return getFallbackEnrichmentData(lineItem);
        }
        
        // Read the response
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();
        
        // Parse the JSON response
        JsonNode jsonNode = objectMapper.readTree(response.toString());
        
        // Create a map to store the enrichment data
        Map<String, String> result = new HashMap<>();
        
        // Extract data from the JSONPlaceholder response and map to our domain
        // We'll use the company name as department and address city as category
        if (jsonNode.has("company") && jsonNode.get("company").has("name")) {
            result.put("department", jsonNode.get("company").get("name").asText());
        } else {
            result.put("department", "GENERAL");
        }
        
        if (jsonNode.has("address") && jsonNode.get("address").has("city")) {
            // Determine if it's EXPENSE or REVENUE based on the transaction amount
            // This is just for demonstration - in a real app, this logic would be different
            String category = lineItem.toLowerCase().contains("sales") || 
                              lineItem.toLowerCase().contains("salary") ? 
                              "REVENUE" : "EXPENSE";
            result.put("category", category);
        } else {
            result.put("category", "EXPENSE");
        }
        
        System.out.println("Enriched " + lineItem + " with data from JSONPlaceholder: " + result);
        return result;
    }
    
    // Fallback method in case the API is unavailable
    private Map<String, String> getFallbackEnrichmentData(String lineItem) {
        Map<String, String> result = new HashMap<>();
        
        switch (lineItem.toLowerCase()) {
            case "salary":
                result.put("department", "HR");
                result.put("category", "REVENUE");
                break;
            case "rent":
                result.put("department", "FACILITIES");
                result.put("category", "EXPENSE");
                break;
            case "utilities":
                result.put("department", "FACILITIES");
                result.put("category", "EXPENSE");
                break;
            case "sales":
                result.put("department", "SALES");
                result.put("category", "REVENUE");
                break;
            case "marketing":
                result.put("department", "MARKETING");
                result.put("category", "EXPENSE");
                break;
            case "software":
                result.put("department", "IT");
                result.put("category", "EXPENSE");
                break;
            case "hardware":
                result.put("department", "IT");
                result.put("category", "EXPENSE");
                break;
            case "consulting":
                result.put("department", "PROFESSIONAL_SERVICES");
                result.put("category", "EXPENSE");
                break;
            case "training":
                result.put("department", "HR");
                result.put("category", "EXPENSE");
                break;
            default:
                result.put("department", "GENERAL");
                result.put("category", "EXPENSE");
        }
        
        System.out.println("Using fallback data for " + lineItem + ": " + result);
        return result;
    }
} 