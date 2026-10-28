package com.vetclinic.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetclinic.adoption.AdoptionRepository;
import com.vetclinic.appointment.AppointmentRepository;
import com.vetclinic.auth.RefreshTokenRepository;
import com.vetclinic.auth.Role;
import com.vetclinic.auth.UserRepository;
import com.vetclinic.auth.UserService;
import com.vetclinic.food.FoodCategory;
import com.vetclinic.food.FoodProduct;
import com.vetclinic.food.FoodProductRepository;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.pet.PetRepository;
import com.vetclinic.sale.FoodSaleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Client shop: catalogue visibility, server side pricing, stock discount and role boundaries. */
@SpringBootTest
@AutoConfigureMockMvc
class ClientShopApiTest {

    private static final String PASSWORD = "Clave1234!";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private OwnerRepository ownerRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private FoodSaleRepository foodSaleRepository;
    @Autowired private FoodProductRepository foodProductRepository;
    @Autowired private AdoptionRepository adoptionRepository;
    @Autowired private UserService userService;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        foodSaleRepository.deleteAll();
        adoptionRepository.deleteAll();
        appointmentRepository.deleteAll();
        petRepository.deleteAll();
        foodProductRepository.deleteAll();
        ownerRepository.deleteAll();
    }

    @Test
    @DisplayName("a client only sees active products and can order at server side prices")
    void clientOrdersAtCataloguePrice() throws Exception {
        FoodProduct adult = givenProduct("Adult Mini", "24.50", 10, true);
        FoodProduct withdrawn = givenProduct("Retirado", "9.99", 5, false);
        String token = registerClient("ana@example.com");
        long ownerId = ownerRepository.findByEmailIgnoreCase("ana@example.com").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/me/shop/products").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Adult Mini"))
                .andExpect(jsonPath("$.content[0].stock").value(10));

        mockMvc.perform(get("/api/v1/me/shop/products/" + withdrawn.getId())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());

        JsonNode sale = postJson("/api/v1/me/shop/orders", Map.of(
                "paymentMethod", "card",
                "notes", "Recojo el sábado",
                "lines", List.of(Map.of("productId", adult.getId(), "quantity", 2))), token, 201);

        // The client cannot influence the price even if an extra field is sent.
        assertThat(sale.get("totalAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("49.00"));
        assertThat(sale.get("customerId").asLong()).isEqualTo(ownerId);
        assertThat(sale.at("/lines/0/unitPrice").decimalValue())
                .isEqualByComparingTo(new BigDecimal("24.50"));

        assertThat(foodProductRepository.findById(adult.getId()).orElseThrow().getStock()).isEqualTo(8);
        assertThat(foodSaleRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/me/purchases").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].totalAmount").value(49.00));
    }

    @Test
    @DisplayName("an order over the available stock is a 409 and changes nothing")
    void orderBeyondStockIsRejected() throws Exception {
        FoodProduct product = givenProduct("Última bolsa", "19.40", 1, true);
        String token = registerClient("ana@example.com");

        mockMvc.perform(post("/api/v1/me/shop/orders")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("paymentMethod", "card",
                                "lines", List.of(Map.of("productId", product.getId(), "quantity", 2))))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));

        assertThat(foodProductRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(1);
        assertThat(foodSaleRepository.count()).isZero();
    }

    @Test
    @DisplayName("only clients reach the shop and validation is reported per field")
    void shopIsClientOnly() throws Exception {
        FoodProduct product = givenProduct("Adult Mini", "24.50", 3, true);
        userService.createAccount("Gestor", "gestor@example.com", PASSWORD, Role.MANAGER, null);
        JsonNode manager = postAnonymous("/api/v1/auth/login",
                Map.of("email", "gestor@example.com", "password", PASSWORD), 200);
        String managerToken = bearer(manager);

        mockMvc.perform(get("/api/v1/me/shop/products")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/me/shop/products"))
                .andExpect(status().isUnauthorized());

        String clientToken = registerClient("ana@example.com");
        mockMvc.perform(post("/api/v1/me/shop/orders")
                        .header(HttpHeaders.AUTHORIZATION, clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("paymentMethod", "card",
                                "lines", List.of(Map.of("productId", product.getId(), "quantity", 0))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['lines[0].quantity']").exists());
    }

    // ------------------------------------------------------------------ helpers

    private String registerClient(String email) throws Exception {
        JsonNode auth = postAnonymous("/api/v1/auth/register", Map.of(
                "fullName", "Ana Clienta",
                "email", email,
                "phone", "+34 600 000 000",
                "password", PASSWORD), 201);
        return bearer(auth);
    }

    private String bearer(JsonNode auth) {
        return "Bearer " + auth.get("accessToken").asText();
    }

    private FoodProduct givenProduct(String name, String price, int stock, boolean active) {
        FoodProduct product = foodProductRepository.saveAndFlush(new FoodProduct(name, "Acme",
                FoodCategory.DOG_FOOD, "Test product", new BigDecimal(price), stock, new BigDecimal("1.000")));
        if (!active) {
            product.setActive(false);
            product = foodProductRepository.saveAndFlush(product);
        }
        return product;
    }

    private JsonNode postJson(String url, Object body, String token, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return read(result);
    }

    private JsonNode postAnonymous(String url, Object body, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return read(result);
    }

    private JsonNode read(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
