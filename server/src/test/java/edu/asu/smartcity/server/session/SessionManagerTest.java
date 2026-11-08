package edu.asu.smartcity.server.session;

import edu.asu.smartcity.server.domain.Citizen;
import edu.asu.smartcity.server.domain.SystemUser;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class SessionManagerTest {

    private final SessionManager sessions = SessionManager.instance();

    private static SystemUser citizen(int id) {
        Citizen citizen = new Citizen();
        citizen.setUserId(id);
        return citizen;
    }

    @Test
    public void instanceAlwaysReturnsTheSameObject() {
        assertSame(SessionManager.instance(), SessionManager.instance());
    }

    @Test
    public void loginThenGetUserReturnsThatUser() {
        SystemUser user = citizen(1);
        String token = sessions.login(user);
        assertNotNull(token);
        assertSame(user, sessions.getUser(token));
    }

    @Test
    public void logoutEndsTheSession() {
        String token = sessions.login(citizen(2));
        sessions.logout(token);
        assertNull(sessions.getUser(token));
    }

    @Test
    public void unknownOrMissingTokenHasNoUser() {
        assertNull(sessions.getUser("not-a-real-token"));
        assertNull(sessions.getUser(null));
    }

    @Test
    public void logoutTwiceOrWithNoTokenIsHarmless() {
        String token = sessions.login(citizen(3));
        sessions.logout(token);
        sessions.logout(token);
        sessions.logout(null);
        assertNull(sessions.getUser(token));
    }

    @Test
    public void eachLoginGetsItsOwnToken() {
        SystemUser user = citizen(4);
        String first = sessions.login(user);
        String second = sessions.login(user);
        assertNotEquals(first, second);
        sessions.logout(first);
        assertSame(user, sessions.getUser(second));
    }
}
