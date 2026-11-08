package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.Role;

/**
 * Base class for every kind of user (Factory Method pattern: the Product).
 * The server builds users through a UserCreator, which picks the subclass for the role.
 * Holds no password: the hash is checked at login and never kept in memory.
 */
public abstract class SystemUser {

    private int userId;
    private String name;
    private String email;

    /** Each subclass answers with its own fixed role. */
    public abstract Role getRole();

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
