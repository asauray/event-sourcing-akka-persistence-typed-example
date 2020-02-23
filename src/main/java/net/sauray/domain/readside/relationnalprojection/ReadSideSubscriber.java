package net.sauray.domain.readside.relationnalprojection;
/*
 * ReadSideSubscriber.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

import java.util.List;
import java.util.concurrent.Flow.*;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.persistence.query.EventEnvelope;
import akka.persistence.query.Offset;
import akka.persistence.query.Sequence;
import io.r2dbc.spi.Connection;
import net.sauray.domain.events.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReadSideSubscriber implements Subscriber<List<EventEnvelope>>
{

  private final Connection conn;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Subscription subscription;

  private final int batchSize = 10;

  public ReadSideSubscriber(Connection conn) {
    this.conn = conn;
  }


  @Override
  public void onSubscribe(Subscription s) {
    logger.info("new subscription: {}", s.toString());
    this.subscription = s;
    subscription.request(batchSize);
  }

  @Override
  public void onNext(List<EventEnvelope> t) {
    logger.info("onNext: {}", t.size());
    final var batch = conn.createBatch();
    t.forEach(envelope-> {
      var event = envelope.event(); 
      if(event instanceof MoneyAddedToAccount) {
        var moneyAddedToAccountEvent = (MoneyAddedToAccount)event;
        var statement = String.format("INSERT INTO test_projection(account_id, value) VALUES(%s, %d) ON DUPLICATE KEY UPDATE value=%d;", envelope.persistenceId(), moneyAddedToAccountEvent.getAmountCents(), moneyAddedToAccountEvent.getAmountCents());
        batch.add(statement.toString());
      } else if (event instanceof MoneyRemovedFromAccount) {
        var moneyRemovedFromAccountEvent = (MoneyRemovedFromAccount)event;
        var statement = String.format("INSERT INTO test_projection(account_id, value) VALUES(%s, %d) ON DUPLICATE KEY UPDATE value=%d;", envelope.persistenceId(), moneyRemovedFromAccountEvent.getAmountCents(), moneyRemovedFromAccountEvent.getAmountCents());
        batch.add(statement.toString());
      } else {
        logger.info("not found event: {}", event.toString());
      }
    });

    Mono<Offset> maxOffsetMono = 
      Flux.fromIterable(t)
      .sort((t1,t2) -> ((Sequence)t1.offset()).compareTo((Sequence)t2.offset()))
      .last()
      .map(env -> env.offset());

    maxOffsetMono.flatMap(maxOffset -> 
      Flux.from(conn.beginTransaction())
        .then(Mono.from(batch.execute()))
        .then(Mono.fromRunnable(() -> logger.info("max offset is {}", maxOffset)))
        .then(
          Mono.from(
            conn.createStatement("UPDATE offsets SET offset=? WHERE read_side_id=?")
            .bind(0, ((Sequence)maxOffset).value())
            .bind(1, ReadSideActor.readSideId)
            .execute()
          )
        )
    ).then(Mono.from(conn.commitTransaction()))
     .then(Mono.fromRunnable(() -> subscription.request(batchSize)))
     .subscribe();
  }

  @Override
  public void onError(Throwable t) {
    t.printStackTrace(); 
    logger.error(t.getMessage());
  }

  @Override
  public void onComplete() {
    logger.info("onComplete");
  }

}

