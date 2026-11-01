package kz.aitu.greenhouse.visitor;

import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.GreenhouseElement;
import kz.aitu.greenhouse.model.Heater;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;

/** A new operation added without changing any existing element class. */
public final class WaterDemandVisitor implements Visitor<Double> {
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
        return crop.waterLitresPerDay();
    }

    @Override
    public Double visit(Pump pump) {
        return 0.0;
    }

    @Override
    public Double visit(Lamp lamp) {
        return 0.0;
    }

    @Override
    public Double visit(Sensor sensor) {
        return 0.0;
    }

    @Override
    public Double visit(Heater heater) {
        return 0.0;
    }
}
