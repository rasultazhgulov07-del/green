package kz.aitu.greenhouse.listeners;

import kz.aitu.greenhouse.events.InspectionEvent;
import kz.aitu.greenhouse.observer.Observer;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ReportCollector implements Observer<InspectionEvent> {
    private final ConcurrentLinkedQueue<InspectionEvent> reports = new ConcurrentLinkedQueue<>();

    @Override
    public void onEvent(InspectionEvent event) {
        reports.add(event);
    }

    public List<InspectionEvent> reports() {
        return List.copyOf(reports);
    }
}
