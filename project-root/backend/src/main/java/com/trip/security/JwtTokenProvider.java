package com.trip.security;

import com.trip.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expireMinutes;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expire-minutes}") long expireMinutes) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expireMinutes = expireMinutes;
    }

    public String generateToken(User user) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expireMinutes * 60;

        String headerJson = toJson(Map.of("alg", "HS256", "typ", "JWT"));
        String payloadJson = toJson(Map.of(
                "sub", String.valueOf(user.getId()),
                "username", nullToEmpty(user.getUsername()),
                "role", nullToEmpty(user.getRole()),
                "iat", issuedAt,
                "exp", expiresAt));

        String header = encode(headerJson);
        String payload = encode(payloadJson);
        String signingInput = header + "." + payload;
        return signingInput + "." + sign(signingInput);
    }

    public JwtClaims parseToken(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT structure");
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            JsonNode header = objectMapper.readTree(decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText()) || !"JWT".equals(header.path("typ").asText())) {
                throw new IllegalArgumentException("Invalid JWT header");
            }

            JsonNode payload = objectMapper.readTree(decode(parts[1]));
            Long userId = payload.path("sub").asLong();
            String username = payload.path("username").asText();
            String role = payload.path("role").asText();
            long issuedAt = payload.path("iat").asLong();
            long expiresAt = payload.path("exp").asLong();

            if (userId == null || userId <= 0 || expiresAt <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Invalid or expired JWT payload");
            }
            return new JwtClaims(userId, username, role, issuedAt, expiresAt);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JWT token", exception);
        }
    }

    private String encode(String value) {
        return BASE64_URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(BASE64_URL_DECODER.decode(value), StandardCharsets.UTF_8);
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT token", exception);
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to build JWT payload", exception);
        }
    }

    private String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
