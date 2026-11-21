package com.digitalfootprint.digital_footprint_backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:default-secret-key-for-ai-digital-footprint-must-be-at-least-256-bits!}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-seconds:604800}") // 7 days
    private long jwtExpirationSeconds;

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Generates a signed JWT for the given subject, user ID, and isAdmin flag.
     * The role claim is derived automatically: isAdmin = 1 -> ROLE_ADMIN, else ROLE_USER.
     */
    public String generateToken(String subject, Long userId, Integer isAdmin) {
        long now = Instant.now().getEpochSecond();
        long exp = now + jwtExpirationSeconds;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String role = (isAdmin != null && isAdmin == 1) ? "ROLE_ADMIN" : "ROLE_USER";
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"userId\":%d,\"role\":\"%s\",\"isAdmin\":%d,\"iat\":%d,\"exp\":%d}",
                subject, userId, role, isAdmin != null ? isAdmin : 0, now, exp
        );

        String headerEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String contentToSign = headerEncoded + "." + payloadEncoded;
        String signature = sign(contentToSign, jwtSecret);

        return contentToSign + "." + signature;
    }

    public String generateToken(String phoneNumber, Long userId, String role, Integer isAdmin) {
        return generateToken(phoneNumber, userId, isAdmin);
    }

    public String generateToken(String phoneNumber, Long userId, String role) {
        return generateToken(phoneNumber, userId, 0);
    }

    /**
     * Validates token format, signature, and expiration.
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) return false;
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String content = parts[0] + "." + parts[1];
            String expectedSig = sign(content, jwtSecret);
            if (!expectedSig.equals(parts[2])) {
                return false;
            }

            // Check expiration
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int expIndex = payloadJson.indexOf("\"exp\":");
            if (expIndex != -1) {
                int start = expIndex + 6;
                int end = findDelimiter(payloadJson, start);
                if (end != -1) {
                    long exp = Long.parseLong(payloadJson.substring(start, end).trim());
                    return Instant.now().getEpochSecond() < exp;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int findDelimiter(String text, int start) {
        int comma = text.indexOf(",", start);
        int brace = text.indexOf("}", start);
        if (comma != -1 && brace != -1) {
            return Math.min(comma, brace);
        }
        return (comma != -1) ? comma : brace;
    }

    /**
     * Extracts the subject (sub) from token — phone number for regular users,
     * email for admin users.
     */
    public String getPhoneNumberFromToken(String token) {
        return getSubjectFromToken(token);
    }

    /**
     * Generic subject extractor — returns the "sub" claim value.
     */
    public String getSubjectFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int subIndex = payloadJson.indexOf("\"sub\":\"");
            if (subIndex != -1) {
                int start = subIndex + 7;
                int end = payloadJson.indexOf("\"", start);
                if (end != -1) {
                    return payloadJson.substring(start, end);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the role claim from token (e.g. "ROLE_USER" or "ROLE_ADMIN").
     */
    public String getRoleFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int roleIndex = payloadJson.indexOf("\"role\":\"");
            if (roleIndex != -1) {
                int start = roleIndex + 8;
                int end = payloadJson.indexOf("\"", start);
                if (end != -1) {
                    return payloadJson.substring(start, end);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the userId claim from token.
     */
    public Long getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int idIndex = payloadJson.indexOf("\"userId\":");
            if (idIndex != -1) {
                int start = idIndex + 9;
                int end = findDelimiter(payloadJson, start);
                if (end != -1) {
                    return Long.parseLong(payloadJson.substring(start, end).trim());
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the isAdmin claim from token (0 or 1).
     */
    public Integer getIsAdminFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return 0;
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int adminIndex = payloadJson.indexOf("\"isAdmin\":");
            if (adminIndex != -1) {
                int start = adminIndex + 10;
                int end = findDelimiter(payloadJson, start);
                if (end != -1) {
                    return Integer.parseInt(payloadJson.substring(start, end).trim());
                }
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String sign(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error signing JWT token", e);
        }
    }
}
