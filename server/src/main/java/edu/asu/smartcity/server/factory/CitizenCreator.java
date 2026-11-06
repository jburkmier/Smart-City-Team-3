package edu.asu.smartcity.server.factory;

import edu.asu.smartcity.server.dao.UserRecord;
import edu.asu.smartcity.server.domain.Citizen;
import edu.asu.smartcity.server.domain.SystemUser;

/** Factory Method pattern: ConcreteCreator for citizens. */
public class CitizenCreator extends UserCreator {

    @Override
    protected SystemUser createUser(UserRecord row) {
        Citizen citizen = new Citizen();
        citizen.setAddress(row.address());
        citizen.setPhone(row.phone());
        return citizen;
    }
}
