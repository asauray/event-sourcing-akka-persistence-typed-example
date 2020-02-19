package net.sauray.infrastructure;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

public class UUIDPublisher implements Publisher<UUID> {

    Map<UUID, LinkedList<Subscriber<UUID>>> subscribers = new ConcurrentHashMap<>();

    @Override
    public void subscribe(Subscriber<? super UUID> subscriber) {
        var uuidSubscriber = (UUIDSubscriber)subscriber;
        subscribers.merge(uuidSubscriber.getValue(), Arrays.asList(subscriber), (prev, test) -> )
        subscribers.getOrDefault(uuidSubscriber.getValue(), new LinkedList<>())
    }
}
