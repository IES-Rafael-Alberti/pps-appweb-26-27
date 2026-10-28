package com.vetclinic.food;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/** A food product sold in the clinic shop. */
@Entity
@Table(name = "food_products")
public class FoodProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodCategory category;

    @Column(length = 500)
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int stock;

    @Column(name = "weight_kg", precision = 7, scale = 3)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FoodProduct() {
        // for JPA
    }

    public FoodProduct(String name, String brand, FoodCategory category, String description,
                       BigDecimal unitPrice, int stock, BigDecimal weightKg) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.description = description;
        this.unitPrice = unitPrice;
        this.stock = stock;
        this.weightKg = weightKg;
        this.active = true;
    }

    public void updateDetails(String name, String brand, FoodCategory category, String description,
                              BigDecimal unitPrice, BigDecimal weightKg) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.description = description;
        this.unitPrice = unitPrice;
        this.weightKg = weightKg;
    }

    /** @return the new stock level. @throws IllegalStateException when the movement would go below zero */
    public int applyStockDelta(int delta) {
        int updated = this.stock + delta;
        if (updated < 0) {
            throw new IllegalStateException("stock would drop to %d".formatted(updated));
        }
        this.stock = updated;
        return this.stock;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public FoodCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getStock() {
        return stock;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public boolean isActive() {
        return active;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
