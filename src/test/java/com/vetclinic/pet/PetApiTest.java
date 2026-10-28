package com.vetclinic.pet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetclinic.ApiIntegrationTestBase;
import com.vetclinic.appointment.Appointment;
import com.vetclinic.owner.Owner;
import com.vetclinic.pet.dto.PetRequest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pets")
class PetApiTest extends ApiIntegrationTestBase {

    @Test
    @DisplayName("registers a shelter animal when no owner is given")
    void createsShelterPet() throws Exception {
        JsonNode pet = postAndRead("/api/v1/pets", new PetRequest("Roco", Species.DOG, "Schnauzer",
                LocalDate.now().minusYears(4), PetSex.MALE, null, null, "Shy with strangers"), 201);

        assertThat(pet.get("status").asText()).isEqualTo("available");
        assertThat(pet.get("ownerId").isNull()).isTrue();
        assertThat(pet.get("species").asText()).isEqualTo("dog");
        assertThat(pet.get("sex").asText()).isEqualTo("male");
    }

    @Test
    @DisplayName("derives the status from the owner")
    void createsOwnedPet() throws Exception {
        Owner lucia = givenOwner("Lucia", "lucia@example.com");

        JsonNode pet = postAndRead("/api/v1/pets", new PetRequest("Bruno", Species.DOG, "Beagle",
                LocalDate.now().minusYears(3), PetSex.MALE, null, lucia.getId(), null), 201);

        assertThat(pet.get("status").asText()).isEqualTo("owned");
        assertThat(pet.get("ownerId").asLong()).isEqualTo(lucia.getId());
        assertThat(pet.get("ownerName").asText()).isEqualTo("Lucia");
    }

    @Test
    @DisplayName("rejects inconsistent status and owner combinations")
    void rejectsInconsistentStatusAndOwner() throws Exception {
        Owner lucia = givenOwner("Lucia", "lucia@example.com");

        JsonNode ownedWithoutOwner = postAndRead("/api/v1/pets", new PetRequest("X", Species.DOG, null, null,
                PetSex.MALE, PetStatus.OWNED, null, null), 400);
        assertThat(ownedWithoutOwner.get("detail").asText()).contains("status 'owned' requires ownerId");

        JsonNode availableWithOwner = postAndRead("/api/v1/pets", new PetRequest("X", Species.DOG, null, null,
                PetSex.MALE, PetStatus.AVAILABLE, lucia.getId(), null), 400);
        assertThat(availableWithOwner.get("detail").asText()).contains("cannot have an owner");

        JsonNode adopted = postAndRead("/api/v1/pets", new PetRequest("X", Species.DOG, null, null,
                PetSex.MALE, PetStatus.ADOPTED, lucia.getId(), null), 400);
        assertThat(adopted.get("detail").asText()).contains("POST /api/v1/adoptions");
    }

    @Test
    @DisplayName("rejects a missing name, an unknown species and a future birth date")
    void rejectsInvalidPayloads() throws Exception {
        JsonNode noName = postAndRead("/api/v1/pets",
                Map.of("species", "dog", "sex", "male"), 400);
        assertThat(noName.get("errors").get("name").asText()).isEqualTo("name is required");

        JsonNode badSpecies = postAndRead("/api/v1/pets", new PetRequest("X", null, null, null,
                PetSex.MALE, null, null, null), 400);
        assertThat(badSpecies.get("errors").get("species").asText()).isEqualTo("species is required");

        // the enum parser lists the accepted values in the message
        JsonNode unknownSpecies = postAndRead("/api/v1/pets", Map.of(
                "name", "X", "species", "dragon", "sex", "male"), 400);
        assertThat(unknownSpecies.get("detail").asText()).contains("not a valid Species");
        assertThat(unknownSpecies.get("detail").asText()).contains("dog, cat, bird");

        JsonNode futureBirth = postAndRead("/api/v1/pets", new PetRequest("X", Species.DOG, null,
                LocalDate.now().plusDays(5), PetSex.MALE, null, null, null), 400);
        assertThat(futureBirth.get("errors").get("birthDate").asText()).contains("cannot be in the future");
    }

    @Test
    @DisplayName("accepts lowercase enum values")
    void acceptsLowercaseEnums() throws Exception {
        JsonNode pet = postAndRead("/api/v1/pets", Map.of(
                "name", "Nina", "species", "cat", "sex", "female", "status", "available"), 201);
        assertThat(pet.get("species").asText()).isEqualTo("cat");
    }

    @Test
    @DisplayName("filters the shelter animals")
    void searchesShelterPets() throws Exception {
        givenShelterPet("Nina", Species.CAT);
        givenShelterPet("Roco", Species.DOG);
        Owner lucia = givenOwner("Lucia", "lucia@example.com");
        givenOwnedPet("Bruno", lucia);

        mockMvc.perform(get("/api/v1/pets?shelter=true").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/pets?species=dog&shelter=true").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Roco"));

        mockMvc.perform(get("/api/v1/pets?ownerId=" + lucia.getId()).accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // filtering by a client that does not exist is a 404, not an empty page
        mockMvc.perform(get("/api/v1/pets?ownerId=999999").accept("application/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("updates and returns 404 for an unknown pet")
    void updatesPet() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);

        mockMvc.perform(put("/api/v1/pets/" + nina.getId())
                        .contentType("application/json")
                        .content(toJson(new PetRequest("Nina II", Species.CAT, "Persian", null,
                                PetSex.FEMALE, null, null, "Better groomed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nina II"))
                .andExpect(jsonPath("$.breed").value("Persian"));

        mockMvc.perform(get("/api/v1/pets/999999").accept("application/json"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @DisplayName("refuses to delete a pet that has history")
    void refusesToDeletePetWithHistory() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        appointmentRepository.saveAndFlush(new Appointment(nina, java.time.LocalDateTime.now().plusDays(1),
                30, "Check-up", null, null));

        mockMvc.perform(delete("/api/v1/pets/" + nina.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("1 appointment(s)")));

        mockMvc.perform(get("/api/v1/pets/" + nina.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deletes a pet without history")
    void deletesPet() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);

        mockMvc.perform(delete("/api/v1/pets/" + nina.getId())).andExpect(status().isNoContent());
        assertThat(petRepository.count()).isZero();
    }
}
