package com.deliveryiq.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("deliveryiq-test-secret-key-32bytes!!", 3_600_000);
    }

    @Test
    void generateAndValidateToken() {
        String token = jwtService.generateToken("dispatcher", List.of("DISPATCHER", "ADMIN"));

        assertTrue(jwtService.isValid(token));
        assertEquals("dispatcher", jwtService.extractUsername(token));
        assertTrue(jwtService.hasRole(token, "ADMIN"));
        assertTrue(jwtService.hasRole(token, "ROLE_DISPATCHER"));
        assertFalse(jwtService.hasRole(token, "DRIVER"));
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertFalse(jwtService.isValid("not.a.jwt"));
    }
}
