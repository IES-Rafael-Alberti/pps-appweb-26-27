package com.vetclinic.sale.dto;

import com.vetclinic.sale.FoodSale;
import com.vetclinic.sale.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Schema(name = "SaleResponse")
public record SaleResponse(
        Long id,
        String saleNumber,
        Instant soldAt,
        String customerName,
        Long customerId,
        PaymentMethod paymentMethod,
        List<Line> lines,
        int totalItems,
        BigDecimal totalAmount,
        String notes) {

    @Schema(name = "SaleLine")
    public record Line(
            Long productId,
            String productName,
            String productBrand,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal) {
    }

    public static SaleResponse from(FoodSale sale) {
        List<Line> lines = sale.getLines().stream()
                .map(line -> new Line(
                        line.getProduct().getId(),
                        line.getProductName(),
                        line.getProductBrand(),
                        line.getUnitPrice(),
                        line.getQuantity(),
                        line.getLineTotal()))
                .sorted(Comparator.comparing(Line::productId))
                .toList();
        return new SaleResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getSoldAt(),
                sale.getCustomerName(),
                sale.getCustomer() == null ? null : sale.getCustomer().getId(),
                sale.getPaymentMethod(),
                lines,
                sale.getTotalItems(),
                sale.getTotalAmount(),
                sale.getNotes());
    }
}
