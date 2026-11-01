package kz.aitu.greenhouse.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.GreenhouseElement;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitorTest {
    private Greenhouse sampleTree() {
        Zone first = new Zone("zone-a", "Tomatoes", List.of(
                new Crop("crop-a", "Tomato", 10),
                new Pump("pump-a", "Pump A", 100, true),
                new Lamp("lamp-a", "Lamp A", 50, false),
                new Sensor("sensor-a", "Sensor A", 5, true)));
        Zone second = new Zone("zone-b", "Cucumbers", List.of(
                new Crop("crop-b", "Cucumber", 20),
                new Pump("pump-b", "Pump B", 200, false),
                new Lamp("lamp-b", "Lamp B", 75, true),
                new Sensor("sensor-b", "Sensor B", 5, false)));
        return new Greenhouse("greenhouse", "AITU greenhouse", List.of(first, second));
    }

    @Test
    void calculatesPowerAgainstHandCalculatedTotal() {
        // (100 + 50 + 5) + (200 + 75 + 5) = 435 watts.
        assertEquals(435.0, sampleTree().accept(new PowerVisitor()), 0.000001);
    }

    @Test
    void calculatesOnlyTheSelectedSubtree() {
        Zone firstZone = sampleTree().zones().get(0);
        assertEquals(155.0, firstZone.accept(new PowerVisitor()), 0.000001);
    }

    @Test
    void doubleDispatchChoosesTheConcreteLeafOverload() {
        List<GreenhouseElement> elements = List.of(
                new Crop("c", "Crop", 10),
                new Pump("p", "Pump", 100, false),
                new Lamp("l", "Lamp", 50, false),
                new Sensor("s", "Sensor", 5, false));
        double[] expected = {0, 100, 50, 5};

        for (int i = 0; i < elements.size(); i++) {
            // Static type is GreenhouseElement; accept still selects the correct visit.
            assertEquals(expected[i], elements.get(i).accept(new PowerVisitor()), 0.000001);
        }
    }

    @Test
    void inventoryAccumulatesCountsAndWaterAcrossTheWholeTree() {
        InventoryVisitor visitor = new InventoryVisitor();
        sampleTree().accept(visitor);

        assertEquals(11, visitor.getTotalElements());
        assertEquals(Map.of("Greenhouse", 1, "Zone", 2, "Crop", 2,
                "Pump", 2, "Lamp", 2, "Sensor", 2, "Heater", 0), visitor.getCounts());
        assertEquals(30.0, visitor.getWaterLitresPerDay(), 0.000001);
    }

    @Test
    void separateInventoryReportsHaveIndependentState() {
        InventoryVisitor wholeTree = new InventoryVisitor();
        InventoryVisitor oneZone = new InventoryVisitor();
        Greenhouse greenhouse = sampleTree();
        greenhouse.accept(wholeTree);
        greenhouse.zones().get(0).accept(oneZone);

        assertEquals(11, wholeTree.getTotalElements());
        assertEquals(5, oneZone.getTotalElements());
        assertEquals(10.0, oneZone.getWaterLitresPerDay(), 0.000001);
    }

    @Test
    void inventoryReturnsAnImmutableSnapshot() {
        InventoryVisitor visitor = new InventoryVisitor();
        sampleTree().accept(visitor);
        Map<String, Integer> snapshot = visitor.getCounts();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("Pump", 99));
        new Pump("extra", "Extra pump", 20, false).accept(visitor);
        assertEquals(2, snapshot.get("Pump"));
        assertEquals(3, visitor.getCounts().get("Pump"));
    }

    @Test
    void maintenanceReturnsOnlyTheThreeFlaggedDevices() {
        List<GreenhouseElement> result = sampleTree().accept(new MaintenanceVisitor());
        List<String> ids = new ArrayList<>();
        for (GreenhouseElement element : result) {
            ids.add(element.id());
        }

        assertEquals(List.of("pump-a", "sensor-a", "lamp-b"), ids);
    }

    @Test
    void maintenanceDoesNotVisitAnotherZone() {
        List<GreenhouseElement> result = sampleTree().zones().get(0)
                .accept(new MaintenanceVisitor());

        assertEquals(2, result.size());
        assertTrue(result.contains(new Pump("pump-a", "Pump A", 100, true)));
        assertTrue(result.contains(new Sensor("sensor-a", "Sensor A", 5, true)));
    }

    @Test
    void emptyTreeHasZeroPowerAndNoMaintenanceDevices() {
        Greenhouse empty = new Greenhouse("empty", "Empty greenhouse", List.of());

        assertEquals(0.0, empty.accept(new PowerVisitor()), 0.000001);
        assertTrue(empty.accept(new MaintenanceVisitor()).isEmpty());
        InventoryVisitor inventory = new InventoryVisitor();
        empty.accept(inventory);
        assertEquals(1, inventory.getTotalElements());
    }

    @Test
    void containersDefensivelyCopyTheirChildren() {
        List<GreenhouseElement> children = new ArrayList<>();
        children.add(new Crop("crop", "Crop", 10));
        Zone zone = new Zone("zone", "Zone", children);
        children.clear();

        List<Zone> zones = new ArrayList<>();
        zones.add(zone);
        Greenhouse greenhouse = new Greenhouse("g", "Greenhouse", zones);
        zones.clear();

        assertEquals(1, zone.children().size());
        assertEquals(1, greenhouse.zones().size());
        assertThrows(UnsupportedOperationException.class, () -> zone.children().clear());
        assertThrows(UnsupportedOperationException.class, () -> greenhouse.zones().clear());
    }

    @Test
    void rejectsNonFiniteNegativeOrUnnamedData() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pump("p", "Pump", Double.NaN, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Lamp("l", "Lamp", Double.POSITIVE_INFINITY, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Sensor("s", "Sensor", -1, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Crop("c", "Crop", -1));
        assertThrows(IllegalArgumentException.class,
                () -> new Crop(" ", "Crop", 10));
    }

    @Test
    void zoneRejectsContainersToPreserveTheThreeLevelStructure() {
        Zone nestedZone = new Zone("inner", "Inner zone", List.of());
        Greenhouse nestedGreenhouse = new Greenhouse("g", "Greenhouse", List.of());

        assertThrows(IllegalArgumentException.class,
                () -> new Zone("outer", "Outer zone", List.of(nestedZone)));
        assertThrows(IllegalArgumentException.class,
                () -> new Zone("outer", "Outer zone", List.of(nestedGreenhouse)));
    }
}
