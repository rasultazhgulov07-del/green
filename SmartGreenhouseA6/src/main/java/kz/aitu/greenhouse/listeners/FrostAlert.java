package kz.aitu.greenhouse.listeners;

import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.observer.Observer;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class FrostAlert implements Observer<ReadingEvent> {
    private final double temperatureThreshold;
    private final ConcurrentLinkedQueue<ReadingEvent> alerts = new ConcurrentLinkedQueue<>();

    public FrostAlert(double temperatureThreshold) {
        if (!Double.isFinite(temperatureThreshold)) {
            throw new IllegalArgumentException("temperatureThreshold must be finite");
        }
        this.temperatureThreshold = temperatureThreshold;
    }

    @Override
    public void onEvent(ReadingEvent event) {
        if (event.type() == EventType.TEMPERATURE && event.value() < temperatureThreshold) {
            alerts.add(event);
        }
    }

    public List<ReadingEvent> alerts() {
        return List.copyOf(alerts);
    }
}
