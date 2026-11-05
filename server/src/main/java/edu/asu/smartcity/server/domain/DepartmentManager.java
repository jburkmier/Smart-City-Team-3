package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.Role;

/** Runs one department: assigns tasks to its workers and views its reports. */
public class DepartmentManager extends StaffMember {

    @Override
    public Role getRole() {
        return Role.DEPARTMENT_MANAGER;
    }
}
