package com.teo1.clinicadental.security;

import com.teo1.clinicadental.model.Rol;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTests {

    private static final String TEST_SECRET =
            "VGhpcy1pcy1vbmx5LWEtbG9jYWwtZGV2ZWxvcG1lbnQtand0LXNlY3JldC1rZXk=";
    private static final String OTHER_SECRET =
            "QW5vdGhlci1sb2NhbC10ZXN0LXNlY3JldC1rZXktZm9yLWp3dC12YWxpZGF0aW9u";

    private JwtAuthenticationFilter filter;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        filter = new JwtAuthenticationFilter(new JwtService(TEST_SECRET));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutTokenRemainsUnauthenticated() throws Exception {
        executeFilter(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void validTokenLoadsSubjectAndRole(Rol rol) throws Exception {
        String token = createToken(rol, Instant.now().plus(1, ChronoUnit.HOURS), signingKey);

        executeFilter(token);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("usuario-123", authentication.getName());
        assertTrue(authentication.isAuthenticated());
        assertEquals(
                "ROLE_" + rol.name(),
                authentication.getAuthorities().iterator().next().getAuthority()
        );
    }

    @Test
    void invalidSignatureRemainsUnauthenticated() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(OTHER_SECRET));
        String token = createToken(Rol.CLIENTE, Instant.now().plus(1, ChronoUnit.HOURS), otherKey);

        executeFilter(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void expiredTokenRemainsUnauthenticated() throws Exception {
        String token = createToken(Rol.CLIENTE, Instant.now().minus(1, ChronoUnit.HOURS), signingKey);

        executeFilter(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private void executeFilter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        if (token != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        filter.doFilter(request, response, new MockFilterChain());
    }

    private String createToken(Rol rol, Instant expiration, SecretKey key) {
        return Jwts.builder()
                .subject("usuario-123")
                .claim("rol", rol.name())
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}
