package kz.aitu.greenhouse.observer;

import kz.aitu.greenhouse.events.Event;
import kz.aitu.greenhouse.events.EventType;

public interface Subject<E extends Event> {
    void subscribe(EventType type, Observer<E> observer);

    void unsubscribe(EventType type, Observer<E> observer);

    void publish(E event);
}
