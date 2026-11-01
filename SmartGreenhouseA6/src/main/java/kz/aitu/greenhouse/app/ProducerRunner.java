package kz.aitu.greenhouse.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Owns the supplied executor and always closes it after the demo. */
public final class ProducerRunner implements AutoCloseable {
    private final ExecutorService executor;

    public ProducerRunner(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    public void run(List<Runnable> producers) throws InterruptedException {
        List<Future<?>> tasks = new ArrayList<>();
        for (Runnable producer : producers) {
            tasks.add(executor.submit(producer));
        }
        IllegalStateException failure = null;
        for (Future<?> task : tasks) {
            try {
                task.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException error) {
                if (failure == null) {
                    failure = new IllegalStateException("A producer failed or timed out");
                }
                failure.addSuppressed(error);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public void close() throws InterruptedException {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Producer threads did not stop");
                }
                throw new IllegalStateException("Shutdown required cancellation; run was incomplete");
            }
        } catch (InterruptedException error) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw error;
        }
    }
}
