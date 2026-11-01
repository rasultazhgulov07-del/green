package kz.aitu.greenhouse.visitor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.GreenhouseElement;
import kz.aitu.greenhouse.model.Heater;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;

/** Accumulates an inventory. Create one instance for each report. */
public final class InventoryVisitor implements Visitor<Void> {
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private double waterLitresPerDay;

    public InventoryVisitor() {
        counts.put("Greenhouse", 0);
        counts.put("Zone", 0);
        counts.put("Crop", 0);
        counts.put("Pump", 0);
        counts.put("Lamp", 0);
        counts.put("Sensor", 0);
        counts.put("Heater", 0);
    }

    @Override
    public Void visit(Greenhouse greenhouse) {
        count("Greenhouse");
        for (Zone zone : greenhouse.zones()) {
            zone.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Zone zone) {
        count("Zone");
        for (GreenhouseElement child : zone.children()) {
            child.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(Crop crop) {
        count("Crop");
        waterLitresPerDay += crop.waterLitresPerDay();
        return null;
    }

    @Override
    public Void visit(Pump pump) {
        count("Pump");
        return null;
    }

    @Override
    public Void visit(Lamp lamp) {
        count("Lamp");
        return null;
    }

    @Override
    public Void visit(Sensor sensor) {
        count("Sensor");
        return null;
    }

    private void count(String type) {
        counts.put(type, counts.get(type) + 1);
    }

    @Override
    public Void visit(Heater heater) {
        count("Heater");
        return null;
    }

    public Map<String, Integer> getCounts() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(counts));
    }

    public int getTotalElements() {
        int total = 0;
        for (int count : counts.values()) {
            total += count;
        }
        return total;
    }

    public double getWaterLitresPerDay() {
        return waterLitresPerDay;
    }
}
