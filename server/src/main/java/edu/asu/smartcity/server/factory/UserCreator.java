package edu.asu.smartcity.server.factory;

import edu.asu.smartcity.server.dao.UserRecord;
import edu.asu.smartcity.server.domain.SystemUser;

/**
 * Factory Method pattern: the Creator.
 * loadUser() holds the steps shared by every role; each subclass overrides the factory method
 * createUser() to decide which SystemUser subclass to build and fill in that role's own fields.
 * Use UserCreators.forRole(role) to get the creator for a database row.
 */
public abstract class UserCreator {

    /** The factory method: builds this creator's kind of user and sets its role-specific fields. */
    protected abstract SystemUser createUser(UserRecord row);

    /** Builds a complete user from a database row: the subclass's object plus the shared fields. */
    public final SystemUser loadUser(UserRecord row) {
        SystemUser user = createUser(row);
        user.setUserId(row.id());
        user.setName(row.name());
        user.setEmail(row.email());
        return user;
    }
}
