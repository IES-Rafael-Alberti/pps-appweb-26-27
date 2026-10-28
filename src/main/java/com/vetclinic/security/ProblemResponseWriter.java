package com.vetclinic.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Writes RFC 7807 documents from inside the servlet filters. Spring Security errors never reach
 * {@code @RestControllerAdvice}, so without this the client would get an empty body or the default
 * white-label error page instead of the API's usual problem shape.
 */
public final class ProblemResponseWriter {

    private static final String BASE_TYPE = "https://vetclinic.example.com/problems/";

    private ProblemResponseWriter() {
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, ObjectMapper objectMapper,
                             HttpStatus status, String title, String detail, String type) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_TYPE + type));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());

        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
