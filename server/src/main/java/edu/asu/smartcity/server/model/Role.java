package edu.asu.smartcity.server.model;

/**
 * The four kinds of users (docs/api.md section 9).
 * Each role has a fixed id that matches the roles table in db/seed.sql.
 */
public enum Role {
    CITIZEN(1),
    CITY_ADMINISTRATOR(2),
    DEPARTMENT_MANAGER(3),
    WORKER(4);

    private final int id;

    Role(int id) {
        this.id = id;
    }

    /** The role_id stored in the database. */
    public int id() {
        return id;
    }

    /** Looks up a role by its database role_id. */
    public static Role fromId(int id) {
        for (Role role : values()) {
            if (role.id == id) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role_id: " + id);
    }
}
