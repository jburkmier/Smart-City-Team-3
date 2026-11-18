package edu.asu.smartcity.server.service;

import edu.asu.smartcity.server.dao.NotificationDAO;
import edu.asu.smartcity.server.dto.NotificationDto;
import edu.asu.smartcity.server.model.NotificationType;
import edu.asu.smartcity.server.observer.Notice;
import edu.asu.smartcity.server.observer.Subject;
import edu.asu.smartcity.server.web.ApiException;
import org.junit.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** Checks sending notifications through the Observer pattern and the caller's notification actions. */
public class NotificationServiceTest {

    private static final Notice NOTICE =
            new Notice(NotificationType.EMERGENCY_ALERT, "Heat advisory: stay hydrated.", 2);

    private static class TestSubject extends Subject {
        @Override
        public Notice getState() {
            return NOTICE;
        }
    }

    private static class FakeNotificationDAO extends NotificationDAO {
        private final List<Integer> insertedFor = new ArrayList<>();
        private Integer failOnUserId;
        private Integer listedUserId;
        private Boolean listedUnreadOnly;
        private Integer markedNotificationId;
        private Integer markedUserId;
        private Integer allReadUserId;
        private boolean notificationExists = true;

        FakeNotificationDAO() {
            super(null);
        }

        @Override
        public void insert(int userId, Notice notice) throws SQLException {
            if (failOnUserId != null && failOnUserId == userId) {
                throw new SQLException("Connection lost", "08S01");
            }
            insertedFor.add(userId);
        }

        @Override
        public List<NotificationDto> findByUser(int userId, boolean unreadOnly) {
            listedUserId = userId;
            listedUnreadOnly = unreadOnly;
            return List.of(new NotificationDto(7, NotificationType.REQUEST_STATUS,
                    "Your request #4 (POTHOLE) is now ASSIGNED.", 4, false, LocalDateTime.now()));
        }

        @Override
        public boolean markRead(int notificationId, int userId) {
            markedNotificationId = notificationId;
            markedUserId = userId;
            return notificationExists;
        }

        @Override
        public void markAllRead(int userId) {
            allReadUserId = userId;
        }
    }

    @Test
    public void sendSavesOneNotificationPerRecipient() throws SQLException {
        FakeNotificationDAO dao = new FakeNotificationDAO();

        new NotificationService(dao).send(new TestSubject(), List.of(1, 4, 6));

        assertEquals(List.of(1, 4, 6), dao.insertedFor);
    }

    @Test
    public void sendNotifiesDuplicateRecipientsOnce() throws SQLException {
        FakeNotificationDAO dao = new FakeNotificationDAO();

        new NotificationService(dao).send(new TestSubject(), List.of(1, 1, 4));

        assertEquals(List.of(1, 4), dao.insertedFor);
    }

    @Test
    public void sendWithNoRecipientsSavesNothing() throws SQLException {
        FakeNotificationDAO dao = new FakeNotificationDAO();

        new NotificationService(dao).send(new TestSubject(), List.of());

        assertTrue(dao.insertedFor.isEmpty());
    }

    @Test
    public void sendDetachesInboxesSoTheNextSendDoesNotRepeatThem() throws SQLException {
        FakeNotificationDAO dao = new FakeNotificationDAO();
        NotificationService service = new NotificationService(dao);
        TestSubject subject = new TestSubject();

        service.send(subject, List.of(1));
        service.send(subject, List.of(4));

        assertEquals(List.of(1, 4), dao.insertedFor);
    }

    @Test
    public void sendKeepsEarlierRowsAndReportsADatabaseFailure() {
        FakeNotificationDAO dao = new FakeNotificationDAO();
        dao.failOnUserId = 4;
        NotificationService service = new NotificationService(dao);
        TestSubject subject = new TestSubject();

        assertThrows(SQLException.class, () -> service.send(subject, List.of(1, 4, 6)));
        assertEquals(List.of(1), dao.insertedFor);
    }

    @Test
    public void sendDetachesInboxesEvenAfterAFailure() throws SQLException {
        FakeNotificationDAO dao = new FakeNotificationDAO();
        dao.failOnUserId = 4;
        NotificationService service = new NotificationService(dao);
        TestSubject subject = new TestSubject();
        assertThrows(SQLException.class, () -> service.send(subject, List.of(1, 4)));

        dao.failOnUserId = null;
        dao.insertedFor.clear();
        service.send(subject, List.of(6));

        assertEquals(List.of(6), dao.insertedFor);
    }

    @Test
    public void findMinePassesTheCallerAndUnreadFilter() throws SQLException {
        FakeNotificationDAO dao = new FakeNotificationDAO();

        List<NotificationDto> notifications = new NotificationService(dao).findMine(1, true);

        assertEquals(1, notifications.size());
        assertEquals(Integer.valueOf(1), dao.listedUserId);
        assertEquals(Boolean.TRUE, dao.listedUnreadOnly);
    }

    @Test
    public void markReadMarksTheCallersNotification() throws SQLException {
        FakeNotificationDAO dao = new FakeNotificationDAO();

        new NotificationService(dao).markRead(1, 7);

        assertEquals(Integer.valueOf(7), dao.markedNotificationId);
        assertEquals(Integer.valueOf(1), dao.markedUserId);
    }

    @Test
    public void markReadOfMissingOrOtherUsersNotificationIsNotFound() {
        FakeNotificationDAO dao = new FakeNotificationDAO();
        dao.notificationExists = false;

        ApiException e = assertThrows(ApiException.class, () -> new NotificationService(dao).markRead(1, 99));

        assertEquals(404, e.status());
        assertEquals("NOT_FOUND", e.code());
    }

    @Test
    public void markAllReadMarksTheCallersNotifications() throws SQLException {
        FakeNotificationDAO dao = new FakeNotificationDAO();

        new NotificationService(dao).markAllRead(4);

        assertEquals(Integer.valueOf(4), dao.allReadUserId);
    }
}
