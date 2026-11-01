package kz.aitu.greenhouse;

import kz.aitu.greenhouse.app.ProducerRunner;
import kz.aitu.greenhouse.events.EventType;
import kz.aitu.greenhouse.events.ReadingEvent;
import kz.aitu.greenhouse.listeners.ReadingCounter;
import kz.aitu.greenhouse.observer.SimpleSubject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProducerRunnerTest {
    @Test
    @Timeout(15)
    void usesThreeConcurrentProducerThreadsAndFinishesEveryEventBeforeShutdown()
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        CyclicBarrier allProducersStarted = new CyclicBarrier(3);
        SimpleSubject<ReadingEvent> source = new SimpleSubject<>();
        ReadingCounter counter = new ReadingCounter();
        source.subscribe(EventType.MOISTURE, counter);
        Runnable producer = () -> {
            threadNames.add(Thread.currentThread().getName());
            await(allProducersStarted);
            for (int i = 0; i < 100; i++) {
                source.publish(new ReadingEvent(EventType.MOISTURE, "zone-a", 40));
            }
        };

        try (ProducerRunner runner = new ProducerRunner(executor)) {
            runner.run(List.of(producer, producer, producer));
        }

        assertEquals(3, threadNames.size());
        assertEquals(300, counter.count());
        assertTrue(executor.isTerminated());
    }

    @Test
    @Timeout(15)
    void reportsAProducerFailureAndStillFinishesTheOtherProducers()
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        AtomicInteger completed = new AtomicInteger();
        Runnable failed = () -> {
            throw new IllegalStateException("Simulated producer failure");
        };
        Runnable successful = () -> completed.incrementAndGet();

        try (ProducerRunner runner = new ProducerRunner(executor)) {
            assertThrows(IllegalStateException.class,
                    () -> runner.run(List.of(failed, successful, successful)));
            assertEquals(2, completed.get());
        }

        assertTrue(executor.isTerminated());
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting producers", exception);
        } catch (BrokenBarrierException | TimeoutException exception) {
            throw new IllegalStateException("Producers did not start together", exception);
        }
    }
}
