package edu.asu.smartcity.server.domain;

/** A city employee who belongs to one department (department managers and workers). */
public abstract class StaffMember extends SystemUser {

    private Integer departmentId;

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }
}
