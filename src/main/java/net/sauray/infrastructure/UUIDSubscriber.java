package net.sauray.infrastructure;

import java.util.UUID;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public class UUIDSubscriber implements Subscriber<UUID> {


    private UUID value;

    public UUIDSubscriber(UUID value) {
        this.value = value;
    }

    @Override
    public void onSubscribe(Subscription subscription) {

    }

    @Override
    public void onNext(UUID item) {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }

    public UUID getValue() {
        return value;
    }
}
