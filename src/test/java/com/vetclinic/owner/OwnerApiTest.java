package com.vetclinic.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetclinic.ApiIntegrationTestBase;
import com.vetclinic.owner.dto.OwnerRequest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Owners")
class OwnerApiTest extends ApiIntegrationTestBase {

    @Test
    @DisplayName("registers a client and trims the input")
    void createsOwner() throws Exception {
        JsonNode owner = postAndRead("/api/v1/owners",
                new OwnerRequest("  Marta Ruiz  ", "Marta.Ruiz@Example.com", " +34 600 123 456 ",
                        " Calle Mayor 12 "), 201);

        assertThat(owner.get("fullName").asText()).isEqualTo("Marta Ruiz");
        assertThat(owner.get("phone").asText()).isEqualTo("+34 600 123 456");
        assertThat(owner.get("address").asText()).isEqualTo("Calle Mayor 12");
        assertThat(owner.get("createdAt").asText()).isNotBlank();
    }

    @Test
    @DisplayName("keeps the email case but compares it without case")
    void keepsEmailCase() throws Exception {
        JsonNode owner = postAndRead("/api/v1/owners",
                new OwnerRequest("Marta", "Marta.Ruiz@Example.com", "+34 600", null), 201);

        assertThat(owner.get("email").asText()).isEqualTo("Marta.Ruiz@Example.com");
    }

    @Test
    @DisplayName("rejects a duplicate email whatever its case")
    void rejectsDuplicateEmail() throws Exception {
        postAndRead("/api/v1/owners",
                new OwnerRequest("Marta", "marta@example.com", "+34 600", null), 201);

        JsonNode error = postAndRead("/api/v1/owners",
                new OwnerRequest("Otra", "MARTA@example.com", "+34 601", null), 409);
        assertThat(error.get("title").asText()).isEqualTo("Business rule violation");
        assertThat(error.get("detail").asText()).contains("already exists");
    }

    @Test
    @DisplayName("returns every field error at once")
    void reportsAllValidationErrors() throws Exception {
        JsonNode error = postAndRead("/api/v1/owners",
                Map.of("fullName", " ", "email", "not-an-email", "phone", ""), 400);

        assertThat(error.get("title").asText()).isEqualTo("Validation failed");
        assertThat(error.get("errors").get("fullName").asText()).isEqualTo("fullName is required");
        assertThat(error.get("errors").get("email").asText()).isEqualTo("email must be a valid address");
        assertThat(error.get("errors").get("phone").asText()).isEqualTo("phone is required");
        assertThat(error.get("instance").asText()).isEqualTo("/api/v1/owners");
    }

    @Test
    @DisplayName("replaces the details of a client")
    void updatesOwner() throws Exception {
        JsonNode created = postAndRead("/api/v1/owners",
                new OwnerRequest("Marta", "marta@example.com", "+34 600", null), 201);
        long id = created.get("id").asLong();

        mockMvc.perform(put("/api/v1/owners/" + id)
                        .contentType("application/json")
                        .content(toJson(new OwnerRequest("Marta Ruiz", "marta@example.com", "+34 699",
                                "Calle Nueva 3"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Marta Ruiz"))
                .andExpect(jsonPath("$.phone").value("+34 699"));

        mockMvc.perform(get("/api/v1/owners/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Calle Nueva 3"));
    }

    @Test
    @DisplayName("does not let two clients swap emails")
    void rejectsEmailTakenByAnother() throws Exception {
        postAndRead("/api/v1/owners", new OwnerRequest("A", "a@example.com", "+34 1", null), 201);
        JsonNode second = postAndRead("/api/v1/owners", new OwnerRequest("B", "b@example.com", "+34 2", null), 201);

        mockMvc.perform(put("/api/v1/owners/" + second.get("id").asLong())
                        .contentType("application/json")
                        .content(toJson(new OwnerRequest("B", "a@example.com", "+34 2", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Another owner already uses email")));
    }

    @Test
    @DisplayName("refuses to delete a client that still has pets")
    void refusesToDeleteOwnerWithPets() throws Exception {
        JsonNode created = postAndRead("/api/v1/owners",
                new OwnerRequest("Lucia", "lucia@example.com", "+34 600", null), 201);
        long id = created.get("id").asLong();
        givenOwnedPet("Bruno", ownerRepository.findById(id).orElseThrow());

        mockMvc.perform(delete("/api/v1/owners/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("still has pets")));

        assertThat(ownerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("paginates the client list")
    void paginatesOwners() throws Exception {
        givenOwner("A", "a@example.com");
        givenOwner("B", "b@example.com");
        givenOwner("C", "c@example.com");

        mockMvc.perform(get("/api/v1/owners?size=2&page=0&sort=fullName,asc").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content[0].fullName").value("A"));

        mockMvc.perform(get("/api/v1/owners?size=2&page=1&sort=fullName,asc").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("C"))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("returns 404 for an unknown client")
    void returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/owners/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", endsWith("not-found")));
    }

    @Test
    @DisplayName("rejects a malformed json body")
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/owners")
                        .contentType("application/json")
                        .content("{\"fullName\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Malformed request body")));
    }

    @Test
    @DisplayName("a new shelter animal shows up in the shelter filter and holds no owner")
    void shelterAnimalHasNoOwner() throws Exception {
        JsonNode pet = postAndRead("/api/v1/pets", Map.of(
                "name", "Nina", "species", "cat", "sex", "female", "status", "available"), 201);

        assertThat(pet.get("status").asText()).isEqualTo("available");
        assertThat(pet.get("ownerId").isNull()).isTrue();

        mockMvc.perform(get("/api/v1/pets?shelter=true").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
