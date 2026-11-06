package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.domain.Citizen;
import edu.asu.smartcity.server.domain.StaffMember;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.UserDto;

/** The only place a SystemUser becomes the JSON-ready UserDto. */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserDto toDto(SystemUser user) {
        String address = null;
        String phone = null;
        Integer departmentId = null;
        if (user instanceof Citizen citizen) {
            address = citizen.getAddress();
            phone = citizen.getPhone();
        } else if (user instanceof StaffMember staff) {
            departmentId = staff.getDepartmentId();
        }
        return new UserDto(user.getUserId(), user.getName(), user.getEmail(), user.getRole(),
                address, phone, departmentId);
    }
}
