package org.spring.project.application.client.undoableEvent.libraryEvent;

import javafx.scene.layout.FlowPane;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.spring.project.application.client.undoableEvent.EventType;
import org.spring.project.application.client.undoableEvent.UndoableEvent;

import java.util.Objects;

@Getter
@AllArgsConstructor
public class LibraryMainPageEvent implements UndoableEvent {

    private final EventType mainType = EventType.LIBRARY_EVENT;
    private final EventType type = EventType.LIBRARY_MAIN_PAGE;
    private final FlowPane mainPage;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryMainPageEvent that = (LibraryMainPageEvent) o;
        return mainType == that.mainType && type == that.type && Objects.equals(mainPage, that.mainPage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainType, type, mainPage);
    }

    @Override
    public Object getElement() {
        return mainPage;
    }
}
