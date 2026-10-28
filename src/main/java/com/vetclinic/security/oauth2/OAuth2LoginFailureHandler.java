package com.vetclinic.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** Redirects a denied or broken GitHub handshake back to the SPA login screen with a generic code. */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private final String failureRedirect;

    public OAuth2LoginFailureHandler(
            @Value("${app.security.oauth2.failure-redirect}") String failureRedirect) {
        this.failureRedirect = failureRedirect;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 login failed: {}", exception.getMessage());
        String separator = failureRedirect.contains("?") ? "&" : "?";
        response.sendRedirect(failureRedirect + separator + "oauthError=github_denied");
    }
}
