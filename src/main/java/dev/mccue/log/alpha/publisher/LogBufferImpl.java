package dev.mccue.log.alpha.publisher;

import dev.mccue.log.alpha.Log;
import io.vavr.collection.Vector;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.*;
import java.util.stream.*;

final class LogBufferImpl implements LogBuffer {
    private final long counter;
    private final long start;
    private final long length;
    private final Vector<OffsetLogPair> buffer;

    private LogBufferImpl(long counter, long start, long length, Vector<OffsetLogPair> buffer) {
        this.counter = counter;
        this.start = start;
        this.length = length;
        this.buffer = buffer;
    }

    static LogBufferImpl create(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        return new LogBufferImpl(0, 0, 0, Vector.of(new OffsetLogPair[capacity]));
    }

    OffsetLogPair peek() {
        return buffer.get((int) (start % buffer.length()));
    }

    LogBufferImpl pop() {
        if (length == 0) {
            throw new IllegalStateException("Can't pop empty queue");
        }
        else {
            return new LogBufferImpl(
                    counter,
                    start + 1 % buffer.length(),
                    length - 1,
                    buffer.update((int) start, (OffsetLogPair) null)
            );
        }
    }

    LogBufferImpl popWhile(Predicate<OffsetLogPair> predicate) {
        LogBufferImpl self = this;
        while (true) {
            var peeked = peek();
            if (peeked != null && predicate.test(peeked)) {
                self = pop();
            }
            else {
                break;
            }
        }
        return self;
    }

    @Override
    public int size() {
        return (int) this.length;
    }

    @Override
    public LogBuffer enqueue(Log log) {
        Objects.requireNonNull(log, "log must not be null");
        var newOffset = counter + 1;
        var pair = new OffsetLogPair(new Offset(newOffset), log);
        if (length == buffer.length()) {
            return new LogBufferImpl(
                    newOffset,
                    (start + 1) % length,
                    length,
                    buffer.update(
                            (int) start,
                            pair
                    )
            );
        }
        else {
            return new LogBufferImpl(
                    newOffset,
                    start,
                    length + 1,
                    buffer.update(
                            (int) ((start + length) % buffer.length()),
                            pair
                    )
            );
        }
    }

    @Override
    public LogBuffer dequeue(Offset offset) {
        return popWhile(offsetLogPair -> Long.compareUnsigned(offsetLogPair.offset().value(), Math.max(offset.value(), 0)) <= 0);
    }

    @Override
    public LogBuffer clear() {
        return new LogBufferImpl(
                counter,
                0,
                0,
                Vector.of(new OffsetLogPair[buffer.length()])
        );
    }

    @Override
    public List<OffsetLogPair> items() {
        return buffer.asJava();
    }

    @Override
    public String toString() {
        return "LogBufferImpl[" +
                "counter=" + counter +
                ", start=" + start +
                ", length=" + length +
                ", buffer=" + buffer +
                ']';
    }
}
