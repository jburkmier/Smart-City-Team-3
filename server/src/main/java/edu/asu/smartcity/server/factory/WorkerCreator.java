package edu.asu.smartcity.server.factory;

import edu.asu.smartcity.server.dao.UserRecord;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.domain.Worker;

/** Factory Method pattern: ConcreteCreator for workers. */
public class WorkerCreator extends UserCreator {

    @Override
    protected SystemUser createUser(UserRecord row) {
        Worker worker = new Worker();
        worker.setDepartmentId(row.departmentId());
        worker.setSkill(row.skill());
        return worker;
    }
}
