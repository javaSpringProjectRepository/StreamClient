package org.spring.project.application.client.undoableEvent.libraryEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.spring.project.application.client.elements.LibraryListElement;
import org.spring.project.application.client.undoableEvent.EventType;
import org.spring.project.application.client.undoableEvent.UndoableEvent;

import java.util.Objects;

@Getter
@AllArgsConstructor
public class LibraryListEvent implements UndoableEvent {

    private final EventType mainType = EventType.LIBRARY_EVENT;
    private final EventType type = EventType.LIBRARY_LIST;
    private final LibraryListElement libraryListElement;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryListEvent that = (LibraryListEvent) o;
        return mainType == that.mainType && type == that.type && Objects.equals(libraryListElement, that.libraryListElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainType, type, libraryListElement);
    }

    @Override
    public Object getElement() {
        return libraryListElement;
    }
}
