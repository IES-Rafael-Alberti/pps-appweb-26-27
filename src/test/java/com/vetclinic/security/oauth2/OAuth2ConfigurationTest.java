package com.vetclinic.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The project also runs without GitHub credentials. This test turns OAuth2 on with the test registration
 * and checks that the wiring is complete: repository, authorized client storage, provider endpoint and the
 * redirect to GitHub.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
class OAuth2ConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("enabling GitHub OAuth2 exposes the provider and redirects to GitHub with state")
    void githubFlowIsWired() throws Exception {
        assertThat(context.getBeanNamesForType(ClientRegistrationRepository.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(OAuth2AuthorizedClientRepository.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(OAuth2AuthorizedClientService.class)).isNotEmpty();

        mockMvc.perform(get("/api/v1/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubEnabled").value(true))
                .andExpect(jsonPath("$.githubAuthorizationUrl")
                        .value("/oauth2/authorization/github"));

        mockMvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("github.com/login/oauth/authorize")))
                .andExpect(header().string("Location", containsString("client_id=test-github-client")))
                .andExpect(header().string("Location", containsString("state=")));
    }
}
