package edu.asu.smartcity.server.factory;

import edu.asu.smartcity.server.dao.UserRecord;
import edu.asu.smartcity.server.domain.DepartmentManager;
import edu.asu.smartcity.server.domain.SystemUser;

/** Factory Method pattern: ConcreteCreator for department managers. */
public class DepartmentManagerCreator extends UserCreator {

    @Override
    protected SystemUser createUser(UserRecord row) {
        DepartmentManager manager = new DepartmentManager();
        manager.setDepartmentId(row.departmentId());
        return manager;
    }
}
