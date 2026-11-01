package kz.aitu.greenhouse.listeners;

import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.observer.Observer;

import java.util.concurrent.atomic.AtomicInteger;

public final class ReadingCounter implements Observer<ReadingEvent> {
    private final AtomicInteger count = new AtomicInteger();

    @Override
    public void onEvent(ReadingEvent event) {
        count.incrementAndGet();
    }

    public int count() {
        return count.get();
    }
}
