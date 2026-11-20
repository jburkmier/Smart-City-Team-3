package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.FlagDAO;
import edu.asu.smartcity.server.dao.NotificationDAO;
import edu.asu.smartcity.server.dao.UserDAO;
import edu.asu.smartcity.server.domain.SystemUser;
import edu.asu.smartcity.server.dto.FlagCreateRequest;
import edu.asu.smartcity.server.dto.FlagDto;
import edu.asu.smartcity.server.model.FlagContentType;
import edu.asu.smartcity.server.model.FlagStatus;
import edu.asu.smartcity.server.model.NotificationType;
import edu.asu.smartcity.server.model.Role;
import edu.asu.smartcity.server.observer.Notice;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class FlagServiceTest {

    private static class TestUser extends SystemUser {
        private final Role role;

        TestUser(Role role, int id) {
            this.role = role;
            setUserId(id);
        }

        @Override
        public Role getRole() {
            return role;
        }
    }

    private static class FakeFlagDAO extends FlagDAO {
        private boolean visible = true;
        private FlagDto flag;
        private FlagCreateRequest createdRequest;
        private int reporterId;

        FakeFlagDAO() {
            super(null);
        }

        @Override
        public boolean contentIsVisible(FlagContentType type, int contentId) {
            return visible;
        }

        @Override
        public FlagDto create(FlagCreateRequest request, int reporterId) {
            this.createdRequest = request;
            this.reporterId = reporterId;
            flag = new FlagDto(17, request.contentType(), request.contentId(), "Heat advisory",
                    request.reason(), reporterId, "Reporter", FlagStatus.OPEN, LocalDateTime.now());
            return flag;
        }

        @Override
        public List<FlagDto> findAll(FlagStatus status) {
            return List.of();
        }

        @Override
        public Optional<FlagDto> findById(int flagId) {
            return flag != null && flag.id() == flagId ? Optional.of(flag) : Optional.empty();
        }

        @Override
        public boolean moderate(int flagId, FlagStatus nextStatus) {
            if (flag == null || flag.id() != flagId || flag.status() != FlagStatus.OPEN) {
                return false;
            }
            flag = new FlagDto(flag.id(), flag.contentType(), flag.contentId(), flag.contentSummary(),
                    flag.reason(), flag.flaggedById(), flag.flaggedByName(), nextStatus, flag.createdAt());
            return true;
        }
    }

    private static class FakeUserDAO extends UserDAO {
        private Role requestedRole;

        FakeUserDAO() {
            super(null);
        }

        @Override
        public List<Integer> findIdsByRole(Role role) {
            requestedRole = role;
            return List.of(2, 4);
        }
    }

    private static class FakeNotificationDAO extends NotificationDAO {
        private final List<Integer> recipients = new ArrayList<>();
        private final List<Notice> notices = new ArrayList<>();

        FakeNotificationDAO() {
            super(null);
        }

        @Override
        public void insert(int userId, Notice notice) throws SQLException {
            recipients.add(userId);
            notices.add(notice);
        }
    }

    @Test
    public void citizenCanFlagAnnouncementAndNotifiesAdministrators() throws SQLException {
        FakeFlagDAO flags = new FakeFlagDAO();
        FakeUserDAO users = new FakeUserDAO();
        FakeNotificationDAO notificationDAO = new FakeNotificationDAO();
        FlagService service = new FlagService(flags, users, new NotificationService(notificationDAO));

        FlagDto created = service.create(
                new FlagCreateRequest(FlagContentType.ANNOUNCEMENT, 8, "  Misleading information  "),
                new TestUser(Role.CITIZEN, 12));

        assertEquals(FlagContentType.ANNOUNCEMENT, created.contentType());
        assertEquals("Misleading information", flags.createdRequest.reason());
        assertEquals(12, flags.reporterId);
        assertEquals(Role.CITY_ADMINISTRATOR, users.requestedRole);
        assertEquals(List.of(2, 4), notificationDAO.recipients);
        assertEquals(NotificationType.FLAG_SUBMITTED, notificationDAO.notices.get(0).type());
        assertEquals(Integer.valueOf(created.id()), notificationDAO.notices.get(0).relatedId());
    }

    @Test
    public void citizenCannotFlagRequest() {
        FakeFlagDAO flags = new FakeFlagDAO();
        FakeUserDAO users = new FakeUserDAO();
        FlagService service = new FlagService(flags, users, new NotificationService(new FakeNotificationDAO()));

        ApiException error = assertThrows(ApiException.class, () -> service.create(
                new FlagCreateRequest(FlagContentType.REQUEST, 3, "Spam"),
                new TestUser(Role.CITIZEN, 12)));

        assertEquals(403, error.status());
        assertNull(flags.createdRequest);
    }

    @Test
    public void missingContentCannotBeFlagged() {
        FakeFlagDAO flags = new FakeFlagDAO();
        flags.visible = false;
        FlagService service = new FlagService(flags, new FakeUserDAO(),
                new NotificationService(new FakeNotificationDAO()));

        ApiException error = assertThrows(ApiException.class, () -> service.create(
                new FlagCreateRequest(FlagContentType.EVENT, 3, "Misleading"),
                new TestUser(Role.CITIZEN, 12)));

        assertEquals(404, error.status());
        assertNull(flags.createdRequest);
    }

    @Test
    public void invalidReasonIsRejectedBeforePersistence() {
        FakeFlagDAO flags = new FakeFlagDAO();
        FlagService service = new FlagService(flags, new FakeUserDAO(),
                new NotificationService(new FakeNotificationDAO()));

        ApiException error = assertThrows(ApiException.class, () -> service.create(
                new FlagCreateRequest(FlagContentType.EVENT, 3, "  "),
                new TestUser(Role.CITIZEN, 12)));

        assertEquals(400, error.status());
        assertNull(flags.createdRequest);
    }

    @Test
    public void administratorCanDismissOpenFlag() throws SQLException {
        FakeFlagDAO flags = new FakeFlagDAO();
        FlagService service = new FlagService(flags, new FakeUserDAO(),
                new NotificationService(new FakeNotificationDAO()));
        service.create(new FlagCreateRequest(FlagContentType.EVENT, 3, "Misleading"),
                new TestUser(Role.WORKER, 12));

        FlagDto dismissed = service.dismiss(17);

        assertEquals(FlagStatus.DISMISSED, dismissed.status());
    }
}
