package kz.aitu.greenhouse.events;

public interface Event {
    EventType type();

    String zoneId();
}
