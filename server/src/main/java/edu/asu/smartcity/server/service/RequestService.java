package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.RequestDAO;
import edu.asu.smartcity.server.dto.RequestDto;
import edu.asu.smartcity.server.model.RequestStatus;
import edu.asu.smartcity.server.model.RequestType;

import java.sql.SQLException;
import java.util.List;

/** Coordinates service-request listing. */
public class RequestService {

    private final RequestDAO requests;

    public RequestService(RequestDAO requests) {
        this.requests = requests;
    }

    public List<RequestDto> findByCitizen(int citizenId) throws SQLException {
        return requests.findByCitizen(citizenId);
    }

    public List<RequestDto> findForAdministrator(RequestStatus status, RequestType type, Integer departmentId)
            throws SQLException {
        return requests.findForAdministrator(status, type, departmentId);
    }

    public List<RequestDto> findForDepartmentManager(int departmentId, RequestStatus status, RequestType type)
            throws SQLException {
        return requests.findForDepartmentManager(departmentId, status, type);
    }
}
