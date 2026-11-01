package kz.aitu.greenhouse.visitor;

import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.Heater;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;

/** R is the result of this operation: a number, a list, JSON, or Void. */
public interface Visitor<R> {
    R visit(Greenhouse greenhouse);

    R visit(Zone zone);

    R visit(Crop crop);

    R visit(Pump pump);

    R visit(Lamp lamp);

    R visit(Sensor sensor);

    R visit(Heater heater);
}
