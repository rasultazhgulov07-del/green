package kz.aitu.greenhouse.observer;

import kz.aitu.greenhouse.events.Event;

@FunctionalInterface
public interface Observer<E extends Event> {
    void onEvent(E event);
}
