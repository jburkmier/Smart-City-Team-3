package edu.asu.smartcity.server.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {

    @Test
    public void hashIsNotThePlainPasswordAndMatchesIt() {
        String hash = PasswordHasher.hash("password123");
        assertNotEquals("password123", hash);
        assertTrue(hash.startsWith("$2a$"));
        assertEquals(60, hash.length());
        assertTrue(PasswordHasher.matches("password123", hash));
    }

    @Test
    public void wrongPasswordDoesNotMatch() {
        assertFalse(PasswordHasher.matches("password124", PasswordHasher.hash("password123")));
    }

    @Test
    public void samePasswordHashesDifferentlyEachTime() {
        assertNotEquals(PasswordHasher.hash("password123"), PasswordHasher.hash("password123"));
    }
}
