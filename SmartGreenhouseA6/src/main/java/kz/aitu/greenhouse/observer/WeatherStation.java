package kz.aitu.greenhouse.observer;

import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.ReadingEvent;

import java.util.Objects;

public final class WeatherStation implements Subject<ReadingEvent> {
    private final Subject<ReadingEvent> events;

    public WeatherStation(Subject<ReadingEvent> events) {
        this.events = Objects.requireNonNull(events);
    }

    public void temperature(String zoneId, double value) {
        publish(new ReadingEvent(EventType.TEMPERATURE, zoneId, value));
    }

    public void humidity(String zoneId, double value) {
        publish(new ReadingEvent(EventType.HUMIDITY, zoneId, value));
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
        if (event.type() != EventType.TEMPERATURE && event.type() != EventType.HUMIDITY) {
            throw new IllegalArgumentException("WeatherStation publishes temperature or humidity");
        }
        events.publish(event);
    }
}
