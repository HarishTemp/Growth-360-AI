package com.growth360.model;

import java.time.LocalDateTime;

/**
 * Represents a customer in the business.
 */
public class Customer {
    private int id;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime createdAt;

    public Customer() {
        this.createdAt = LocalDateTime.now();
    }

    public Customer(int id, String name, String email, String phone, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Customer(String name, String email, String phone) {
        this(0, name, email, phone, LocalDateTime.now());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name + " (#" + id + ")";
    }
}
