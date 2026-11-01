package kz.aitu.greenhouse.model;

import kz.aitu.greenhouse.visitor.Visitor;

/** The fifth leaf type added in the separate new-element commit. */
public record Heater(String id, String name, double watts, boolean maintenanceRequired)
        implements GreenhouseElement {
    public Heater {
        ModelChecks.text(id, "id");
        ModelChecks.text(name, "name");
        ModelChecks.nonNegative(watts, "watts");
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
