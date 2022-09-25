package dev.mccue.log.alpha.publisher;

import dev.mccue.log.alpha.Log;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface LogBuffer {
    int size();
    LogBuffer enqueue(Log log);
    LogBuffer dequeue(Offset offset);
    LogBuffer clear();

    interface Future<T> {
        Future<T> onSuccess(Consumer<T> data);

        Future<T> onFailure(Consumer<Throwable> error);
    }

    default <T> T get(Future<T> f) {
        var completableFuture = new CompletableFuture<T>();
        f
                .onSuccess(completableFuture::complete)
                .onFailure(completableFuture::completeExceptionally);

        return completableFuture.join();
    }

    List<OffsetLogPair> items();

    record Offset(long value) {}
    record OffsetLogPair(Offset offset, Log log) {}

    static LogBuffer withCapacity(int capacity) {
       return LogBufferImpl.create(capacity);
    }
}
