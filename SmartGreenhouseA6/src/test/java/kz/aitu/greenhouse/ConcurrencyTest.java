package kz.aitu.greenhouse;

import kz.aitu.greenhouse.events.AverageEvent;
import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.listeners.EventLog;
import kz.aitu.greenhouse.listeners.FarmerDashboard;
import kz.aitu.greenhouse.listeners.IrrigationController;
import kz.aitu.greenhouse.listeners.MoistureAggregator;
import kz.aitu.greenhouse.listeners.ReadingCounter;
import kz.aitu.greenhouse.observer.Observer;
import kz.aitu.greenhouse.observer.SimpleSubject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {
    @Test
    @Timeout(20)
    void fourProducersDeliverExactlyOneThousandEventsEachWithoutLosingState()
            throws Exception {
        int threadCount = 4;
        int eventsPerThread = 1_000;
        int expectedEvents = threadCount * eventsPerThread;
        SimpleSubject<ReadingEvent> subject = new SimpleSubject<>();
        ReadingCounter firstCounter = new ReadingCounter();
        AtomicInteger secondCount = new AtomicInteger();
        EventLog log = new EventLog();
        FarmerDashboard dashboard = new FarmerDashboard();
        IrrigationController irrigation = new IrrigationController(30);
        subject.subscribe(EventType.MOISTURE, firstCounter);
        subject.subscribe(EventType.MOISTURE, event -> secondCount.incrementAndGet());
        subject.subscribe(EventType.MOISTURE, log);
        subject.subscribe(EventType.MOISTURE, dashboard);
        subject.subscribe(EventType.MOISTURE, irrigation);

        MoistureAggregator aggregator = new MoistureAggregator(
                List.of("zone-a"), new SimpleSubject<>());
        ConcurrentLinkedQueue<AverageEvent> averages = new ConcurrentLinkedQueue<>();
        aggregator.subscribe(EventType.AVERAGE_MOISTURE, averages::add);
        subject.subscribe(EventType.MOISTURE, aggregator);

        CyclicBarrier startTogether = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> producers = new ArrayList<>();
        try {
            for (int producer = 0; producer < threadCount; producer++) {
                producers.add(executor.submit(() -> {
                    // Each producer changes only its own temporary subscription.
                    AtomicInteger temporaryCount = new AtomicInteger();
                    Observer<ReadingEvent> temporary = event -> temporaryCount.incrementAndGet();
                    startTogether.await(5, TimeUnit.SECONDS);
                    for (int i = 0; i < eventsPerThread; i++) {
                        if (i % 2 == 0) {
                            subject.subscribe(EventType.MOISTURE, temporary);
                        } else {
                            subject.unsubscribe(EventType.MOISTURE, temporary);
                        }
                        subject.publish(new ReadingEvent(EventType.MOISTURE, "zone-a", 40));
                    }
                    subject.unsubscribe(EventType.MOISTURE, temporary);
                    return null;
                }));
            }
            // Graceful shutdown finishes every submitted task; it does not interrupt them.
            executor.shutdown();
            for (Future<?> producer : producers) {
                producer.get(10, TimeUnit.SECONDS);
            }
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(expectedEvents, firstCounter.count());
            assertEquals(expectedEvents, secondCount.get());
            assertEquals(expectedEvents, log.events().size());
            assertEquals(40.0, dashboard.latest(EventType.MOISTURE, "zone-a").value());
            assertFalse(irrigation.isIrrigationOn("zone-a"));
            assertEquals(expectedEvents, averages.size());
            assertEquals(expectedEvents, aggregator.sampleCount("zone-a"));
            assertEquals(40.0, aggregator.average("zone-a"), 0.000001);

            // Delivery order across threads is unspecified. Every snapshot number
            // must nevertheless occur exactly once, and all averages must equal 40.
            boolean[] seen = new boolean[expectedEvents + 1];
            for (AverageEvent average : averages) {
                int count = average.sampleCount();
                assertTrue(count >= 1 && count <= expectedEvents);
                assertFalse(seen[count], "Duplicate aggregate sample count " + count);
                seen[count] = true;
                assertEquals(40.0, average.average(), 0.000001);
            }
            for (int count = 1; count <= expectedEvents; count++) {
                assertTrue(seen[count], "Missing aggregate sample count " + count);
            }
        } finally {
            if (!executor.isTerminated()) {
                // Emergency cleanup is used only if the test failed or timed out.
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            }
        }
    }
}
