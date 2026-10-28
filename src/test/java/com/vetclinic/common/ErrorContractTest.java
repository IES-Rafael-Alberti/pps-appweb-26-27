package com.vetclinic.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetclinic.ApiIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Guards the contract that a bad request never answers 500. These regressions are easy to reintroduce
 * because the catch-all {@code @ExceptionHandler(Exception.class)} sits right below the handlers it hides.
 */
class ErrorContractTest extends ApiIntegrationTestBase {

    private static final String PROBLEM_JSON = "application/problem+json";

    // --- unknown sort column ------------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"owners", "pets", "appointments", "foods", "sales", "adoptions"})
    @DisplayName("an unknown sort column is a 400, not a 500, in every listing")
    void unknownSortColumnIsBadRequest(String module) throws Exception {
        mockMvc.perform(get("/api/v1/" + module).param("sort", "columnaInventada,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type")
                        .value("https://vetclinic.example.com/problems/bad-parameter"));
    }

    @Test
    @DisplayName("an invalid sort direction is a 400, not a 500")
    void invalidSortDirectionIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/owners").param("sort", "email,sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("a valid sort column still works")
    void validSortStillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/owners").param("sort", "email,desc"))
                .andExpect(status().isOk());
    }

    // --- HTTP protocol errors -----------------------------------------------------------------------------

    @Test
    @DisplayName("an unsupported method is a 405 with an Allow header, not a 500")
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/v1/owners"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("POST")))
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @DisplayName("POSTing to an individual resource is a 405, not a 500")
    void postToItemIsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/owners/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @DisplayName("an unsupported Content-Type is a 415, not a 500")
    void unsupportedContentTypeIsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/v1/owners")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("esto no es json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @DisplayName("an unsupported Accept header is a 406, not a 500")
    void unsupportedAcceptIsNotAcceptable() throws Exception {
        mockMvc.perform(get("/api/v1/owners").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value(406));
    }

    @Test
    @DisplayName("an unknown route is still a 404 with a problem document")
    void unknownRouteIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("a malformed body is still a 400, not swallowed by the protocol handlers")
    void malformedBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("https://vetclinic.example.com/problems/malformed-body"));
    }
}