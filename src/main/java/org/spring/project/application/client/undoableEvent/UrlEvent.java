package org.spring.project.application.client.undoableEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public class UrlEvent implements UndoableEvent {

    private final EventType mainType;
    private final EventType type = EventType.URL;
    private final String url;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlEvent urlEvent = (UrlEvent) o;
        return mainType == urlEvent.mainType && type == urlEvent.type && Objects.equals(url, urlEvent.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainType, type, url);
    }

    @Override
    public Object getElement() {
        return url;
    }
}


