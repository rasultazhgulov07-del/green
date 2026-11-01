package kz.aitu.greenhouse.events;

public record AverageEvent(String zoneId, double average, int sampleCount) implements Event {
    public AverageEvent {
        EventValidation.requireId(zoneId, "zoneId");
        EventValidation.requirePercentage(average, "average");
        if (sampleCount < 1) {
            throw new IllegalArgumentException("sampleCount must be positive");
        }
    }

    @Override
    public EventType type() {
        return EventType.AVERAGE_MOISTURE;
    }
}
