package com.vetclinic.sale;

import com.vetclinic.owner.Owner;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A ticket from the clinic shop. Totals are always computed on the server, never taken from the client. */
@Entity
@Table(name = "food_sales")
public class FoodSale {

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MONEY_SCALE = 2;
    private static final String PENDING_NUMBER = "VET-PENDING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_number", nullable = false, length = 32, unique = true)
    private String saleNumber = PENDING_NUMBER;

    @Column(name = "sold_at", nullable = false)
    private Instant soldAt;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Owner customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FoodSaleLine> lines = new ArrayList<>();

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);

    @Column(length = 500)
    private String notes;

    protected FoodSale() {
        // for JPA
    }

    public FoodSale(Instant soldAt, String customerName, Owner customer, PaymentMethod paymentMethod,
                    String notes) {
        this.soldAt = soldAt;
        this.customerName = customerName;
        this.customer = customer;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    /** Adds a line and discounts the product stock. The caller is responsible for checking availability. */
    public void addLine(com.vetclinic.food.FoodProduct product, int quantity) {
        product.applyStockDelta(-quantity);
        FoodSaleLine line = new FoodSaleLine(this, product, quantity);
        lines.add(line);
        totalItems += quantity;
        totalAmount = totalAmount.add(line.getLineTotal());
    }

    /** Builds the human readable ticket number, e.g. {@code VET-20260401-000042}. */
    public void assignSaleNumber() {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(soldAt, ZoneId.systemDefault());
        this.saleNumber = "VET-%s-%06d".formatted(localDateTime.format(NUMBER_DATE), id);
    }

    public List<FoodSaleLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public Long getId() {
        return id;
    }

    public String getSaleNumber() {
        return saleNumber;
    }

    public Instant getSoldAt() {
        return soldAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Owner getCustomer() {
        return customer;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getNotes() {
        return notes;
    }
}
