package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.Role;

/** A department employee who carries out assigned tasks. skill may be null. */
public class Worker extends StaffMember {

    private String skill;

    @Override
    public Role getRole() {
        return Role.WORKER;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }
}
