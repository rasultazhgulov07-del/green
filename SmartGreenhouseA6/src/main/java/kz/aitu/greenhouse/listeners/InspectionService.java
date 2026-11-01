package kz.aitu.greenhouse.listeners;

import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.InspectionEvent;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.model.GreenhouseElement;
import kz.aitu.greenhouse.model.Zone;
import kz.aitu.greenhouse.observer.Observer;
import kz.aitu.greenhouse.observer.Subject;
import kz.aitu.greenhouse.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The integration point: an event starts a visitor on ONE related zone. */
public final class InspectionService implements Observer<ReadingEvent> {
    private final Map<String, Zone> zones;
    private final Visitor<List<GreenhouseElement>> inspector;
    private final Subject<InspectionEvent> reports;
    private final double dryThreshold;

    public InspectionService(Map<String, Zone> zones,
                             Visitor<List<GreenhouseElement>> inspector,
                             Subject<InspectionEvent> reports,
                             double dryThreshold) {
        this.zones = Map.copyOf(zones);
        this.inspector = Objects.requireNonNull(inspector);
        this.reports = Objects.requireNonNull(reports);
        if (!Double.isFinite(dryThreshold) || dryThreshold < 0 || dryThreshold > 100) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100");
        }
        this.dryThreshold = dryThreshold;
    }

    @Override
    public void onEvent(ReadingEvent event) {
        if (event.type() != EventType.MOISTURE || event.value() >= dryThreshold) {
            return;
        }
        Zone affectedZone = zones.get(event.zoneId());
        if (affectedZone == null) {
            throw new IllegalArgumentException("Unknown zone: " + event.zoneId());
        }
        List<GreenhouseElement> devices = affectedZone.accept(inspector);
        List<String> deviceIds = new ArrayList<>();
        for (GreenhouseElement device : devices) {
            deviceIds.add(device.id());
        }
        reports.publish(new InspectionEvent(event.zoneId(), deviceIds));
    }
}
