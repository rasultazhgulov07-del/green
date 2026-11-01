package kz.aitu.greenhouse.model;

import java.util.List;
import kz.aitu.greenhouse.visitor.Visitor;

public record Greenhouse(String id, String name, List<Zone> zones)
        implements GreenhouseElement {
    public Greenhouse {
        ModelChecks.text(id, "id");
        ModelChecks.text(name, "name");
        zones = List.copyOf(zones);
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
