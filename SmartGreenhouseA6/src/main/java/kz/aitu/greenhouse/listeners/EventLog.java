package kz.aitu.greenhouse.listeners;

import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.observer.Observer;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class EventLog implements Observer<ReadingEvent> {
    private final ConcurrentLinkedQueue<ReadingEvent> events = new ConcurrentLinkedQueue<>();

    @Override
    public void onEvent(ReadingEvent event) {
        events.add(event);
    }

    public List<ReadingEvent> events() {
        return List.copyOf(events);
    }
}
