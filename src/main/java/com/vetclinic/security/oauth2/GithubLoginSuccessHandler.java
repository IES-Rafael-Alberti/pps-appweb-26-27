package com.vetclinic.security.oauth2;

import com.vetclinic.auth.AuthService;
import com.vetclinic.auth.dto.AuthResponse;
import com.vetclinic.common.ConflictException;
import com.vetclinic.common.UnauthorizedException;
import com.vetclinic.security.RefreshCookie;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Turns a successful GitHub login into the same session the password login produces: a local client
 * account (created or linked) and the httpOnly refresh cookie. The browser is then sent back to the SPA,
 * which asks for an access token through the normal refresh endpoint. No token ever travels in a URL.
 */
@Component
public class GithubLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GithubLoginSuccessHandler.class);
    private static final String PROVIDER = "github";

    private final AuthService authService;
    private final GithubEmailService emailService;
    private final RefreshCookie refreshCookie;
    private final OAuth2AuthorizedClientRepository authorizedClients;
    private final String successRedirect;
    private final String failureRedirect;

    public GithubLoginSuccessHandler(AuthService authService,
                                     GithubEmailService emailService,
                                     RefreshCookie refreshCookie,
                                     OAuth2AuthorizedClientRepository authorizedClients,
                                     @Value("${app.security.oauth2.success-redirect}") String successRedirect,
                                     @Value("${app.security.oauth2.failure-redirect}") String failureRedirect) {
        this.authService = authService;
        this.emailService = emailService;
        this.refreshCookie = refreshCookie;
        this.authorizedClients = authorizedClients;
        this.successRedirect = successRedirect;
        this.failureRedirect = failureRedirect;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        try {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient client = authorizedClients.loadAuthorizedClient(
                    token.getAuthorizedClientRegistrationId(), authentication, request);
            OAuth2User principal = token.getPrincipal();

            if (client == null) {
                throw new IllegalStateException("No authorized GitHub client in the session");
            }

            String subject = String.valueOf(principal.getAttributes().get("id"));
            String displayName = firstNonBlank(
                    stringAttribute(principal, "name"),
                    stringAttribute(principal, "login"),
                    "Cliente GitHub");
            String email = emailService.primaryVerifiedEmail(client.getAccessToken().getTokenValue());

            AuthResponse auth = authService.loginWithOAuth2(PROVIDER, subject, email, displayName);
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.create(auth.refreshToken(),
                    auth.refreshExpiresIn()).toString());
            log.info("GitHub login for {} (user {})", email, auth.user().id());
            getRedirectStrategy().sendRedirect(request, response, successRedirect);
        } catch (ConflictException ex) {
            redirectFailure(request, response, "oauth_staff_account");
        } catch (UnauthorizedException ex) {
            redirectFailure(request, response, "oauth_disabled");
        } catch (Exception ex) {
            log.warn("GitHub login failed: {}", ex.getMessage(), ex);
            redirectFailure(request, response, "oauth_error");
        }
    }

    private String stringAttribute(OAuth2User principal, String name) {
        Object value = principal.getAttributes().get(name);
        return value == null ? null : value.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void redirectFailure(HttpServletRequest request, HttpServletResponse response, String code)
            throws IOException {
        String separator = failureRedirect.contains("?") ? "&" : "?";
        String target = failureRedirect + separator + "oauthError="
                + URLEncoder.encode(code, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
