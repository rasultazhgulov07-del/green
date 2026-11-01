package kz.aitu.greenhouse.model;

import kz.aitu.greenhouse.visitor.Visitor;

/** A common contract for containers and leaves in the greenhouse tree. */
public interface GreenhouseElement {
    String id();

    String name();

    <R> R accept(Visitor<R> visitor);
}
