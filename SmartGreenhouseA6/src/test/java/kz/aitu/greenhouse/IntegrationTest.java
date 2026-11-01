package kz.aitu.greenhouse;

import kz.aitu.greenhouse.events.AverageEvent;
import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.InspectionEvent;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.listeners.AverageDashboard;
import kz.aitu.greenhouse.listeners.InspectionService;
import kz.aitu.greenhouse.listeners.ReportCollector;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;
import kz.aitu.greenhouse.observer.SimpleSubject;
import kz.aitu.greenhouse.observer.SoilSensor;
import kz.aitu.greenhouse.visitor.MaintenanceVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    @Test
    void drySoilEventInspectsOnlyTheAffectedZoneAndPublishesItsReport() {
        ReportCollector reports = new ReportCollector();
        InspectionService inspection = createInspection(reports);
        SoilSensor soil = new SoilSensor(new SimpleSubject<>());
        soil.subscribe(EventType.MOISTURE, inspection);

        soil.moisture("zone-a", 20);

        // lamp-b also needs maintenance, but belongs to the unrelated zone.
        assertEquals(List.of(new InspectionEvent("zone-a", List.of("pump-a", "sensor-a"))),
                reports.reports());
    }

    @Test
    void thresholdBoundaryAndUnrelatedEventTypesDoNotTriggerInspection() {
        ReportCollector reports = new ReportCollector();
        InspectionService inspection = createInspection(reports);

        inspection.onEvent(new ReadingEvent(EventType.MOISTURE, "zone-a", 30));
        inspection.onEvent(new ReadingEvent(EventType.MOISTURE, "zone-a", 40));
        inspection.onEvent(new ReadingEvent(EventType.TEMPERATURE, "zone-a", 20));

        assertTrue(reports.reports().isEmpty());
    }

    @Test
    void unknownDryZoneFailsExplicitlyWithoutInspectingAnotherZone() {
        ReportCollector reports = new ReportCollector();
        InspectionService inspection = createInspection(reports);

        assertThrows(IllegalArgumentException.class,
                () -> inspection.onEvent(new ReadingEvent(EventType.MOISTURE, "unknown", 20)));

        assertTrue(reports.reports().isEmpty());
    }

    @Test
    void olderAggregateDeliveredLateCannotOverwriteTheNewerDashboardValue() {
        AverageDashboard dashboard = new AverageDashboard();
        AverageEvent newer = new AverageEvent("zone-a", 40, 2);

        dashboard.onEvent(newer);
        dashboard.onEvent(new AverageEvent("zone-a", 20, 1));
        dashboard.onEvent(new AverageEvent("zone-b", 80, 1));

        assertEquals(newer, dashboard.latest("zone-a"));
        assertEquals(80.0, dashboard.latest("zone-b").average());
    }

    private InspectionService createInspection(ReportCollector collector) {
        Zone zoneA = new Zone("zone-a", "Tomatoes", List.of(
                new Pump("pump-a", "Water pump A", 100, true),
                new Lamp("lamp-a", "Grow lamp A", 50, false),
                new Sensor("sensor-a", "Soil probe A", 5, true)));
        Zone zoneB = new Zone("zone-b", "Cucumbers", List.of(
                new Lamp("lamp-b", "Grow lamp B", 75, true)));
        SimpleSubject<InspectionEvent> output = new SimpleSubject<>();
        output.subscribe(EventType.INSPECTION_REPORT, collector);
        return new InspectionService(Map.of("zone-a", zoneA, "zone-b", zoneB),
                new MaintenanceVisitor(), output, 30);
    }
}
