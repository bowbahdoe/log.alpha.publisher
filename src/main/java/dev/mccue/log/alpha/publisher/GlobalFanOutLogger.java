package dev.mccue.log.alpha.publisher;
import dev.mccue.async.Atom;
import dev.mccue.log.alpha.Flake;
import dev.mccue.log.alpha.Log;
import dev.mccue.log.alpha.Logger;
import dev.mccue.log.alpha.LoggerFactory;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * A Logger which will fan out logs in batches to publishers.
 */
public final class GlobalFanOutLogger implements LoggerFactory {
    private static final Atom<HashMap<Flake, PublisherWiring>> WIRINGS = Atom.of(HashMap.empty());
    private static final Atom<LogBuffer> DEFAULT_BUFFER = Atom.of(LogBuffer.withCapacity(1000));
    private static final ScheduledExecutorService DISPATCH_SCHEDULED_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    private static final int DELAY_MS = 200;

    private static final Lazy<Future<?>> DISPATCH_TASK = Lazy.of(() -> {
        DISPATCH_SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                var buffer = DEFAULT_BUFFER.get();
                var items = buffer.items();

                for (var wiring: WIRINGS.get()) {
                    wiring._2.buffer.swap(publisherBuffer -> {
                        for (var offsetLogPair : items) {
                            publisherBuffer = publisherBuffer.enqueue(offsetLogPair.log());
                        }
                        return publisherBuffer;
                    });
                }

                LogBuffer.Offset lastOffset = items.isEmpty() ? null : items.get(items.size() - 1).offset();
                if (lastOffset != null) {
                    DEFAULT_BUFFER.swap(logBuffer -> logBuffer.dequeue(lastOffset));
                }
            }
            catch (Exception e) {
                // unexpected
            }
        }, DELAY_MS, DELAY_MS, TimeUnit.MILLISECONDS);
        return null;
    });

    public static void registerPublisher(Publisher publisher, int bufferCapacity, Duration publishDelay) {
        DISPATCH_TASK.get();

        var bufferAtom = Atom.of(LogBuffer.withCapacity(bufferCapacity));

        var wakeupThread = Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    Thread.sleep(publishDelay.toMillis());
                    bufferAtom.swap(buffer -> buffer);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        wakeupThread.setDaemon(true);
        wakeupThread.start();
        WIRINGS.swap(map -> map.put(
                Flake.create(),
                new PublisherWiring(
                        bufferAtom,
                        publisher,
                        wakeupThread::interrupt
                )
        ));
    }

    public static void log(Log log) {
        DEFAULT_BUFFER.swap(buffer -> buffer.enqueue(log));
    }

    @Override
    public Logger createLogger() {
        return GlobalFanOutLogger::log;
    }

    private record PublisherWiring(
            Atom<LogBuffer> buffer,
            Publisher publisher,
            Runnable stop
    ) {}
}
