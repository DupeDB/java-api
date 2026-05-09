package com.dupedb.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * RFC 7636 PKCE helpers for the OAuth 2.1 Authorization Code grant.
 * Package-private — used internally by {@link OAuthFlow}.
 *
 * Charset: ALPHA / DIGIT / "-" / "." / "_" / "~"  (the unreserved set).
 * SecureRandom is used for verifier generation; SHA-256 for the challenge.
 */
final class PkceUtil {

    private PkceUtil() {}

    /**
     * Generates a 43-character base64url-no-pad code_verifier from 32
     * cryptographically random bytes. Per RFC 7636 §4.1 (43-128 chars
     * unreserved), 43 chars is the minimum-and-also-sufficient value.
     */
    static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Derives the S256 code_challenge from a verifier per RFC 7636 §4.2:
     *   code_challenge = BASE64URL-NO-PAD(SHA-256(ASCII(code_verifier)))
     */
    static String deriveCodeChallenge(String verifier) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Unreachable on JDK 25 — SHA-256 is mandated by JCA.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
