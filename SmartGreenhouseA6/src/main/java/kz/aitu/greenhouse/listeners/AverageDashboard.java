package kz.aitu.greenhouse.listeners;

import kz.aitu.greenhouse.events.AverageEvent;
import kz.aitu.greenhouse.observer.Observer;

import java.util.concurrent.ConcurrentHashMap;

/** A later-delivered old snapshot must not replace a newer running average. */
public final class AverageDashboard implements Observer<AverageEvent> {
    private final ConcurrentHashMap<String, AverageEvent> latest = new ConcurrentHashMap<>();

    @Override
    public void onEvent(AverageEvent event) {
        latest.merge(event.zoneId(), event, (oldEvent, newEvent) ->
                newEvent.sampleCount() > oldEvent.sampleCount() ? newEvent : oldEvent);
    }

    public AverageEvent latest(String zoneId) {
        return latest.get(zoneId);
    }
}
