package edu.asu.smartcity.server.factory;

import edu.asu.smartcity.server.model.Role;

import java.util.EnumMap;
import java.util.Map;

/**
 * Looks up the UserCreator for a role. This map is the only place that links roles to creators,
 * so adding a role means adding one entry here instead of editing switch statements.
 */
public final class UserCreators {

    private static final Map<Role, UserCreator> CREATORS = new EnumMap<>(Map.of(
            Role.CITIZEN, new CitizenCreator(),
            Role.CITY_ADMINISTRATOR, new CityAdministratorCreator(),
            Role.DEPARTMENT_MANAGER, new DepartmentManagerCreator(),
            Role.WORKER, new WorkerCreator()));

    private UserCreators() {
    }

    public static UserCreator forRole(Role role) {
        UserCreator creator = CREATORS.get(role);
        if (creator == null) {
            throw new IllegalArgumentException("No UserCreator for role " + role);
        }
        return creator;
    }
}
