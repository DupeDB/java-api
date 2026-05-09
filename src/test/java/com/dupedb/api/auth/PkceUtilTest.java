package com.dupedb.api.auth;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PkceUtilTest {

    // RFC 7636 unreserved charset
    private static final Pattern UNRESERVED = Pattern.compile("^[A-Za-z0-9\\-._~]+$");

    @Test
    void verifierLengthIs43Chars() {
        String v = PkceUtil.generateCodeVerifier();
        assertEquals(43, v.length(), "32 random bytes → 43 base64url-no-pad chars");
    }

    @Test
    void verifierUnreservedCharsetOnly() {
        String v = PkceUtil.generateCodeVerifier();
        assertTrue(UNRESERVED.matcher(v).matches(),
            "verifier must use unreserved charset (RFC 7636 §4.1): " + v);
    }

    @Test
    void challengeMatchesS256OfVerifier() {
        // Known fixture from RFC 7636 §4.6 example
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String expected = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
        assertEquals(expected, PkceUtil.deriveCodeChallenge(verifier),
            "S256 challenge must match RFC 7636 §4.6 reference vector");
    }

    @Test
    void differentVerifiersProduceDifferentChallenges() {
        String c1 = PkceUtil.deriveCodeChallenge(PkceUtil.generateCodeVerifier());
        String c2 = PkceUtil.deriveCodeChallenge(PkceUtil.generateCodeVerifier());
        assertNotEquals(c1, c2, "fresh verifiers must hash to different challenges");
    }

    @Test
    void deterministicMapping() {
        String v = PkceUtil.generateCodeVerifier();
        assertEquals(PkceUtil.deriveCodeChallenge(v), PkceUtil.deriveCodeChallenge(v),
            "same verifier must always derive the same challenge");
    }
}
