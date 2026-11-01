package kz.aitu.greenhouse.observer;

import kz.aitu.greenhouse.events.Event;
import kz.aitu.greenhouse.events.EventType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SimpleSubject<E extends Event> implements Subject<E> {
    private final Map<EventType, CopyOnWriteArrayList<Observer<E>>> listeners;

    public SimpleSubject() {
        Map<EventType, CopyOnWriteArrayList<Observer<E>>> registry =
                new EnumMap<>(EventType.class);
        for (EventType type : EventType.values()) {
            registry.put(type, new CopyOnWriteArrayList<>());
        }
        listeners = Map.copyOf(registry);
    }

    @Override
    public void subscribe(EventType type, Observer<E> observer) {
        listeners.get(Objects.requireNonNull(type)).addIfAbsent(Objects.requireNonNull(observer));
    }

    @Override
    public void unsubscribe(EventType type, Observer<E> observer) {
        listeners.get(Objects.requireNonNull(type)).remove(Objects.requireNonNull(observer));
    }

    @Override
    public void publish(E event) {
        Objects.requireNonNull(event);
        RuntimeException failure = null;
        // The iterator sees a snapshot, so callbacks may subscribe or unsubscribe safely.
        for (Observer<E> observer : listeners.get(event.type())) {
            try {
                observer.onEvent(event);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = new IllegalStateException("One or more observers failed");
                }
                failure.addSuppressed(exception);
            }
        }
        // Do not hide failures, but give every other listener its event first.
        if (failure != null) {
            throw failure;
        }
    }
}
