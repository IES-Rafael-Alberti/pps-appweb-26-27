package com.vetclinic.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "TopProduct", description = "Aggregated sales figures for one product")
public record TopProduct(
        Long productId,
        String productName,
        Long unitsSold,
        BigDecimal revenue) {
}
