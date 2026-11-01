package kz.aitu.greenhouse.app;

import kz.aitu.greenhouse.events.AverageEvent;
import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.InspectionEvent;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.listeners.*;
import kz.aitu.greenhouse.model.*;
import kz.aitu.greenhouse.observer.Observer;
import kz.aitu.greenhouse.observer.SimpleSubject;
import kz.aitu.greenhouse.observer.SoilSensor;
import kz.aitu.greenhouse.observer.WeatherStation;
import kz.aitu.greenhouse.visitor.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/** The only production composition root: constructs and connects all components. */
public final class Main {
    private Main() { }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = args.length == 0 ? 3 : Integer.parseInt(args[0]);
        if (threadCount < 3) {
            throw new IllegalArgumentException("Use at least 3 producer threads");
        }

        Greenhouse greenhouse = createGreenhouse();
        Map<String, Zone> zones = new LinkedHashMap<>();
        for (Zone zone : greenhouse.zones()) {
            zones.put(zone.id(), zone);
        }
        List<String> zoneIds = List.copyOf(zones.keySet());

        SoilSensor soil = new SoilSensor(new SimpleSubject<ReadingEvent>());
        WeatherStation weather = new WeatherStation(new SimpleSubject<ReadingEvent>());
        SimpleSubject<InspectionEvent> reportSource = new SimpleSubject<>();
        MoistureAggregator aggregator = new MoistureAggregator(
                zoneIds, new SimpleSubject<AverageEvent>());

        FarmerDashboard dashboard = new FarmerDashboard();
        ReadingCounter counter = new ReadingCounter();
        EventLog log = new EventLog();
        IrrigationController irrigation = new IrrigationController(30);
        FrostAlert frostAlert = new FrostAlert(0);
        AverageDashboard averageDashboard = new AverageDashboard();
        ReportCollector reports = new ReportCollector();
        MaintenanceVisitor maintenance = new MaintenanceVisitor();
        InspectionService inspection = new InspectionService(zones, maintenance, reportSource, 30);

        List<Observer<ReadingEvent>> commonListeners = List.of(dashboard, counter, log);
        for (Observer<ReadingEvent> listener : commonListeners) {
            soil.subscribe(EventType.MOISTURE, listener);
            weather.subscribe(EventType.TEMPERATURE, listener);
            weather.subscribe(EventType.HUMIDITY, listener);
        }
        soil.subscribe(EventType.MOISTURE, irrigation);
        soil.subscribe(EventType.MOISTURE, aggregator);
        soil.subscribe(EventType.MOISTURE, inspection);
        weather.subscribe(EventType.TEMPERATURE, frostAlert);
        aggregator.subscribe(EventType.AVERAGE_MOISTURE, averageDashboard);
        reportSource.subscribe(EventType.INSPECTION_REPORT, reports);

        // Three independent producer tasks, with one source shared by two tasks.
        Runnable soilProducer = () -> {
            soil.moisture("zone-a", 20);
            soil.moisture("zone-b", 50);
            soil.moisture("zone-a", 40);
        };
        Runnable temperatureProducer = () -> {
            weather.temperature("zone-a", 24);
            weather.temperature("zone-b", -2);
            weather.temperature("zone-a", 26);
        };
        Runnable humidityProducer = () -> {
            weather.humidity("zone-a", 60);
            weather.humidity("zone-b", 70);
            weather.humidity("zone-a", 65);
        };
        try (ProducerRunner runner = new ProducerRunner(Executors.newFixedThreadPool(threadCount))) {
            runner.run(List.of(soilProducer, temperatureProducer, humidityProducer));
        }

        PowerVisitor energy = new PowerVisitor();
        InventoryVisitor inventory = new InventoryVisitor();
        JsonExportVisitor json = new JsonExportVisitor();
        WaterDemandVisitor water = new WaterDemandVisitor();
        greenhouse.accept(inventory);

        System.out.println("Smart Greenhouse - Assignment 6");
        System.out.println("Producer threads configured: " + threadCount);
        System.out.println("Readings received: " + counter.count());
        System.out.println("Readings logged: " + log.events().size());
        System.out.println("Zone A average moisture: " + averageDashboard.latest("zone-a").average());
        System.out.println("Zone A irrigation on: " + irrigation.isIrrigationOn("zone-a"));
        System.out.println("Frost alerts: " + frostAlert.alerts().size());
        System.out.printf(Locale.ROOT, "Total rated power: %.1f W%n", greenhouse.accept(energy));
        System.out.println("Inventory elements: " + inventory.getTotalElements());
        System.out.println("Inventory counts: " + inventory.getCounts());
        System.out.println("Crop water demand: " + inventory.getWaterLitresPerDay() + " L/day");
        System.out.println("Water demand visitor: " + greenhouse.accept(water) + " L/day");
        System.out.println("Devices needing maintenance: " + greenhouse.accept(maintenance).size());
        for (InspectionEvent report : reports.reports()) {
            System.out.println("Inspection " + report.zoneId() + ": " + report.deviceIds());
        }
        System.out.println("JSON: " + greenhouse.accept(json));
        System.out.println("All producers finished; executor terminated.");
    }

    private static Greenhouse createGreenhouse() {
        Zone zoneA = new Zone("zone-a", "Tomatoes", List.of(
                new Crop("crop-a", "Tomatoes", 10),
                new Pump("pump-a", "Water pump A", 100, true),
                new Lamp("lamp-a", "Grow lamp A", 50, false),
                new Sensor("sensor-a", "Soil probe A", 5, true),
                new Heater("heater-a", "Greenhouse heater A", 300, true)));
        Zone zoneB = new Zone("zone-b", "Cucumbers", List.of(
                new Crop("crop-b", "Cucumbers", 20),
                new Pump("pump-b", "Water pump B", 200, false),
                new Lamp("lamp-b", "Grow lamp B", 75, true),
                new Sensor("sensor-b", "Soil probe B", 5, false)));
        return new Greenhouse("greenhouse-1", "AITU Smart Greenhouse", List.of(zoneA, zoneB));
    }
}
