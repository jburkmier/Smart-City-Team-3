package edu.asu.smartcity.server.observer;

import edu.asu.smartcity.server.dao.NotificationDAO;
import edu.asu.smartcity.server.model.NotificationType;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** Checks the Observer pattern classes: Subject, Notice, and the UserInbox observer. */
public class ObserverPatternTest {

    private static final Notice NOTICE =
            new Notice(NotificationType.REQUEST_STATUS, "Your request #4 (POTHOLE) is now ASSIGNED.", 4);

    /** A subject whose state is a fixed notice. */
    private static class TestSubject extends Subject {
        @Override
        public Notice getState() {
            return NOTICE;
        }
    }

    /** An observer that records each subject it is updated with. */
    private static class RecordingObserver implements Observer {
        private final List<Subject> updates = new ArrayList<>();

        @Override
        public void update(Subject subject) {
            updates.add(subject);
        }
    }

    /** Records inserted rows instead of writing to the database. */
    private static class FakeNotificationDAO extends NotificationDAO {
        private final List<Integer> userIds = new ArrayList<>();
        private final List<Notice> notices = new ArrayList<>();

        FakeNotificationDAO() {
            super(null);
        }

        @Override
        public void insert(int userId, Notice notice) {
            userIds.add(userId);
            notices.add(notice);
        }
    }

    @Test
    public void notifyObserversUpdatesEveryAttachedObserverOnceWithTheSubject() throws SQLException {
        TestSubject subject = new TestSubject();
        RecordingObserver first = new RecordingObserver();
        RecordingObserver second = new RecordingObserver();
        subject.attach(first);
        subject.attach(second);

        subject.notifyObservers();

        assertEquals(List.of(subject), first.updates);
        assertEquals(List.of(subject), second.updates);
    }

    @Test
    public void detachedObserverIsNotUpdated() throws SQLException {
        TestSubject subject = new TestSubject();
        RecordingObserver kept = new RecordingObserver();
        RecordingObserver detached = new RecordingObserver();
        subject.attach(kept);
        subject.attach(detached);
        subject.detach(detached);

        subject.notifyObservers();

        assertEquals(1, kept.updates.size());
        assertTrue(detached.updates.isEmpty());
    }

    @Test
    public void attachingTheSameObserverTwiceNotifiesItOnce() throws SQLException {
        TestSubject subject = new TestSubject();
        RecordingObserver observer = new RecordingObserver();
        subject.attach(observer);
        subject.attach(observer);

        subject.notifyObservers();

        assertEquals(1, observer.updates.size());
    }

    @Test
    public void notifyingWithNoObserversDoesNothing() throws SQLException {
        new TestSubject().notifyObservers();
    }

    @Test(expected = IllegalArgumentException.class)
    public void attachingNullIsRejected() {
        new TestSubject().attach(null);
    }

    @Test
    public void observerCanDetachItselfDuringUpdate() throws SQLException {
        TestSubject subject = new TestSubject();
        RecordingObserver after = new RecordingObserver();
        Observer selfDetaching = new Observer() {
            @Override
            public void update(Subject changed) {
                changed.detach(this);
            }
        };
        subject.attach(selfDetaching);
        subject.attach(after);

        subject.notifyObservers();

        assertEquals(1, after.updates.size());
    }

    @Test
    public void userInboxSavesTheSubjectStateForItsUser() throws SQLException {
        FakeNotificationDAO dao = new FakeNotificationDAO();
        TestSubject subject = new TestSubject();
        subject.attach(new UserInbox(1, dao));

        subject.notifyObservers();

        assertEquals(List.of(1), dao.userIds);
        assertEquals(List.of(NOTICE), dao.notices);
    }

    @Test
    public void noticeAllowsMissingRelatedId() {
        Notice notice = new Notice(NotificationType.ANNOUNCEMENT, "City offices are closed Friday.", null);

        assertNull(notice.relatedId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void noticeRequiresType() {
        new Notice(null, "Message", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noticeRequiresMessage() {
        new Notice(NotificationType.ANNOUNCEMENT, "   ", 1);
    }

    @Test
    public void noticeAllowsMessageAtColumnLimit() {
        new Notice(NotificationType.ANNOUNCEMENT, "a".repeat(Notice.MAX_MESSAGE_LENGTH), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noticeRejectsMessageLongerThanColumn() {
        new Notice(NotificationType.ANNOUNCEMENT, "a".repeat(Notice.MAX_MESSAGE_LENGTH + 1), 1);
    }
}
