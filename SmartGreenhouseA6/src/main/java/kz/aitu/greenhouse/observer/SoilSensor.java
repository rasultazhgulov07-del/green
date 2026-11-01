package kz.aitu.greenhouse.observer;

import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.ReadingEvent;

import java.util.Objects;

public final class SoilSensor implements Subject<ReadingEvent> {
    private final Subject<ReadingEvent> events;

    public SoilSensor(Subject<ReadingEvent> events) {
        this.events = Objects.requireNonNull(events);
    }

    public void moisture(String zoneId, double value) {
        publish(new ReadingEvent(EventType.MOISTURE, zoneId, value));
    }

    @Override
    public void subscribe(EventType type, Observer<ReadingEvent> observer) {
        events.subscribe(type, observer);
    }

    @Override
    public void unsubscribe(EventType type, Observer<ReadingEvent> observer) {
        events.unsubscribe(type, observer);
    }

    @Override
    public void publish(ReadingEvent event) {
        if (event.type() != EventType.MOISTURE) {
            throw new IllegalArgumentException("SoilSensor publishes only moisture readings");
        }
        events.publish(event);
    }
}
