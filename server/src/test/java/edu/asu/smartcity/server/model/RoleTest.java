package edu.asu.smartcity.server.model;

import org.junit.Test;

import static org.junit.Assert.*;

/** Role ids must match the roles table in db/seed.sql. */
public class RoleTest {

    @Test
    public void idsMatchSeedData() {
        assertEquals(1, Role.CITIZEN.id());
        assertEquals(2, Role.CITY_ADMINISTRATOR.id());
        assertEquals(3, Role.DEPARTMENT_MANAGER.id());
        assertEquals(4, Role.WORKER.id());
    }

    @Test
    public void fromIdIsTheReverseOfId() {
        for (Role role : Role.values()) {
            assertEquals(role, Role.fromId(role.id()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromIdRejectsUnknownId() {
        Role.fromId(99);
    }
}
