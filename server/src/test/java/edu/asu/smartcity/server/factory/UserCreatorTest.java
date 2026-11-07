package edu.asu.smartcity.server.factory;

import edu.asu.smartcity.server.dao.UserRecord;
import edu.asu.smartcity.server.domain.Citizen;
import edu.asu.smartcity.server.domain.CityAdministrator;
import edu.asu.smartcity.server.domain.DepartmentManager;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.domain.Worker;
import edu.asu.smartcity.server.model.Role;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UserCreatorTest {

    private static UserRecord row(Role role, String address, String phone, Integer departmentId, String skill) {
        return new UserRecord(7, "Test User", "test@smartcity.test", "$2a$10$hash", role,
                address, phone, departmentId, skill);
    }

    private static void assertSharedFields(SystemUser user, Role role) {
        assertEquals(7, user.getUserId());
        assertEquals("Test User", user.getName());
        assertEquals("test@smartcity.test", user.getEmail());
        assertEquals(role, user.getRole());
    }

    @Test
    public void citizenCreatorBuildsCitizen() {
        SystemUser user = new CitizenCreator().loadUser(row(Role.CITIZEN, "123 Main St", "480-555-0101", null, null));
        assertTrue(user instanceof Citizen);
        assertSharedFields(user, Role.CITIZEN);
        assertEquals("123 Main St", ((Citizen) user).getAddress());
        assertEquals("480-555-0101", ((Citizen) user).getPhone());
    }

    @Test
    public void cityAdministratorCreatorBuildsCityAdministrator() {
        SystemUser user = new CityAdministratorCreator().loadUser(row(Role.CITY_ADMINISTRATOR, null, null, null, null));
        assertTrue(user instanceof CityAdministrator);
        assertSharedFields(user, Role.CITY_ADMINISTRATOR);
    }

    @Test
    public void departmentManagerCreatorBuildsDepartmentManager() {
        SystemUser user = new DepartmentManagerCreator().loadUser(row(Role.DEPARTMENT_MANAGER, null, null, 1, null));
        assertTrue(user instanceof DepartmentManager);
        assertSharedFields(user, Role.DEPARTMENT_MANAGER);
        assertEquals(Integer.valueOf(1), ((DepartmentManager) user).getDepartmentId());
    }

    @Test
    public void workerCreatorBuildsWorker() {
        SystemUser user = new WorkerCreator().loadUser(row(Role.WORKER, null, null, 1, "Road repair"));
        assertTrue(user instanceof Worker);
        assertSharedFields(user, Role.WORKER);
        assertEquals(Integer.valueOf(1), ((Worker) user).getDepartmentId());
        assertEquals("Road repair", ((Worker) user).getSkill());
    }

    @Test
    public void citizenWithoutAddressOrPhoneHasNulls() {
        Citizen citizen = (Citizen) new CitizenCreator().loadUser(row(Role.CITIZEN, null, null, null, null));
        assertNull(citizen.getAddress());
        assertNull(citizen.getPhone());
    }

    @Test
    public void forRoleBuildsTheMatchingSubclassForEveryRole() {
        for (Role role : Role.values()) {
            SystemUser user = UserCreators.forRole(role).loadUser(row(role, null, null, null, null));
            assertEquals(role, user.getRole());
        }
    }
}
