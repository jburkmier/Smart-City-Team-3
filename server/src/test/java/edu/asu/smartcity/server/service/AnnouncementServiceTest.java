package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.AnnouncementDAO;
import edu.asu.smartcity.server.dao.NotificationDAO;
import edu.asu.smartcity.server.dao.UserDAO;
import edu.asu.smartcity.server.domain.Announcement;
import edu.asu.smartcity.server.dto.AnnouncementCreateRequest;
import edu.asu.smartcity.server.dto.AnnouncementDto;
import edu.asu.smartcity.server.model.AnnouncementType;
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

/** Checks announcement validation, posting with citizen notifications, listing, and deleting. */
public class AnnouncementServiceTest {

    private static class FakeAnnouncementDAO extends AnnouncementDAO {
        private Announcement saved;
        private boolean exists = true;
        private Integer deletedId;

        FakeAnnouncementDAO() {
            super(null);
        }

        @Override
        public Announcement create(AnnouncementType type, String title, String message, int createdById) {
            saved = new Announcement(3, type, title, message, createdById, LocalDateTime.of(2026, 11, 18, 9, 0));
            return saved;
        }

        @Override
        public List<Announcement> findActive() {
            return List.of(new Announcement(2, AnnouncementType.EMERGENCY, "Heat Advisory", "Stay hydrated.", 2,
                    LocalDateTime.of(2026, 11, 17, 7, 30)));
        }

        @Override
        public boolean delete(int announcementId) {
            deletedId = announcementId;
            return exists;
        }

        @Override
        public Optional<Announcement> update(int announcementId, AnnouncementType type, String title, String message) {
            if (!exists) {
                return Optional.empty();
            }
            saved = new Announcement(announcementId, type, title, message, 2,
                    LocalDateTime.of(2026, 11, 18, 9, 0));
            return Optional.of(saved);
        }
    }

    private static class FakeUserDAO extends UserDAO {
        private List<Integer> citizenIds = List.of(1, 9);
        private Role requestedRole;

        FakeUserDAO() {
            super(null);
        }

        @Override
        public List<Integer> findIdsByRole(Role role) {
            requestedRole = role;
            return citizenIds;
        }
    }

    private static class FakeNotificationDAO extends NotificationDAO {
        private final List<Integer> userIds = new ArrayList<>();
        private final List<Notice> notices = new ArrayList<>();
        private Integer failOnUserId;

        FakeNotificationDAO() {
            super(null);
        }

        @Override
        public void insert(int userId, Notice notice) throws SQLException {
            if (failOnUserId != null && failOnUserId == userId) {
                throw new SQLException("Connection lost", "08S01");
            }
            userIds.add(userId);
            notices.add(notice);
        }
    }

    private final FakeAnnouncementDAO announcementDAO = new FakeAnnouncementDAO();
    private final FakeUserDAO userDAO = new FakeUserDAO();
    private final FakeNotificationDAO notificationDAO = new FakeNotificationDAO();
    private final AnnouncementService service =
            new AnnouncementService(announcementDAO, userDAO, new NotificationService(notificationDAO));

    @Test
    public void createSavesTrimmedAnnouncementForTheAdministrator() throws SQLException {
        AnnouncementDto created = service.create(new AnnouncementCreateRequest(
                AnnouncementType.GENERAL, "  Pool Closures  ", "  City pools close for maintenance.  "), 2);

        assertEquals("Pool Closures", announcementDAO.saved.getTitle());
        assertEquals("City pools close for maintenance.", announcementDAO.saved.getMessage());
        assertEquals(2, announcementDAO.saved.getCreatedById());
        assertEquals(3, created.id());
        assertEquals(AnnouncementType.GENERAL, created.type());
        assertEquals("Pool Closures", created.title());
    }

    @Test
    public void generalAnnouncementNotifiesEveryCitizen() throws SQLException {
        service.create(new AnnouncementCreateRequest(AnnouncementType.GENERAL, "Pool Closures", "Details."), 2);

        assertEquals(Role.CITIZEN, userDAO.requestedRole);
        assertEquals(List.of(1, 9), notificationDAO.userIds);
        Notice notice = notificationDAO.notices.get(0);
        assertEquals(NotificationType.ANNOUNCEMENT, notice.type());
        assertEquals("New announcement: Pool Closures", notice.message());
        assertEquals(Integer.valueOf(3), notice.relatedId());
    }

    @Test
    public void emergencyAnnouncementSendsEmergencyAlerts() throws SQLException {
        service.create(new AnnouncementCreateRequest(AnnouncementType.EMERGENCY, "Heat Advisory", "Details."), 2);

        Notice notice = notificationDAO.notices.get(0);
        assertEquals(NotificationType.EMERGENCY_ALERT, notice.type());
        assertEquals("Emergency alert: Heat Advisory", notice.message());
    }

