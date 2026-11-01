package kz.aitu.greenhouse;

import kz.aitu.greenhouse.events.AverageEvent;
import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.InspectionEvent;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.listeners.MoistureAggregator;
import kz.aitu.greenhouse.observer.Observer;
import kz.aitu.greenhouse.observer.SimpleSubject;
import kz.aitu.greenhouse.observer.SoilSensor;
import kz.aitu.greenhouse.observer.WeatherStation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ObserverTest {
    @Test
    void notifiesOnlySubscribersToThePublishedEventType() {
        SimpleSubject<ReadingEvent> subject = new SimpleSubject<>();
        AtomicInteger moistureCalls = new AtomicInteger();
        AtomicInteger temperatureCalls = new AtomicInteger();
        subject.subscribe(EventType.MOISTURE, event -> moistureCalls.incrementAndGet());
        subject.subscribe(EventType.TEMPERATURE, event -> temperatureCalls.incrementAndGet());

        subject.publish(moisture(25));

        assertEquals(1, moistureCalls.get());
        assertEquals(0, temperatureCalls.get());
    }

    @Test
    void subscribingTheSameListenerTwiceDoesNotDuplicateDelivery() {
        SimpleSubject<ReadingEvent> subject = new SimpleSubject<>();
        AtomicInteger calls = new AtomicInteger();
        Observer<ReadingEvent> listener = event -> calls.incrementAndGet();
        subject.subscribe(EventType.MOISTURE, listener);
        subject.subscribe(EventType.MOISTURE, listener);

        subject.publish(moisture(25));

        assertEquals(1, calls.get());
    }

    @Test
    void unsubscribedListenerDoesNotReceiveLaterEvents() {
        SimpleSubject<ReadingEvent> subject = new SimpleSubject<>();
        AtomicInteger calls = new AtomicInteger();
        Observer<ReadingEvent> listener = event -> calls.incrementAndGet();
        subject.subscribe(EventType.MOISTURE, listener);
        subject.publish(moisture(25));

        subject.unsubscribe(EventType.MOISTURE, listener);
        subject.publish(moisture(30));

        assertEquals(1, calls.get());
    }

    @Test
    void listenerCanUnsubscribeFromInsideItsOwnHandler() {
        SimpleSubject<ReadingEvent> subject = new SimpleSubject<>();
        AtomicInteger calls = new AtomicInteger();
        Observer<ReadingEvent> once = new Observer<>() {
            @Override
            public void onEvent(ReadingEvent event) {
                calls.incrementAndGet();
                subject.unsubscribe(EventType.MOISTURE, this);
            }
        };
        AtomicInteger permanentCalls = new AtomicInteger();
        subject.subscribe(EventType.MOISTURE, once);
        subject.subscribe(EventType.MOISTURE, event -> permanentCalls.incrementAndGet());

        subject.publish(moisture(25));
        subject.publish(moisture(30));

        assertEquals(1, calls.get());
        assertEquals(2, permanentCalls.get());
    }

    @Test
    void listenerAddedInsideAHandlerStartsWithTheNextSnapshot() {
        SimpleSubject<ReadingEvent> subject = new SimpleSubject<>();
        AtomicInteger newListenerCalls = new AtomicInteger();
        Observer<ReadingEvent> newListener = event -> newListenerCalls.incrementAndGet();
        subject.subscribe(EventType.MOISTURE,
                event -> subject.subscribe(EventType.MOISTURE, newListener));

        subject.publish(moisture(25));
        assertEquals(0, newListenerCalls.get());

        subject.publish(moisture(30));
        assertEquals(1, newListenerCalls.get());
    }

    @Test
    void removingAListenerDoesNotCancelAnAlreadyCapturedSnapshot() {
        SimpleSubject<ReadingEvent> subject = new SimpleSubject<>();
        AtomicInteger calls = new AtomicInteger();
        Observer<ReadingEvent> laterListener = event -> calls.incrementAndGet();
        subject.subscribe(EventType.MOISTURE,
                event -> subject.unsubscribe(EventType.MOISTURE, laterListener));
        subject.subscribe(EventType.MOISTURE, laterListener);

        subject.publish(moisture(25));
        assertEquals(1, calls.get());

        subject.publish(moisture(30));
        assertEquals(1, calls.get());
    }

    @Test
    void unsubscribingOneTypeKeepsTheOtherTypeSubscription() {
        SimpleSubject<ReadingEvent> subject = new SimpleSubject<>();
        List<EventType> receivedTypes = new ArrayList<>();
        Observer<ReadingEvent> listener = event -> receivedTypes.add(event.type());
        subject.subscribe(EventType.MOISTURE, listener);
        subject.subscribe(EventType.TEMPERATURE, listener);

        subject.unsubscribe(EventType.MOISTURE, listener);
        subject.publish(moisture(25));
        subject.publish(new ReadingEvent(EventType.TEMPERATURE, "zone-a", 20));

        assertEquals(List.of(EventType.TEMPERATURE), receivedTypes);
    }

    @Test
    void oneListenerCanReceiveEventsFromTwoIndependentSources() {
        SoilSensor soil = new SoilSensor(new SimpleSubject<>());
        WeatherStation weather = new WeatherStation(new SimpleSubject<>());
        List<ReadingEvent> readings = new ArrayList<>();
        Observer<ReadingEvent> listener = readings::add;
        soil.subscribe(EventType.MOISTURE, listener);
        weather.subscribe(EventType.TEMPERATURE, listener);
        weather.subscribe(EventType.HUMIDITY, listener);

        soil.moisture("zone-a", 30);
        weather.temperature("zone-a", 22);
        weather.humidity("zone-a", 55);

        assertEquals(List.of(
                new ReadingEvent(EventType.MOISTURE, "zone-a", 30),
                new ReadingEvent(EventType.TEMPERATURE, "zone-a", 22),
                new ReadingEvent(EventType.HUMIDITY, "zone-a", 55)), readings);
    }

    @Test
    void failingListenerDoesNotPreventDeliveryToRemainingListeners() {
        SimpleSubject<ReadingEvent> subject = new SimpleSubject<>();
        AtomicInteger successfulCalls = new AtomicInteger();
        subject.subscribe(EventType.MOISTURE, event -> {
            throw new IllegalArgumentException("Simulated listener failure");
        });
        subject.subscribe(EventType.MOISTURE, event -> successfulCalls.incrementAndGet());

        assertThrows(RuntimeException.class, () -> subject.publish(moisture(25)));

        assertEquals(1, successfulCalls.get());
    }

    @Test
    void observerChainRepublishesIndependentRunningAveragesPerZone() {
        SoilSensor soil = new SoilSensor(new SimpleSubject<>());
        MoistureAggregator aggregator = new MoistureAggregator(
                List.of("zone-a", "zone-b"), new SimpleSubject<>());
        List<AverageEvent> averages = new ArrayList<>();
        soil.subscribe(EventType.MOISTURE, aggregator);
        aggregator.subscribe(EventType.AVERAGE_MOISTURE, averages::add);

        soil.moisture("zone-a", 10);
        soil.moisture("zone-b", 80);
        soil.moisture("zone-a", 30);

        assertEquals(List.of(
                new AverageEvent("zone-a", 10, 1),
                new AverageEvent("zone-b", 80, 1),
                new AverageEvent("zone-a", 20, 2)), averages);
    }

    @Test
    void inspectionEventCopiesItsListAndCannotBeChangedByConsumers() {
        List<String> ids = new ArrayList<>(List.of("pump-a"));
        InspectionEvent event = new InspectionEvent("zone-a", ids);

        ids.add("lamp-a");

        assertEquals(List.of("pump-a"), event.deviceIds());
        assertThrows(UnsupportedOperationException.class,
                () -> event.deviceIds().add("sensor-a"));
        assertEquals(EventType.INSPECTION_REPORT, event.type());
    }

    @Test
    void readingsRejectValuesThatWouldBreakDomainRulesOrJson() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReadingEvent(EventType.MOISTURE, "zone-a", -1));
        assertThrows(IllegalArgumentException.class,
                () -> new ReadingEvent(EventType.HUMIDITY, "zone-a", 101));
        assertThrows(IllegalArgumentException.class,
                () -> new ReadingEvent(EventType.TEMPERATURE, "zone-a", Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new ReadingEvent(EventType.AVERAGE_MOISTURE, "zone-a", 30));
    }

    private ReadingEvent moisture(double value) {
        return new ReadingEvent(EventType.MOISTURE, "zone-a", value);
    }
}
