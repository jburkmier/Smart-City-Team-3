package edu.asu.smartcity.server.service;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt hashing for passwords. Plain passwords are never stored or logged. */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    /** Returns a salted BCrypt hash (60 characters, starts with "$2a$"). */
    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /** True if the password matches the stored hash. */
    public static boolean matches(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
}
