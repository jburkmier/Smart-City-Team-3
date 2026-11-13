package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.DepartmentDAO;
import edu.asu.smartcity.server.domain.DepartmentManager;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.DepartmentDto;
import edu.asu.smartcity.server.dto.WorkerDto;
import edu.asu.smartcity.server.web.ApiException;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Department lookups. Role checks happen in the controller; this class adds the
 * "managers only see their own department" rule (docs/api.md section 3).
 */
public class DepartmentService {

    private final DepartmentDAO departmentDAO;

    public DepartmentService(DepartmentDAO departmentDAO) {
        this.departmentDAO = departmentDAO;
    }

    public List<DepartmentDto> findAll() throws SQLException {
        return departmentDAO.findAll();
    }

    /**
     * The workers in a department. A department manager asking about another department
     * gets 404, the same as a department that doesn't exist, so other departments stay hidden.
     */
    public List<WorkerDto> findWorkers(SystemUser caller, int departmentId) throws SQLException {
        if (caller instanceof DepartmentManager manager
                && !Objects.equals(manager.getDepartmentId(), departmentId)) {
            throw notFound();
        }
        if (departmentDAO.findById(departmentId).isEmpty()) {
            throw notFound();
        }
        return departmentDAO.findWorkers(departmentId);
    }

    private static ApiException notFound() {
        return new ApiException(404, "NOT_FOUND", "Department not found.");
    }
}
