package com.growth360.model;

import java.time.LocalDateTime;

/**
 * Represents a business profile / store entity.
 */
public class User {
    private int id;
    private String businessName;
    private String ownerName;
    private String email;
    private String currency;
    private LocalDateTime createdAt;

    public User() {
        this.currency = "INR";
        this.createdAt = LocalDateTime.now();
    }

    public User(int id, String businessName, String ownerName, String email, String currency, LocalDateTime createdAt) {
        this.id = id;
        this.businessName = businessName;
        this.ownerName = ownerName;
        this.email = email;
        this.currency = currency != null ? currency : "INR";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public User(String businessName, String ownerName, String email, String currency) {
        this(0, businessName, ownerName, email, currency, LocalDateTime.now());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return businessName + " (" + ownerName + ")";
    }
}
