package kz.aitu.greenhouse.events;

import java.util.List;

public record InspectionEvent(String zoneId, List<String> deviceIds) implements Event {
    public InspectionEvent {
        EventValidation.requireId(zoneId, "zoneId");
        deviceIds = List.copyOf(deviceIds);
        for (String deviceId : deviceIds) {
            EventValidation.requireId(deviceId, "deviceId");
        }
    }

    @Override
    public EventType type() {
        return EventType.INSPECTION_REPORT;
    }
}
