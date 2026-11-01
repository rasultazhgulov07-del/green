package kz.aitu.greenhouse.visitor;

import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaterDemandVisitorTest {
    @Test
    void totalsOnlyCropDemandAcrossBothZones() {
        Zone a = new Zone("a", "Tomatoes", List.of(
                new Crop("c1", "Tomatoes", 10),
                new Pump("p1", "Pump", 100, false),
                new Lamp("l1", "Lamp", 50, false),
                new Sensor("s1", "Sensor", 5, false)));
        Zone b = new Zone("b", "Cucumbers", List.of(new Crop("c2", "Cucumbers", 20)));
        Greenhouse greenhouse = new Greenhouse("g", "Greenhouse", List.of(a, b));

        assertEquals(30.0, greenhouse.accept(new WaterDemandVisitor()));
        assertEquals(10.0, a.accept(new WaterDemandVisitor()));
    }

    @Test
    void emptyZoneNeedsNoWater() {
        Zone empty = new Zone("empty", "Empty zone", List.of());
        assertEquals(0.0, empty.accept(new WaterDemandVisitor()));
    }
}
