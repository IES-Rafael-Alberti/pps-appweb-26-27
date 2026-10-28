package com.vetclinic.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetclinic.ApiIntegrationTestBase;
import com.vetclinic.food.FoodCategory;
import com.vetclinic.food.FoodProduct;
import com.vetclinic.owner.Owner;
import com.vetclinic.sale.dto.CreateSaleRequest;
import com.vetclinic.sale.dto.CreateSaleRequest.SaleLineRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shop sales")
class FoodSaleApiTest extends ApiIntegrationTestBase {

    @Test
    @DisplayName("registers a ticket, computes the total and discounts the stock")
    void registersSaleAndDiscountsStock() throws Exception {
        FoodProduct dogFood = givenProduct("Crocodile Adult Mini", "24.50", 40);
        FoodProduct treats = givenProduct("Dental Sticks", "8.75", 12, FoodCategory.TREATS);
        Owner lucia = givenOwner("Lucia Fernandez", "lucia@example.com");

        JsonNode sale = postAndRead("/api/v1/sales", new CreateSaleRequest(
                lucia.getId(), "Lucia Fernandez", PaymentMethod.CARD, null,
                List.of(new SaleLineRequest(dogFood.getId(), 2), new SaleLineRequest(treats.getId(), 3)),
                "Loyalty discount asked"), 201);

        // 2 x 24.50 + 3 x 8.75 = 75.25
        assertThat(sale.get("totalAmount").decimalValue()).isEqualByComparingTo("75.25");
        assertThat(sale.get("totalItems").asInt()).isEqualTo(5);
        assertThat(sale.get("paymentMethod").asText()).isEqualTo("card");
        assertThat(sale.get("saleNumber").asText()).matches("VET-\\d{8}-\\d{6}");
        assertThat(sale.get("lines")).hasSize(2);
        assertThat(sale.get("lines").get(0).get("productName").asText()).isNotBlank();

        assertThat(foodProductRepository.findById(dogFood.getId()).orElseThrow().getStock()).isEqualTo(38);
        assertThat(foodProductRepository.findById(treats.getId()).orElseThrow().getStock()).isEqualTo(9);
    }

    @Test
    @DisplayName("keeps two decimals when the price forces rounding")
    void roundsLineTotalsToTwoDecimals() throws Exception {
        FoodProduct cheap = givenProduct("Cheap treats", "0.10", 50);

        JsonNode sale = postAndRead("/api/v1/sales", new CreateSaleRequest(
                null, "Walk-in customer", PaymentMethod.CASH, null,
                List.of(new SaleLineRequest(cheap.getId(), 3)), null), 201);

        assertThat(sale.get("totalAmount").decimalValue()).isEqualByComparingTo("0.30");
        assertThat(sale.get("lines").get(0).get("lineTotal").decimalValue()).isEqualByComparingTo("0.30");
    }