    @Test
    public void createWithNoCitizensStillPosts() throws SQLException {
        userDAO.citizenIds = List.of();

        AnnouncementDto created = service.create(
                new AnnouncementCreateRequest(AnnouncementType.GENERAL, "Pool Closures", "Details."), 2);

        assertEquals(3, created.id());
        assertTrue(notificationDAO.userIds.isEmpty());
    }

    @Test
    public void notificationFailureReportsErrorButKeepsAnnouncementAndEarlierNotifications() {
        notificationDAO.failOnUserId = 9;

        assertThrows(SQLException.class, () -> service.create(
                new AnnouncementCreateRequest(AnnouncementType.GENERAL, "Pool Closures", "Details."), 2));
        assertNotNull(announcementDAO.saved);
        assertEquals(List.of(1), notificationDAO.userIds);
    }

    @Test
    public void createRequiresTypeTitleAndMessage() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.create(new AnnouncementCreateRequest(null, "   ", null), 2));

        assertEquals(400, e.status());
        assertEquals("VALIDATION_FAILED", e.code());
        assertEquals("Is required.", e.fields().get("type"));
        assertEquals("Is required.", e.fields().get("title"));
        assertEquals("Is required.", e.fields().get("message"));
        assertNull(announcementDAO.saved);
        assertTrue(notificationDAO.userIds.isEmpty());
    }

    @Test
    public void createRejectsMissingBody() {
        ApiException e = assertThrows(ApiException.class, () -> service.create(null, 2));

        assertEquals(400, e.status());
        assertTrue(e.fields().containsKey("request"));
    }

    @Test
    public void createAllowsMaximumLengths() throws SQLException {
        service.create(new AnnouncementCreateRequest(AnnouncementType.GENERAL,
                "t".repeat(AnnouncementService.MAX_TITLE_LENGTH),
                "m".repeat(AnnouncementService.MAX_MESSAGE_LENGTH)), 2);

        assertNotNull(announcementDAO.saved);
    }

    @Test
    public void createRejectsTooLongTitleAndMessage() {
        ApiException e = assertThrows(ApiException.class, () -> service.create(new AnnouncementCreateRequest(
                AnnouncementType.GENERAL,
                "t".repeat(AnnouncementService.MAX_TITLE_LENGTH + 1),
                "m".repeat(AnnouncementService.MAX_MESSAGE_LENGTH + 1)), 2));

        assertEquals("Must be at most 150 characters.", e.fields().get("title"));
        assertEquals("Must be at most 2000 characters.", e.fields().get("message"));
    }

    @Test
    public void updateEditsAnnouncementWithoutSendingAnotherNotification() throws SQLException {
        AnnouncementDto updated = service.update(2,
                new AnnouncementCreateRequest(AnnouncementType.GENERAL, "  Updated title  ", "  Updated message  "));

        assertEquals(2, updated.id());
        assertEquals("Updated title", updated.title());
        assertEquals("Updated message", updated.message());
        assertTrue(notificationDAO.userIds.isEmpty());
    }

    @Test
    public void updateOfMissingAnnouncementReturnsNotFound() {
        announcementDAO.exists = false;

        ApiException error = assertThrows(ApiException.class, () -> service.update(99,
                new AnnouncementCreateRequest(AnnouncementType.GENERAL, "Title", "Message")));

        assertEquals(404, error.status());
        assertTrue(notificationDAO.userIds.isEmpty());
    }

    @Test
    public void lengthIsCheckedAfterTrimming() throws SQLException {
        service.create(new AnnouncementCreateRequest(AnnouncementType.GENERAL,
                "  " + "t".repeat(AnnouncementService.MAX_TITLE_LENGTH) + "  ", "Details."), 2);

        assertEquals(AnnouncementService.MAX_TITLE_LENGTH, announcementDAO.saved.getTitle().length());
    }

    @Test
    public void findActiveReturnsAnnouncementsAsDtos() throws SQLException {
        List<AnnouncementDto> announcements = service.findActive();

        assertEquals(1, announcements.size());
        assertEquals(2, announcements.get(0).id());
        assertEquals(AnnouncementType.EMERGENCY, announcements.get(0).type());
    }

    @Test
    public void deleteRemovesTheAnnouncement() throws SQLException {
        service.delete(2);

        assertEquals(Integer.valueOf(2), announcementDAO.deletedId);
    }

    @Test
    public void deleteOfMissingOrDeletedAnnouncementIsNotFound() {
        announcementDAO.exists = false;

        ApiException e = assertThrows(ApiException.class, () -> service.delete(99));

        assertEquals(404, e.status());
        assertEquals("NOT_FOUND", e.code());
    }
}
