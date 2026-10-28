package com.vetclinic.sale;

import com.vetclinic.common.PageResponse;
import com.vetclinic.sale.dto.CreateSaleRequest;
import com.vetclinic.sale.dto.SaleResponse;
import com.vetclinic.sale.dto.TopProduct;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
@Tag(name = "Sales", description = "Tickets sold in the clinic shop")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class FoodSaleController {

    private final FoodSaleService foodSaleService;

    public FoodSaleController(FoodSaleService foodSaleService) {
        this.foodSaleService = foodSaleService;
    }

    @GetMapping
    @Operation(summary = "List sales", description = "Filter by customer, payment method or date range")
    public PageResponse<SaleResponse> list(
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "cash, card, transfer")
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @Parameter(description = "ISO instant, e.g. 2026-04-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "soldAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(foodSaleService.search(customerId, paymentMethod, from, to, pageable));
    }

    @GetMapping("/reports/top-products")
    @Operation(summary = "Best selling products by revenue")
    public List<TopProduct> topProducts(
            @Parameter(description = "How many entries to return", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        return foodSaleService.topProducts(limit);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a sale by id")
    public SaleResponse get(@PathVariable Long id) {
        return foodSaleService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a sale",
            description = "Atomically checks the stock of every line, discounts it and returns the ticket "
                    + "with the totals computed by the server. Fails with 409 when there is not enough stock.")
    public SaleResponse create(@Valid @RequestBody CreateSaleRequest request) {
        return foodSaleService.registerSale(request);
    }
}
