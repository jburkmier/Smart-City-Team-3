package edu.asu.smartcity.server.observer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Observer pattern: the Subject. Keeps the list of attached observers and tells each one
 * when its state changes. Concrete subjects (Announcement, Request, Task) extend this class
 * and describe their change through getState().
 *
 * Difference from the Phase II design: notify() is named notifyObservers(), because
 * java.lang.Object already has a final notify() method that no class can redefine.
 */
public abstract class Subject {

    private final List<Observer> observers = new ArrayList<>();

    /** Adds an observer. Attaching the same observer twice still notifies it only once. */
    public void attach(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Cannot attach a null observer.");
        }
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void detach(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Calls update() on every attached observer, in the order they were attached.
     * Observers are notified one at a time: if one fails, the ones before it have
     * already been updated and the error is passed to the caller.
     */
    public void notifyObservers() throws SQLException {
        // Copy the list so an observer can detach itself during update().
        for (Observer observer : List.copyOf(observers)) {
            observer.update(this);
        }
    }

    /** What changed, pulled by observers during update(). */
    public abstract Notice getState();
}
