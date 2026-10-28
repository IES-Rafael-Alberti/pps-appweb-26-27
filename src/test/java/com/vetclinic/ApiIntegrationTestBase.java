package com.vetclinic;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetclinic.adoption.AdoptionRepository;
import com.vetclinic.appointment.AppointmentRepository;
import com.vetclinic.auth.RefreshTokenRepository;
import com.vetclinic.auth.UserRepository;
import com.vetclinic.food.FoodCategory;
import com.vetclinic.food.FoodProduct;
import com.vetclinic.food.FoodProductRepository;
import com.vetclinic.owner.Owner;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.pet.Pet;
import com.vetclinic.pet.PetRepository;
import com.vetclinic.pet.PetSex;
import com.vetclinic.pet.Species;
import com.vetclinic.sale.FoodSaleRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Base class for the HTTP level tests: boots the whole app against an in memory H2 and empties it per test. */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin@vetclinic.example", roles = "ADMIN")
public abstract class ApiIntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected OwnerRepository ownerRepository;

    @Autowired
    protected PetRepository petRepository;

    @Autowired
    protected FoodProductRepository foodProductRepository;

    @Autowired
    protected FoodSaleRepository foodSaleRepository;

    @Autowired
    protected AppointmentRepository appointmentRepository;

    @Autowired
    protected AdoptionRepository adoptionRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected com.vetclinic.sale.FoodSaleService foodSaleService;

    @BeforeEach
    void cleanDatabase() {
        // children first, otherwise the foreign keys block the deletes
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        foodSaleRepository.deleteAll();
        adoptionRepository.deleteAll();
        appointmentRepository.deleteAll();
        petRepository.deleteAll();
        foodProductRepository.deleteAll();
        ownerRepository.deleteAll();
    }

    // ---------------------------------------------------------------- factories

    protected Owner givenOwner(String fullName, String email) {
        return ownerRepository.saveAndFlush(new Owner(fullName, email, "+34 600 000 000", "Test street 1"));
    }

    protected Pet givenShelterPet(String name, Species species) {
        return petRepository.saveAndFlush(
                new Pet(name, species, "Mixed", java.time.LocalDate.now().minusYears(2), PetSex.UNKNOWN,
                        com.vetclinic.pet.PetStatus.AVAILABLE, null, "Looking for a home"));
    }

    protected Pet givenOwnedPet(String name, Owner owner) {
        return petRepository.saveAndFlush(
                new Pet(name, Species.DOG, "Mixed", java.time.LocalDate.now().minusYears(1), PetSex.MALE,
                        com.vetclinic.pet.PetStatus.OWNED, owner, null));
    }

    protected FoodProduct givenProduct(String name, String price, int stock) {
        return givenProduct(name, price, stock, FoodCategory.DOG_FOOD);
    }

    protected FoodProduct givenProduct(String name, String price, int stock, FoodCategory category) {
        return foodProductRepository.saveAndFlush(new FoodProduct(name, "Acme", category, "Test product",
                new BigDecimal(price), stock, new BigDecimal("1.000")));
    }

    // ---------------------------------------------------------------- http helpers

    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode postAndRead(String url, Object body, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    protected JsonNode getAndRead(String url, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(get(url).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }
}
