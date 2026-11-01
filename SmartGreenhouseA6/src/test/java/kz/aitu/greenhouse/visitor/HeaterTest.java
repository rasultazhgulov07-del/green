package kz.aitu.greenhouse.visitor;

import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.Heater;
import kz.aitu.greenhouse.model.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeaterTest {
    @Test
    void allFiveVisitorsHandleTheNewLeafType() {
        Heater heater = new Heater("heater-a", "Heater", 300, true);
        Zone zone = new Zone("a", "Zone A", List.of(
                new Crop("crop-a", "Tomatoes", 10), heater));
        Greenhouse greenhouse = new Greenhouse("g", "Greenhouse", List.of(zone));

        assertEquals(300.0, greenhouse.accept(new PowerVisitor()));
        assertEquals(10.0, greenhouse.accept(new WaterDemandVisitor()));
        assertEquals(List.of(heater), greenhouse.accept(new MaintenanceVisitor()));

        InventoryVisitor inventory = new InventoryVisitor();
        greenhouse.accept(inventory);
        assertEquals(1, inventory.getCounts().get("Heater"));
        assertEquals(4, inventory.getTotalElements());
        assertEquals(10.0, inventory.getWaterLitresPerDay());
        assertEquals("{\"type\":\"heater\",\"id\":\"heater-a\",\"name\":\"Heater\","
                        + "\"watts\":300.0,\"maintenanceRequired\":true}",
                heater.accept(new JsonExportVisitor()));
    }

    @Test
    void healthyHeaterIsExcludedFromMaintenanceList() {
        Heater heater = new Heater("heater-b", "Healthy heater", 200, false);
        assertTrue(heater.accept(new MaintenanceVisitor()).isEmpty());
    }
}
