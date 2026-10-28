package com.vetclinic.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetclinic.ApiIntegrationTestBase;
import com.vetclinic.appointment.dto.AppointmentRequest;
import com.vetclinic.appointment.dto.AppointmentStatusRequest;
import com.vetclinic.pet.Pet;
import com.vetclinic.pet.Species;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Veterinary appointments")
class AppointmentApiTest extends ApiIntegrationTestBase {

    private LocalDateTime inThreeDays() {
        return LocalDateTime.now().plusDays(3).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("books a visit for a pet")
    void booksAppointment() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);

        JsonNode appointment = postAndRead("/api/v1/appointments", new AppointmentRequest(
                nina.getId(), inThreeDays(), "Annual check-up and vaccination",
                "Dra. Elena Vazquez", 30, "Bring the record"), 201);

        assertThat(appointment.get("status").asText()).isEqualTo("scheduled");
        assertThat(appointment.get("petName").asText()).isEqualTo("Nina");
        assertThat(appointment.get("petId").asLong()).isEqualTo(nina.getId());
        assertThat(appointment.get("durationMinutes").asInt()).isEqualTo(30);
    }

    @Test
    @DisplayName("rejects a date in the past or a missing reason")
    void rejectsInvalidBooking() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);

        JsonNode past = postAndRead("/api/v1/appointments", new AppointmentRequest(
                nina.getId(), LocalDateTime.now().minusDays(1), "Check-up", null, 30, null), 400);
        assertThat(past.get("errors").get("scheduledAt").asText()).contains("must be in the future");

        JsonNode noReason = postAndRead("/api/v1/appointments", Map.of(
                "petId", nina.getId(), "scheduledAt", inThreeDays().toString()), 400);
        assertThat(noReason.get("errors").get("reason").asText()).isEqualTo("reason is required");
        // durationMinutes is optional: its absence must not be reported as a validation error
        assertThat(noReason.get("errors").has("durationMinutes")).isFalse();
    }

    @Test
    @DisplayName("defaults durationMinutes to 30 when the field is omitted")
    void defaultsDuration() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);

        JsonNode appointment = postAndRead("/api/v1/appointments", Map.of(
                "petId", nina.getId(), "scheduledAt", inThreeDays().toString(),
                "reason", "Check-up"), 201);

        assertThat(appointment.get("durationMinutes").asInt()).isEqualTo(30);
    }

    @Test
    @DisplayName("still rejects an explicit duration below the minimum")
    void rejectsExplicitShortDuration() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);

        JsonNode response = postAndRead("/api/v1/appointments", Map.of(
                "petId", nina.getId(), "scheduledAt", inThreeDays().toString(),
                "reason", "Check-up", "durationMinutes", 2), 400);

        assertThat(response.get("errors").get("durationMinutes").asText()).contains("at least 5");
    }

    @Test
    @DisplayName("returns 404 for an unknown pet")
    void rejectsUnknownPet() throws Exception {
        postAndRead("/api/v1/appointments",
                new AppointmentRequest(999999L, inThreeDays(), "Check-up", null, 30, null), 404);
    }

    @Test
    @DisplayName("completes a scheduled visit and then refuses to change it")
    void completesThenFreezes() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        JsonNode created = postAndRead("/api/v1/appointments", new AppointmentRequest(
                nina.getId(), inThreeDays(), "Check-up", null, 30, null), 201);
        long id = created.get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + id + "/status")
                        .contentType("application/json")
                        .content(toJson(new AppointmentStatusRequest(AppointmentStatus.COMPLETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));

        mockMvc.perform(patch("/api/v1/appointments/" + id + "/status")
                        .contentType("application/json")
                        .content(toJson(new AppointmentStatusRequest(AppointmentStatus.SCHEDULED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Cannot move an appointment")));
    }

    @Test
    @DisplayName("cancels a visit and re-activates it")
    void cancelsAndReactivates() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        JsonNode created = postAndRead("/api/v1/appointments", new AppointmentRequest(
                nina.getId(), inThreeDays(), "Check-up", null, 30, null), 201);
        long id = created.get("id").asLong();

        mockMvc.perform(patch("/api/v1/appointments/" + id + "/status")
                        .contentType("application/json")
                        .content(toJson(new AppointmentStatusRequest(AppointmentStatus.CANCELLED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));

        mockMvc.perform(patch("/api/v1/appointments/" + id + "/status")
                        .contentType("application/json")
                        .content(toJson(new AppointmentStatusRequest(AppointmentStatus.SCHEDULED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("scheduled"));
    }

    @Test
    @DisplayName("rejects a no-op status change")
    void rejectsNoOpStatusChange() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        JsonNode created = postAndRead("/api/v1/appointments", new AppointmentRequest(
                nina.getId(), inThreeDays(), "Check-up", null, 30, null), 201);

        mockMvc.perform(patch("/api/v1/appointments/" + created.get("id").asLong() + "/status")
                        .contentType("application/json")
                        .content(toJson(new AppointmentStatusRequest(AppointmentStatus.SCHEDULED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("already 'scheduled'")));
    }

    @Test
    @DisplayName("filters appointments by pet and status")
    void filtersAppointments() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        Pet luna = givenShelterPet("Luna", Species.CAT);
        postAndRead("/api/v1/appointments",
                new AppointmentRequest(nina.getId(), inThreeDays(), "Check-up", null, 30, null), 201);
        postAndRead("/api/v1/appointments",
                new AppointmentRequest(luna.getId(), inThreeDays().plusDays(1), "Vaccine", null, 30, null), 201);

        mockMvc.perform(get("/api/v1/appointments?petId=" + nina.getId()).accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/appointments?status=scheduled").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("rejects a reversed date range")
    void rejectsReversedRange() throws Exception {
        mockMvc.perform(get("/api/v1/appointments?from=" + inThreeDays() + "&to=" + LocalDateTime.now())
                        .accept("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("must not be after")));
    }

    @Test
    @DisplayName("keeps completed visits in the medical record")
    void cannotDeleteCompletedVisit() throws Exception {
        Pet nina = givenShelterPet("Nina", Species.CAT);
        JsonNode created = postAndRead("/api/v1/appointments", new AppointmentRequest(
                nina.getId(), inThreeDays(), "Check-up", null, 30, null), 201);
        long id = created.get("id").asLong();

        mockMvc.perform(delete("/api/v1/appointments/" + id)).andExpect(status().isNoContent());

        JsonNode second = postAndRead("/api/v1/appointments", new AppointmentRequest(
                nina.getId(), inThreeDays().plusDays(1), "Vaccine", null, 30, null), 201);
        long secondId = second.get("id").asLong();
        mockMvc.perform(patch("/api/v1/appointments/" + secondId + "/status")
                        .contentType("application/json")
                        .content(toJson(new AppointmentStatusRequest(AppointmentStatus.COMPLETED))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/appointments/" + secondId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("medical record")));
    }
}
