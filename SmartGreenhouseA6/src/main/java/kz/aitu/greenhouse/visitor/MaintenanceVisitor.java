package kz.aitu.greenhouse.visitor;

import java.util.ArrayList;
import java.util.List;
import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.GreenhouseElement;
import kz.aitu.greenhouse.model.Heater;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;

/** Returns only devices whose data marks them as needing maintenance. */
public final class MaintenanceVisitor implements Visitor<List<GreenhouseElement>> {
    @Override
    public List<GreenhouseElement> visit(Greenhouse greenhouse) {
        List<GreenhouseElement> result = new ArrayList<>();
        for (Zone zone : greenhouse.zones()) {
            result.addAll(zone.accept(this));
        }
        return List.copyOf(result);
    }

    @Override
    public List<GreenhouseElement> visit(Zone zone) {
        List<GreenhouseElement> result = new ArrayList<>();
        for (GreenhouseElement child : zone.children()) {
            result.addAll(child.accept(this));
        }
        return List.copyOf(result);
    }

    @Override
    public List<GreenhouseElement> visit(Crop crop) {
        return List.of();
    }

    @Override
    public List<GreenhouseElement> visit(Pump pump) {
        return pump.maintenanceRequired() ? List.of(pump) : List.of();
    }

    @Override
    public List<GreenhouseElement> visit(Lamp lamp) {
        return lamp.maintenanceRequired() ? List.of(lamp) : List.of();
    }

    @Override
    public List<GreenhouseElement> visit(Sensor sensor) {
        return sensor.maintenanceRequired() ? List.of(sensor) : List.of();
    }

    @Override
    public List<GreenhouseElement> visit(Heater heater) {
        return heater.maintenanceRequired() ? List.of(heater) : List.of();
    }
}
