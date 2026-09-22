package com.phi.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.phi.config.PhiProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleTokenVerifier {

    private final RestClient restClient = RestClient.create();
    private final String clientId;

    public GoogleTokenVerifier(PhiProperties properties) {
        this.clientId = properties.google() != null ? properties.google().clientId() : null;
    }

    public GoogleProfile verify(String idToken) {
        String encoded = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
        GoogleTokenInfo info = restClient.get()
                .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encoded))
                .retrieve()
                .body(GoogleTokenInfo.class);

        if (info == null || info.email == null || info.email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }
        if (clientId != null && !clientId.isBlank() && !clientId.equals(info.aud)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token audience mismatch");
        }
        if (!"true".equalsIgnoreCase(info.emailVerified)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email not verified");
        }

        return new GoogleProfile(info.sub, info.email.toLowerCase(), info.name, info.picture);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenInfo(
            String aud,
            String sub,
            String email,
            @JsonProperty("email_verified") String emailVerified,
            String name,
            String picture
    ) {
    }

    public record GoogleProfile(String subject, String email, String name, String pictureUrl) {
    }
}