    @Test
    @DisplayName("rejects a ticket with more units than in stock and leaves the stock untouched")
    void rejectsSaleWithoutStock() throws Exception {
        FoodProduct scarce = givenProduct("Last bag", "10.00", 1);

        JsonNode error = postAndRead("/api/v1/sales", new CreateSaleRequest(
                null, "Lucia", PaymentMethod.CARD, null, List.of(new SaleLineRequest(scarce.getId(), 5)), null),
                409);

        assertThat(error.get("title").asText()).isEqualTo("Business rule violation");
        assertThat(error.get("detail").asText()).contains("requested 5, available 1");
        assertThat(foodProductRepository.findById(scarce.getId()).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("a failed line rolls the whole ticket back")
    void rollsBackEverythingWhenOneLineFails() throws Exception {
        FoodProduct available = givenProduct("Plenty", "5.00", 10);
        FoodProduct scarce = givenProduct("None", "5.00", 0);

        postAndRead("/api/v1/sales", new CreateSaleRequest(null, "Lucia", PaymentMethod.CASH, null,
                List.of(new SaleLineRequest(available.getId(), 2), new SaleLineRequest(scarce.getId(), 1)), null),
                409);

        // the first line must not have been deducted
        assertThat(foodProductRepository.findById(available.getId()).orElseThrow().getStock()).isEqualTo(10);
        assertThat(foodSaleRepository.count()).isZero();
    }

    @Test
    @DisplayName("rejects an empty ticket")
    void rejectsSaleWithoutLines() throws Exception {
        JsonNode error = postAndRead("/api/v1/sales", new CreateSaleRequest(
                null, "Lucia", PaymentMethod.CASH, null, List.of(), null), 400);

        assertThat(error.get("errors").get("lines").asText()).contains("at least one line");
    }

    @Test
    @DisplayName("rejects the same product in two lines")
    void rejectsDuplicateProductInLines() throws Exception {
        FoodProduct food = givenProduct("Crocodile", "10.00", 10);

        JsonNode error = postAndRead("/api/v1/sales", new CreateSaleRequest(
                null, "Lucia", PaymentMethod.CASH, null,
                List.of(new SaleLineRequest(food.getId(), 1), new SaleLineRequest(food.getId(), 2)), null), 400);

        assertThat(error.get("detail").asText()).contains("more than one line");
    }

    @Test
    @DisplayName("returns 404 when a line points to a product that does not exist")
    void rejectsUnknownProduct() throws Exception {
        postAndRead("/api/v1/sales", new CreateSaleRequest(
                null, "Lucia", PaymentMethod.CASH, null, List.of(new SaleLineRequest(987654L, 1)), null), 404);
    }

    @Test
    @DisplayName("does not sell products withdrawn from the catalogue")
    void rejectsDeactivatedProduct() throws Exception {
        FoodProduct food = givenProduct("Discontinued", "10.00", 5);
        food.setActive(false);
        foodProductRepository.saveAndFlush(food);

        JsonNode error = postAndRead("/api/v1/sales", new CreateSaleRequest(
                null, "Lucia", PaymentMethod.CASH, null, List.of(new SaleLineRequest(food.getId(), 1)), null), 409);

        assertThat(error.get("detail").asText()).contains("withdrawn from the catalogue");
    }

    @Test
    @DisplayName("rejects non positive quantities and unknown customers")
    void rejectsInvalidPayloads() throws Exception {
        FoodProduct food = givenProduct("Crocodile", "10.00", 10);

        JsonNode badQuantity = postAndRead("/api/v1/sales", new CreateSaleRequest(
                null, "Lucia", PaymentMethod.CASH, null, List.of(new SaleLineRequest(food.getId(), 0)), null), 400);
        assertThat(badQuantity.get("errors").get("lines[0].quantity").asText()).contains("at least 1");

        postAndRead("/api/v1/sales", new CreateSaleRequest(
                999999L, "Ghost", PaymentMethod.CASH, null, List.of(new SaleLineRequest(food.getId(), 1)), null),
                404);
    }

    @Test
    @DisplayName("lists tickets and reports the best sellers")
    void listsSalesAndTopProducts() throws Exception {
        FoodProduct dogFood = givenProduct("Crocodile", "24.50", 40);
        FoodProduct treats = givenProduct("Sticks", "8.75", 20, FoodCategory.TREATS);

        postAndRead("/api/v1/sales", new CreateSaleRequest(null, "A", PaymentMethod.CARD, null,
                List.of(new SaleLineRequest(dogFood.getId(), 1)), null), 201);
        postAndRead("/api/v1/sales", new CreateSaleRequest(null, "B", PaymentMethod.CASH, null,
                List.of(new SaleLineRequest(treats.getId(), 4)), null), 201);

        mockMvc.perform(get("/api/v1/sales").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/sales?paymentMethod=cash").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].paymentMethod").value("cash"));

        // 4 x 8.75 = 35.00 beats 1 x 24.50
        mockMvc.perform(get("/api/v1/sales/reports/top-products?limit=5").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("Sticks"))
                .andExpect(jsonPath("$[0].unitsSold").value(4))
                .andExpect(jsonPath("$[0].revenue").value(35.00));
    }

    @Test
    @DisplayName("adjusts and reports the stock of a product")
    void adjustsStock() throws Exception {
        FoodProduct food = givenProduct("Crocodile", "10.00", 5);

        mockMvc.perform(patch("/api/v1/foods/" + food.getId() + "/stock")
                        .contentType("application/json")
                        .content(toJson(Map.of("delta", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(15));

        mockMvc.perform(patch("/api/v1/foods/" + food.getId() + "/stock")
                        .contentType("application/json")
                        .content(toJson(Map.of("delta", -100))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/foods/low-stock?threshold=20").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stock").value(15));
    }
}
