package com.vetclinic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vetClinicOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Veterinary Clinic API")
                        .version("1.0.0")
                        .description("""
                                REST API for a veterinary clinic.

                                **Modules**

                                * `owners` — clients of the clinic.
                                * `pets` — client pets and shelter animals, with search filters.
                                * `appointments` — veterinary visits with a `scheduled -> completed|cancelled` \
                                state machine.
                                * `foods` — shop catalogue and stock.
                                * `sales` — shop tickets. Prices and totals are always computed on the server \
                                and the stock is discounted atomically.
                                * `adoptions` — adoption of shelter animals. Only `available` pets can be \
                                adopted, and the adopter becomes the new owner.
                                * `auth` — registration, login, rotating refresh tokens and logout, plus \
                                GitHub OAuth2 login.
                                * `users` — admin only account and role management.
                                * `me` — the client portal: own profile, pets, appointments, purchases.
                                * `me/shop` — the food shop for clients: catalogue and online orders.

                                **Roles** are `admin` (everything, including users), `manager` (clinic \
                                operations) and `client` (only their own data).

                                **Errors** follow RFC 7807 (`application/problem+json`).""")
                        .contact(new Contact().name("Veterinary Clinic").email("clinic@vetclinic.example.com"))
                        .license(new License().name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token returned by POST /api/v1/auth/login")))
                .servers(List.of(new Server().url("/").description("Current server")));
    }
}
