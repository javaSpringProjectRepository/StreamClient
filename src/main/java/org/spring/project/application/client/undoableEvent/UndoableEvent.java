package org.spring.project.application.client.undoableEvent;

public interface UndoableEvent {

    EventType getMainType();
    EventType getType();
    Object getElement();
}
