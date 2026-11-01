package kz.aitu.greenhouse.model;

import kz.aitu.greenhouse.visitor.Visitor;

public record Lamp(String id, String name, double watts, boolean maintenanceRequired)
        implements GreenhouseElement {
    public Lamp {
        ModelChecks.text(id, "id");
        ModelChecks.text(name, "name");
        ModelChecks.nonNegative(watts, "watts");
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
