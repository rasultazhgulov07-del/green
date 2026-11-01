package kz.aitu.greenhouse.listeners;

import kz.aitu.greenhouse.events.AverageEvent;
import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.observer.Observer;
import kz.aitu.greenhouse.observer.Subject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MoistureAggregator implements Observer<ReadingEvent>, Subject<AverageEvent> {
    private final Map<String, Accumulator> valuesByZone;
    private final Subject<AverageEvent> output;

    public MoistureAggregator(List<String> zoneIds, Subject<AverageEvent> output) {
        this.output = Objects.requireNonNull(output);
        Map<String, Accumulator> accumulators = new HashMap<>();
        for (String zoneId : zoneIds) {
            if (zoneId == null || zoneId.isBlank()) {
                throw new IllegalArgumentException("zoneId must not be blank");
            }
            if (accumulators.put(zoneId, new Accumulator()) != null) {
                throw new IllegalArgumentException("Duplicate zoneId: " + zoneId);
            }
        }
        valuesByZone = Map.copyOf(accumulators);
    }

    @Override
    public void onEvent(ReadingEvent event) {
        if (event.type() != EventType.MOISTURE) {
            return;
        }
        AverageEvent average = accumulator(event.zoneId()).add(event.zoneId(), event.value());
        // Publish after releasing the zone lock: callbacks never run under this lock.
        publish(average);
    }

    @Override
    public void subscribe(EventType type, Observer<AverageEvent> observer) {
        output.subscribe(type, observer);
    }

    @Override
    public void unsubscribe(EventType type, Observer<AverageEvent> observer) {
        output.unsubscribe(type, observer);
    }

    @Override
    public void publish(AverageEvent event) {
        output.publish(event);
    }

    public int sampleCount(String zoneId) {
        return accumulator(zoneId).count();
    }

    public double average(String zoneId) {
        return accumulator(zoneId).average();
    }

    private Accumulator accumulator(String zoneId) {
        Accumulator accumulator = valuesByZone.get(zoneId);
        if (accumulator == null) {
            throw new IllegalArgumentException("Unknown zoneId: " + zoneId);
        }
        return accumulator;
    }

    private static final class Accumulator {
        private double sum;
        private int count;

        synchronized AverageEvent add(String zoneId, double value) {
            sum += value;
            count++;
            return new AverageEvent(zoneId, sum / count, count);
        }

        synchronized int count() {
            return count;
        }

        synchronized double average() {
            return count == 0 ? 0 : sum / count;
        }
    }
}
