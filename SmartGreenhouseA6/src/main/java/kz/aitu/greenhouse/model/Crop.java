package kz.aitu.greenhouse.model;

import kz.aitu.greenhouse.visitor.Visitor;

public record Crop(String id, String name, double waterLitresPerDay)
        implements GreenhouseElement {
    public Crop {
        ModelChecks.text(id, "id");
        ModelChecks.text(name, "name");
        ModelChecks.nonNegative(waterLitresPerDay, "waterLitresPerDay");
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
