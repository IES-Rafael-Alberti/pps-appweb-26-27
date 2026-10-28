package com.vetclinic.adoption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetclinic.ApiIntegrationTestBase;
import com.vetclinic.adoption.dto.AdoptionActionRequest;
import com.vetclinic.adoption.dto.CreateAdoptionRequest;
import com.vetclinic.owner.Owner;
import com.vetclinic.pet.Pet;
import com.vetclinic.pet.PetStatus;
import com.vetclinic.pet.Species;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pet adoption")
class AdoptionApiTest extends ApiIntegrationTestBase {

    @Test
    @DisplayName("adopts an available animal and makes the adopter its owner")
    void adoptsAvailablePet() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        Owner carlos = givenOwner("Carlos Gil", "carlos@example.com");

        JsonNode adoption = postAndRead("/api/v1/adoptions", new CreateAdoptionRequest(
                nina.getId(), carlos.getId(), null, new BigDecimal("85.00"), "Picked up on Saturday"), 201);

        assertThat(adoption.get("status").asText()).isEqualTo("completed");
        assertThat(adoption.get("petName").asText()).isEqualTo("Nina");
        assertThat(adoption.get("petSpecies").asText()).isEqualTo("cat");
        assertThat(adoption.get("adopterName").asText()).isEqualTo("Carlos Gil");
        assertThat(adoption.get("fee").decimalValue()).isEqualByComparingTo("85.00");

        Pet stored = petRepository.findById(nina.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PetStatus.ADOPTED);
        assertThat(stored.getOwner()).isNotNull();
        assertThat(stored.getOwner().getId()).isEqualTo(carlos.getId());
    }

    @Test
    @DisplayName("the same animal cannot be adopted twice")
    void rejectsSecondAdoptionOfTheSamePet() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        Owner first = givenOwner("Carlos", "carlos@example.com");
        Owner second = givenOwner("Marta", "marta@example.com");

        postAndRead("/api/v1/adoptions",
                new CreateAdoptionRequest(nina.getId(), first.getId(), null, null, null), 201);

        JsonNode error = postAndRead("/api/v1/adoptions",
                new CreateAdoptionRequest(nina.getId(), second.getId(), null, null, null), 409);

        assertThat(error.get("title").asText()).isEqualTo("Business rule violation");
        assertThat(error.get("detail").asText()).contains("cannot be adopted");
        assertThat(adoptionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("only animals waiting for adoption can be adopted")
    void rejectsAdoptionOfAnOwnedPet() throws Exception {
        Owner lucia = givenOwner("Lucia", "lucia@example.com");
        Owner carlos = givenOwner("Carlos", "carlos@example.com");
        Pet bruno = givenOwnedPet("Bruno", lucia);

        JsonNode error = postAndRead("/api/v1/adoptions",
                new CreateAdoptionRequest(bruno.getId(), carlos.getId(), null, null, null), 409);

        assertThat(error.get("detail").asText()).contains("is 'owned' and cannot be adopted");
    }

    @Test
    @DisplayName("cancelling puts the animal back in the shelter")
    void cancelRestoresAvailability() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        Owner carlos = givenOwner("Carlos", "carlos@example.com");
        JsonNode created = postAndRead("/api/v1/adoptions",
                new CreateAdoptionRequest(nina.getId(), carlos.getId(), null, null, null), 201);
        long adoptionId = created.get("id").asLong();

        mockMvc.perform(post("/api/v1/adoptions/" + adoptionId + "/cancel")
                        .contentType("application/json")
                        .content(toJson(new AdoptionActionRequest("Paperwork error"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("canceled"))
                .andExpect(jsonPath("$.notes", containsString("Paperwork error")));

        Pet stored = petRepository.findById(nina.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PetStatus.AVAILABLE);
        assertThat(stored.getOwner()).isNull();

        // and it can be adopted again
        postAndRead("/api/v1/adoptions", new CreateAdoptionRequest(nina.getId(), carlos.getId(), null, null, null),
                201);
    }

    @Test
    @DisplayName("a returned animal goes back to the shelter")
    void returnRestoresAvailability() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        Owner carlos = givenOwner("Carlos", "carlos@example.com");
        JsonNode created = postAndRead("/api/v1/adoptions",
                new CreateAdoptionRequest(nina.getId(), carlos.getId(), null, null, null), 201);

        mockMvc.perform(post("/api/v1/adoptions/" + created.get("id").asLong() + "/return")
                        .contentType("application/json")
                        .content(toJson(Map.of("reason", "Allergies in the new home"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("returned"));

        assertThat(petRepository.findById(nina.getId()).orElseThrow().getStatus())
                .isEqualTo(PetStatus.AVAILABLE);
    }

    @Test
    @DisplayName("an adoption can only be cancelled once")
    void cannotCancelTwice() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        Owner carlos = givenOwner("Carlos", "carlos@example.com");
        JsonNode created = postAndRead("/api/v1/adoptions",
                new CreateAdoptionRequest(nina.getId(), carlos.getId(), null, null, null), 201);
        long id = created.get("id").asLong();

        mockMvc.perform(post("/api/v1/adoptions/" + id + "/cancel")
                .contentType("application/json").content("{}")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/adoptions/" + id + "/cancel")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("already 'canceled'")));
    }

    @Test
    @DisplayName("a completed adoption cannot be deleted, a closed one can")
    void deletionRules() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        Owner carlos = givenOwner("Carlos", "carlos@example.com");
        JsonNode created = postAndRead("/api/v1/adoptions",
                new CreateAdoptionRequest(nina.getId(), carlos.getId(), null, null, null), 201);
        long id = created.get("id").asLong();

        mockMvc.perform(delete("/api/v1/adoptions/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("still in effect")));

        mockMvc.perform(post("/api/v1/adoptions/" + id + "/cancel")
                .contentType("application/json").content("{}")).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/adoptions/" + id)).andExpect(status().isNoContent());
        assertThat(adoptionRepository.count()).isZero();
    }

    @Test
    @DisplayName("returns 404 for unknown pets and adopters")
    void rejectsUnknownReferences() throws Exception {
        Owner carlos = givenOwner("Carlos", "carlos@example.com");
        Pet nina = givenShelterPet("Nina", Species.CAT);

        postAndRead("/api/v1/adoptions", new CreateAdoptionRequest(555555L, carlos.getId(), null, null, null), 404);
        postAndRead("/api/v1/adoptions", new CreateAdoptionRequest(nina.getId(), 555555L, null, null, null), 404);
    }

    @Test
    @DisplayName("lists adoptions filtered by status")
    void listsAdoptions() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        Owner carlos = givenOwner("Carlos", "carlos@example.com");
        postAndRead("/api/v1/adoptions", new CreateAdoptionRequest(nina.getId(), carlos.getId(), null, null, null),
                201);

        mockMvc.perform(get("/api/v1/adoptions?status=completed").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/adoptions?status=canceled").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
