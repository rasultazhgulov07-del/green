package kz.aitu.greenhouse.model;

import java.util.List;
import kz.aitu.greenhouse.visitor.Visitor;

public record Zone(String id, String name, List<GreenhouseElement> children)
        implements GreenhouseElement {
    public Zone {
        ModelChecks.text(id, "id");
        ModelChecks.text(name, "name");
        children = List.copyOf(children);
        for (GreenhouseElement child : children) {
            if (child instanceof Zone || child instanceof Greenhouse) {
                throw new IllegalArgumentException("A zone can contain leaves only");
            }
        }
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }
}
