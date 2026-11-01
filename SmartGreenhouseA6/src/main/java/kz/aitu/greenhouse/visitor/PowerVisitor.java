package kz.aitu.greenhouse.visitor;

import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.GreenhouseElement;
import kz.aitu.greenhouse.model.Heater;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;

/** Calculates installed electrical power in watts, not energy in watt-hours. */
public final class PowerVisitor implements Visitor<Double> {
    @Override
    public Double visit(Greenhouse greenhouse) {
        double total = 0;
        for (Zone zone : greenhouse.zones()) {
            total += zone.accept(this);
        }
        return total;
    }

    @Override
    public Double visit(Zone zone) {
        double total = 0;
        for (GreenhouseElement child : zone.children()) {
            total += child.accept(this);
        }
        return total;
    }

    @Override
    public Double visit(Crop crop) {
        return 0.0;
    }

    @Override
    public Double visit(Pump pump) {
        return pump.watts();
    }

    @Override
    public Double visit(Lamp lamp) {
        return lamp.watts();
    }

    @Override
    public Double visit(Sensor sensor) {
        return sensor.watts();
    }

    @Override
    public Double visit(Heater heater) {
        return heater.watts();
    }
}
