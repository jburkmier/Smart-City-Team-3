package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.DepartmentDAO;
import edu.asu.smartcity.server.domain.CityAdministrator;
import edu.asu.smartcity.server.domain.DepartmentManager;
import edu.asu.smartcity.server.dto.DepartmentDto;
import edu.asu.smartcity.server.dto.WorkerDto;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** Runs DepartmentService against an in-memory DepartmentDAO, so no database is needed. */
public class DepartmentServiceTest {

    /** Two departments; only department 1 has a worker. */
    private static class FakeDepartmentDAO extends DepartmentDAO {
        final Map<Integer, DepartmentDto> departments = Map.of(
                1, new DepartmentDto(1, "Public Works"),
                2, new DepartmentDto(2, "Utilities"));

        FakeDepartmentDAO() {
            super(null);
        }

        @Override
        public List<DepartmentDto> findAll() {
            return List.of(departments.get(1), departments.get(2));
        }

        @Override
        public Optional<DepartmentDto> findById(int id) {
            return Optional.ofNullable(departments.get(id));
        }

        @Override
        public List<WorkerDto> findWorkers(int departmentId) {
            return departmentId == 1 ? List.of(new WorkerDto(4, "Sam Lee", "Road repair")) : List.of();
        }
    }

    private final DepartmentService service = new DepartmentService(new FakeDepartmentDAO());

    private static DepartmentManager managerOf(int departmentId) {
        DepartmentManager manager = new DepartmentManager();
        manager.setDepartmentId(departmentId);
        return manager;
    }

    @Test
    public void findAllReturnsEveryDepartment() throws Exception {
        assertEquals(2, service.findAll().size());
    }

    @Test
    public void adminSeesWorkersOfAnyDepartment() throws Exception {
        assertEquals(1, service.findWorkers(new CityAdministrator(), 1).size());
        assertEquals(0, service.findWorkers(new CityAdministrator(), 2).size());
    }

    @Test
    public void managerSeesWorkersOfOwnDepartment() throws Exception {
        assertEquals("Sam Lee", service.findWorkers(managerOf(1), 1).get(0).name());
    }

    @Test
    public void managerGetsNotFoundForAnotherDepartment() throws Exception {
        assertNotFound(() -> service.findWorkers(managerOf(1), 2));
    }

    @Test
    public void unknownDepartmentIsNotFound() throws Exception {
        assertNotFound(() -> service.findWorkers(new CityAdministrator(), 99));
    }

    private interface Call {
        void run() throws Exception;
    }

    private static void assertNotFound(Call call) throws Exception {
        try {
            call.run();
            fail("Expected 404.");
        } catch (ApiException e) {
            assertEquals(404, e.status());
            assertEquals("NOT_FOUND", e.code());
        }
    }
}
