package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.Role;

/** A resident using the app. Address and phone are optional and may be null. */
public class Citizen extends SystemUser {

    private String address;
    private String phone;

    @Override
    public Role getRole() {
        return Role.CITIZEN;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
