package dev.mccue.log.alpha.publisher;

public interface Publisher {

    /**
     * Publishes all the logs in the batch that the publisher is able to, returning
     * the state of the buffer after publishing.
     */
    LogBuffer publish(LogBuffer logs);
}
