package com.vetclinic.sale;

import com.vetclinic.food.FoodProduct;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One line of a sale. The product name and unit price are copied on purpose: later catalogue changes must
 * never rewrite what the customer actually paid.
 */
@Entity
@Table(name = "food_sale_lines")
public class FoodSaleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private FoodSale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private FoodProduct product;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "product_brand", length = 80)
    private String productBrand;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal lineTotal;

    protected FoodSaleLine() {
        // for JPA
    }

    FoodSaleLine(FoodSale sale, FoodProduct product, int quantity) {
        this.sale = sale;
        this.product = product;
        this.productName = product.getName();
        this.productBrand = product.getBrand();
        this.unitPrice = product.getUnitPrice();
        this.quantity = quantity;
        this.lineTotal = FoodSaleLine.computeLineTotal(this.unitPrice, quantity);
    }

    private static java.math.BigDecimal computeLineTotal(java.math.BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(java.math.BigDecimal.valueOf(quantity))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public FoodProduct getProduct() {
        return product;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductBrand() {
        return productBrand;
    }

    public java.math.BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public java.math.BigDecimal getLineTotal() {
        return lineTotal;
    }
}
