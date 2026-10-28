package com.vetclinic.security.oauth2;

import java.util.Arrays;
import java.util.Comparator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * GitHub only exposes the public profile email, which may be missing or unverified. The
 * {@code user:email} scope lets us read the verified addresses, so that is what the account is keyed on.
 */
@Service
public class GithubEmailService {

    private final RestClient restClient;

    public GithubEmailService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(5_000);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, "vet-clinic-api")
                .build();
    }

    public String primaryVerifiedEmail(String accessToken) {
        Email[] emails = restClient.get()
                .uri("/user/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Email[].class);

        if (emails == null) {
            throw new IllegalStateException("GitHub returned no email addresses");
        }
        return Arrays.stream(emails)
                .filter(Email::verified)
                .filter(email -> email.email() != null && !email.email().isBlank())
                .sorted(Comparator.comparing(Email::primary).reversed())
                .map(Email::email)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The GitHub account has no verified email"));
    }

    record Email(String email, boolean primary, boolean verified, String visibility) {
    }
}
