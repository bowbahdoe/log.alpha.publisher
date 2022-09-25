package dev.mccue.log.alpha.publisher;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public interface PublisherInternal {
    AtomicReference<LogBuffer> bufferReference();

    default Optional<Duration> publishDelay() {
        return Optional.of(Duration.ofMillis(200));
    }


}
