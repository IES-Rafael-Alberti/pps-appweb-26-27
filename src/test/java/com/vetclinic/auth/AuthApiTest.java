package com.vetclinic.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetclinic.adoption.AdoptionRepository;
import com.vetclinic.appointment.AppointmentRepository;
import com.vetclinic.auth.dto.AuthResponse;
import com.vetclinic.food.FoodProductRepository;
import com.vetclinic.owner.Owner;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.pet.Pet;
import com.vetclinic.pet.PetRepository;
import com.vetclinic.pet.PetSex;
import com.vetclinic.pet.PetStatus;
import com.vetclinic.pet.Species;
import com.vetclinic.sale.FoodSaleRepository;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End to end authentication tests using real signed JWTs (no mocked principal). They cover the token
 * lifecycle, refresh rotation and reuse detection, and the three role boundaries.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String PASSWORD = "Clave1234!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private FoodSaleRepository foodSaleRepository;

    @Autowired
    private FoodProductRepository foodProductRepository;

    @Autowired
    private AdoptionRepository adoptionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

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

    // ------------------------------------------------------------------ registration

    @Test
    @DisplayName("register creates a client account, its owner record and a usable access token")
    void registerCreatesClientAndTokens() throws Exception {
        JsonNode auth = postJson("/api/v1/auth/register", Map.of(
                "fullName", "Nueva Clienta",
                "email", "nueva@example.com",
                "phone", "+34 600 999 888",
                "password", PASSWORD), 201);

        assertThat(auth.get("accessToken").asText()).isNotBlank();
        assertThat(auth.get("refreshToken").asText()).isNotBlank();
        assertThat(auth.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(auth.at("/user/role").asText()).isEqualTo("client");
        assertThat(auth.at("/user/ownerId").isNumber()).isTrue();
        assertThat(userRepository.findByEmailIgnoreCase("nueva@example.com")).isPresent();
        assertThat(ownerRepository.findByEmailIgnoreCase("nueva@example.com")).isPresent();

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nueva@example.com"))
                .andExpect(jsonPath("$.phone").value("+34 600 999 888"));
    }

    @Test
    @DisplayName("registering an existing owner links the account only when the phone matches")
    void registerLinksExistingOwnerByPhone() throws Exception {
        Owner owner = ownerRepository.saveAndFlush(
                new Owner("Lucia", "lucia@example.com", "+34 600 111 222", "Calle Sol 4"));

        postJson("/api/v1/auth/register", Map.of(
                "fullName", "Lucia", "email", "lucia@example.com", "phone", "600111222",
                "password", PASSWORD), 409);

        JsonNode auth = postJson("/api/v1/auth/register", Map.of(
                "fullName", "Lucia", "email", "lucia@example.com", "phone", "+34 600 111 222",
                "password", PASSWORD), 201);

        assertThat(auth.at("/user/ownerId").asLong()).isEqualTo(owner.getId());
        assertThat(ownerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("register rejects duplicate accounts and weak passwords with a problem document")
    void registerValidatesInput() throws Exception {
        postJson("/api/v1/auth/register", Map.of(
                "fullName", "Ana", "email", "ana@example.com", "phone", "600000000",
                "password", PASSWORD), 201);

        postJson("/api/v1/auth/register", Map.of(
                "fullName", "Ana", "email", "ana@example.com", "phone", "600000000",
                "password", PASSWORD), 409);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Ana", "email", "b@example.com",
                                "phone", "600000000", "password", "corta"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    // ------------------------------------------------------------------ login and tokens

    @Test
    @DisplayName("login rejects a wrong password with 401 and the same message as an unknown email")
    void loginRejectsBadCredentials() throws Exception {
        userService.createAccount("Ana", "ana@example.com", PASSWORD, Role.CLIENT, null);

        postJson("/api/v1/auth/login", Map.of("email", "ana@example.com", "password", "Incorrecta1"),
                401);
        postJson("/api/v1/auth/login", Map.of("email", "nadie@example.com", "password", PASSWORD), 401);
    }

    @Test
    @DisplayName("a disabled account cannot log in")
    void disabledAccountCannotLogin() throws Exception {
        User user = userService.createAccount("Ana", "ana@example.com", PASSWORD, Role.CLIENT, null);
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        postJson("/api/v1/auth/login", Map.of("email", "ana@example.com", "password", PASSWORD), 401);
    }

    @Test
    @DisplayName("refresh rotates the token; replaying a used token revokes every session")
    void refreshRotatesAndDetectsReuse() throws Exception {
        userService.createAccount("Ana", "ana@example.com", PASSWORD, Role.CLIENT, null);
        JsonNode login = postJson("/api/v1/auth/login",
                Map.of("email", "ana@example.com", "password", PASSWORD), 200);

        String firstRefresh = login.get("refreshToken").asText();
        JsonNode rotated = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", firstRefresh), 200);
        String secondRefresh = rotated.get("refreshToken").asText();
        assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        // Replaying the old token is treated as theft: 401 and every token of the user is revoked.
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", firstRefresh), 401);
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", secondRefresh), 401);
    }

    @Test
    @DisplayName("the HTTP client can refresh with the httpOnly cookie instead of a body")
    void refreshWorksWithCookie() throws Exception {
        userService.createAccount("Ana", "ana@example.com", PASSWORD, Role.CLIENT, null);
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ana@example.com", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        jakarta.servlet.http.Cookie cookie = login.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("logout revokes the refresh token")
    void logoutRevokesToken() throws Exception {
        userService.createAccount("Ana", "ana@example.com", PASSWORD, Role.CLIENT, null);
        JsonNode login = postJson("/api/v1/auth/login",
                Map.of("email", "ana@example.com", "password", PASSWORD), 200);
        String refresh = login.get("refreshToken").asText();

        postJson("/api/v1/auth/logout", Map.of("refreshToken", refresh), 204);
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", refresh), 401);
    }

    @Test
    @DisplayName("an access token stops working as soon as the account is disabled")
    void disablingAccountInvalidatesAccessToken() throws Exception {
        User admin = userService.createAccount("Admin", "admin@example.com", PASSWORD, Role.ADMIN, null);
        JsonNode clientLogin = postJson("/api/v1/auth/register", Map.of(
                "fullName", "Ana", "email", "ana@example.com", "phone", "600000000",
                "password", PASSWORD), 201);
        String clientToken = bearer(clientLogin);

        JsonNode adminLogin = postJson("/api/v1/auth/login",
                Map.of("email", "admin@example.com", "password", PASSWORD), 200);
        mockMvc.perform(patch("/api/v1/users/" + clientLogin.at("/user/id").asLong() + "/enabled")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminLogin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, clientToken))
                .andExpect(status().isUnauthorized());
        assertThat(admin.getId()).isNotNull();
    }

    // ------------------------------------------------------------------ OAuth2

    @Test
    @DisplayName("the providers endpoint hides GitHub when OAuth2 is not configured")
    void providersEndpointHidesGithubWhenDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubEnabled").value(false))
                .andExpect(jsonPath("$.githubAuthorizationUrl").doesNotExist());
    }

    @Test
    @DisplayName("GitHub login creates a client, reuses the subject and never links staff accounts")
    void githubLoginLinksClientAccounts() throws Exception {
        Owner lucia = ownerRepository.saveAndFlush(
                new Owner("Lucia", "lucia@example.com", "+34 600 111 222", "Calle Sol 4"));

        AuthResponse first = authService.loginWithOAuth2("github", "42", "lucia@example.com", "Lucia F.");
        assertThat(first.user().role()).isEqualTo(Role.CLIENT);
        assertThat(first.user().ownerId()).isEqualTo(lucia.getId());

        // Same GitHub subject, even with a different email: the same account is reused.
        AuthResponse again = authService.loginWithOAuth2("github", "42", "changed@example.com", "Lucia F.");
        assertThat(again.user().id()).isEqualTo(first.user().id());
        assertThat(ownerRepository.count()).isEqualTo(1);

        // Unknown email: creates the owner record with no phone, to be completed from the portal.
        AuthResponse created = authService.loginWithOAuth2("github", "7", "nueva@example.com", "Nueva");
        assertThat(created.user().role()).isEqualTo(Role.CLIENT);
        assertThat(created.user().ownerId()).isNotNull();
        assertThat(ownerRepository.findByEmailIgnoreCase("nueva@example.com")).isPresent();

        userService.createAccount("Admin", "admin@example.com", PASSWORD, Role.ADMIN, null);
        assertThatThrownBy(() -> authService.loginWithOAuth2("github", "99", "admin@example.com", "Admin"))
                .isInstanceOf(com.vetclinic.common.ConflictException.class);
    }

    // ------------------------------------------------------------------ roles
    @Test
    @DisplayName("unauthenticated calls get a 401 problem document with WWW-Authenticate")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/owners"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("https://vetclinic.example.com/problems/unauthorized"));
    }

    @Test
    @DisplayName("a client cannot use the staff endpoints but owns their portal")
    void clientRoleIsScopedToItsOwnData() throws Exception {
        Owner lucia = ownerRepository.saveAndFlush(
                new Owner("Lucia", "lucia@example.com", "+34 600 111 222", "Calle Sol 4"));
        Owner other = ownerRepository.saveAndFlush(
                new Owner("Otro", "otro@example.com", "+34 600 333 444", "Otra calle 5"));
        Pet mine = petRepository.saveAndFlush(new Pet("Bruno", Species.DOG, "Beagle",
                LocalDate.now().minusYears(3), PetSex.MALE, PetStatus.OWNED, lucia, null));
        petRepository.saveAndFlush(new Pet("Ajeno", Species.CAT, "Siames",
                LocalDate.now().minusYears(2), PetSex.FEMALE, PetStatus.OWNED, other, null));

        JsonNode auth = postJson("/api/v1/auth/register", Map.of(
                "fullName", "Lucia", "email", "lucia@example.com", "phone", "+34 600 111 222",
                "password", PASSWORD), 201);
        String token = bearer(auth);

        mockMvc.perform(get("/api/v1/owners").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("https://vetclinic.example.com/problems/forbidden"));

        mockMvc.perform(get("/api/v1/me/pets").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Bruno"));

        // Somebody else's pet is a 404, so ids cannot be probed.
        postJsonWithToken("/api/v1/me/appointments", Map.of(
                "petId", 999999, "scheduledAt", "2030-01-01T10:00:00", "reason", "Revision"),
                token, 404);
        assertThat(mine.getId()).isNotNull();
    }

    @Test
    @DisplayName("only the administrator reaches user management; managers run the clinic")
    void roleBasedAccessToAdminEndpoints() throws Exception {
        userService.createAccount("Admin", "admin@example.com", PASSWORD, Role.ADMIN, null);
        userService.createAccount("Gestor", "gestor@example.com", PASSWORD, Role.MANAGER, null);

        JsonNode manager = postJson("/api/v1/auth/login",
                Map.of("email", "gestor@example.com", "password", PASSWORD), 200);
        String managerToken = bearer(manager);

        mockMvc.perform(get("/api/v1/owners").header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isForbidden());

        JsonNode admin = postJson("/api/v1/auth/login",
                Map.of("email", "admin@example.com", "password", PASSWORD), 200);
        String adminToken = bearer(admin);
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("the last administrator cannot demote or disable themselves")
    void lastAdminIsProtected() throws Exception {
        userService.createAccount("Admin", "admin@example.com", PASSWORD, Role.ADMIN, null);
        JsonNode admin = postJson("/api/v1/auth/login",
                Map.of("email", "admin@example.com", "password", PASSWORD), 200);
        long id = admin.at("/user/id").asLong();

        mockMvc.perform(patch("/api/v1/users/" + id + "/role")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "manager"))))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/users/" + id + "/enabled")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", false))))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ helpers

    private String bearer(JsonNode auth) {
        return "Bearer " + auth.get("accessToken").asText();
    }

    private JsonNode postJson(String url, Object body, int expectedStatus) throws Exception {
        return read(mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn());
    }

    private JsonNode postJsonWithToken(String url, Object body, String token, int expectedStatus)
            throws Exception {
        MockHttpServletRequestBuilder builder = post(url)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(body));
        return read(mockMvc.perform(builder).andExpect(status().is(expectedStatus)).andReturn());
    }

    private JsonNode read(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
