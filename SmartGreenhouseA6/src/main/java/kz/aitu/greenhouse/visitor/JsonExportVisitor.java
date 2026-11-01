package kz.aitu.greenhouse.visitor;

import java.util.List;
import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.GreenhouseElement;
import kz.aitu.greenhouse.model.Heater;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;

/** Each container embeds its own children, preserving the Composite structure. */
public final class JsonExportVisitor implements Visitor<String> {
    @Override
    public String visit(Greenhouse greenhouse) {
        return container("greenhouse", greenhouse, greenhouse.zones());
    }

    @Override
    public String visit(Zone zone) {
        return container("zone", zone, zone.children());
    }

    @Override
    public String visit(Crop crop) {
        return prefix("crop", crop) + ",\"waterLitresPerDay\":"
                + crop.waterLitresPerDay() + "}";
    }

    @Override
    public String visit(Pump pump) {
        return device("pump", pump, pump.watts(), pump.maintenanceRequired());
    }

    @Override
    public String visit(Lamp lamp) {
        return device("lamp", lamp, lamp.watts(), lamp.maintenanceRequired());
    }

    @Override
    public String visit(Sensor sensor) {
        return device("sensor", sensor, sensor.watts(), sensor.maintenanceRequired());
    }

    private String container(String type, GreenhouseElement element,
                             List<? extends GreenhouseElement> children) {
        StringBuilder json = new StringBuilder(prefix(type, element));
        json.append(",\"children\":[");
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(children.get(i).accept(this));
        }
        return json.append("]}").toString();
    }

    @Override
    public String visit(Heater heater) {
        return device("heater", heater, heater.watts(), heater.maintenanceRequired());
    }

    private String device(String type, GreenhouseElement element,
                          double watts, boolean maintenanceRequired) {
        return prefix(type, element) + ",\"watts\":" + watts
                + ",\"maintenanceRequired\":" + maintenanceRequired + "}";
    }

    private String prefix(String type, GreenhouseElement element) {
        return "{\"type\":" + JsonStrings.quote(type)
                + ",\"id\":" + JsonStrings.quote(element.id())
                + ",\"name\":" + JsonStrings.quote(element.name());
    }
}
