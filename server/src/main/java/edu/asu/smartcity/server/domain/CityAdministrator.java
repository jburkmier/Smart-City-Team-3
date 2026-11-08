package edu.asu.smartcity.server.domain;

import edu.asu.smartcity.server.model.Role;

/** Manages events, announcements, requests and flagged content for the whole city. */
public class CityAdministrator extends SystemUser {

    @Override
    public Role getRole() {
        return Role.CITY_ADMINISTRATOR;
    }
}
