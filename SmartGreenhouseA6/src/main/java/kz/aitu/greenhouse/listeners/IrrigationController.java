package kz.aitu.greenhouse.listeners;

import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.observer.Observer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class IrrigationController implements Observer<ReadingEvent> {
    private final double moistureThreshold;
    private final ConcurrentMap<String, Boolean> irrigationByZone = new ConcurrentHashMap<>();

    public IrrigationController(double moistureThreshold) {
        if (!Double.isFinite(moistureThreshold) || moistureThreshold < 0 || moistureThreshold > 100) {
            throw new IllegalArgumentException("moistureThreshold must be between 0 and 100");
        }
        this.moistureThreshold = moistureThreshold;
    }

    @Override
    public void onEvent(ReadingEvent event) {
        if (event.type() == EventType.MOISTURE) {
            irrigationByZone.put(event.zoneId(), event.value() < moistureThreshold);
        }
    }

    public boolean isIrrigationOn(String zoneId) {
        return irrigationByZone.getOrDefault(zoneId, false);
    }
}
