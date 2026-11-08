package edu.asu.smartcity.server.factory;

import edu.asu.smartcity.server.dao.UserRecord;
import edu.asu.smartcity.server.domain.CityAdministrator;
import edu.asu.smartcity.server.domain.SystemUser;

/** Factory Method pattern: ConcreteCreator for city administrators. */
public class CityAdministratorCreator extends UserCreator {

    @Override
    protected SystemUser createUser(UserRecord row) {
        return new CityAdministrator();
    }
}
