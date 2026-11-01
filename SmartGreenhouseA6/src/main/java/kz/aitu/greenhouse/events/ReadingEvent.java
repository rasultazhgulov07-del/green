package kz.aitu.greenhouse.events;

// A record stores one immutable reading; it has no setters.
public record ReadingEvent(EventType type, String zoneId, double value) implements Event {
    public ReadingEvent {
        EventValidation.requireId(zoneId, "zoneId");
        EventValidation.requireFinite(value, "value");
        if (type != EventType.MOISTURE && type != EventType.TEMPERATURE
                && type != EventType.HUMIDITY) {
            throw new IllegalArgumentException("ReadingEvent requires a sensor event type");
        }
        if (type == EventType.MOISTURE || type == EventType.HUMIDITY) {
            EventValidation.requirePercentage(value, "value");
        }
    }
}
