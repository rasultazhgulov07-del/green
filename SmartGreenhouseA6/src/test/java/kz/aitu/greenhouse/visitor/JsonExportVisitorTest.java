package kz.aitu.greenhouse.visitor;

import java.util.List;
import kz.aitu.greenhouse.model.Crop;
import kz.aitu.greenhouse.model.Greenhouse;
import kz.aitu.greenhouse.model.Lamp;
import kz.aitu.greenhouse.model.Pump;
import kz.aitu.greenhouse.model.Sensor;
import kz.aitu.greenhouse.model.Zone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonExportVisitorTest {
    @Test
    void exportsAllThreeLevelsAndAllFourLeafTypesWithCorrectNesting() {
        Zone zone = new Zone("z", "Zone", List.of(
                new Crop("c", "Crop", 10),
                new Pump("p", "Pump", 100, true),
                new Lamp("l", "Lamp", 50, false),
                new Sensor("s", "Sensor", 5, true)));
        Greenhouse greenhouse = new Greenhouse("g", "Greenhouse", List.of(zone));
        String expected = "{\"type\":\"greenhouse\",\"id\":\"g\",\"name\":\"Greenhouse\",\"children\":["
                + "{\"type\":\"zone\",\"id\":\"z\",\"name\":\"Zone\",\"children\":["
                + "{\"type\":\"crop\",\"id\":\"c\",\"name\":\"Crop\",\"waterLitresPerDay\":10.0},"
                + "{\"type\":\"pump\",\"id\":\"p\",\"name\":\"Pump\",\"watts\":100.0,\"maintenanceRequired\":true},"
                + "{\"type\":\"lamp\",\"id\":\"l\",\"name\":\"Lamp\",\"watts\":50.0,\"maintenanceRequired\":false},"
                + "{\"type\":\"sensor\",\"id\":\"s\",\"name\":\"Sensor\",\"watts\":5.0,\"maintenanceRequired\":true}"
                + "]}]}";

        assertEquals(expected, greenhouse.accept(new JsonExportVisitor()));
    }

    @Test
    void escapesQuotesSlashesAndControlCharactersInData() {
        String specialName = "A\"B\\C/D\nE\tF\rG\bH\fI" + (char) 1 + "Z";
        Crop crop = new Crop("c\"", specialName, 1);
        String expected = "{\"type\":\"crop\",\"id\":\"c\\\"\",\"name\":\""
                + "A\\\"B\\\\C\\/D\\nE\\tF\\rG\\bH\\fI\\u0001Z"
                + "\",\"waterLitresPerDay\":1.0}";

        assertEquals(expected, crop.accept(new JsonExportVisitor()));
    }

    @Test
    void emptyContainersExportEmptyArraysWithoutTrailingCommas() {
        Greenhouse greenhouse = new Greenhouse("g", "Greenhouse", List.of());
        Zone zone = new Zone("z", "Zone", List.of());

        assertEquals("{\"type\":\"greenhouse\",\"id\":\"g\",\"name\":\"Greenhouse\",\"children\":[]}",
                greenhouse.accept(new JsonExportVisitor()));
        assertEquals("{\"type\":\"zone\",\"id\":\"z\",\"name\":\"Zone\",\"children\":[]}",
                zone.accept(new JsonExportVisitor()));
    }

    @Test
    void exportsUnicodeAndMultipleSiblingZones() {
        Greenhouse greenhouse = new Greenhouse("g", "Теплица", List.of(
                new Zone("a", "A", List.of()), new Zone("b", "B", List.of())));
        String expected = "{\"type\":\"greenhouse\",\"id\":\"g\",\"name\":\"Теплица\",\"children\":["
                + "{\"type\":\"zone\",\"id\":\"a\",\"name\":\"A\",\"children\":[]},"
                + "{\"type\":\"zone\",\"id\":\"b\",\"name\":\"B\",\"children\":[]}]}";

        assertEquals(expected, greenhouse.accept(new JsonExportVisitor()));
    }

    @Test
    void escapesSurrogateCodeUnitsWithoutLosingText() {
        // A seedling emoji is a UTF-16 pair. Escaping also preserves a lone code unit.
        Crop crop = new Crop("c", "\uD83C\uDF31" + (char) 0xD800, 1);
        String expected = "{\"type\":\"crop\",\"id\":\"c\",\"name\":\"\\ud83c\\udf31\\ud800\","
                + "\"waterLitresPerDay\":1.0}";

        assertEquals(expected, crop.accept(new JsonExportVisitor()));
    }
}
