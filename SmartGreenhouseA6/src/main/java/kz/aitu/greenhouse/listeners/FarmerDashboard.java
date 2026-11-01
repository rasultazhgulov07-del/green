package kz.aitu.greenhouse.listeners;

import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.observer.Observer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FarmerDashboard implements Observer<ReadingEvent> {
    private final ConcurrentMap<ReadingKey, ReadingEvent> readings = new ConcurrentHashMap<>();

    @Override
    public void onEvent(ReadingEvent event) {
        // A separate key for each measurement avoids overwriting other types in the same zone.
        readings.put(new ReadingKey(event.type(), event.zoneId()), event);
    }

    public ReadingEvent latest(EventType type, String zoneId) {
        return readings.get(new ReadingKey(type, zoneId));
    }

    private record ReadingKey(EventType type, String zoneId) {
    }
}
