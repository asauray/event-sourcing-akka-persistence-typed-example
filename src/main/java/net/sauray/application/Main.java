package net.sauray.application;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AskPattern;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.Offset;
import akka.persistence.query.PersistenceQuery;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import com.typesafe.config.Config;
import javacutils.Pair;
import net.sauray.application.Guardian.BankCommandWrapper;
import net.sauray.application.Guardian.CommandWrapper;
import net.sauray.domain.BankAccountEntity;
import net.sauray.domain.OffsetStore;
import net.sauray.domain.commands.*;
import net.sauray.domain.commands.replies.BankCommandReply;
import net.sauray.domain.events.*;

import com.typesafe.config.ConfigFactory;
import net.sauray.domain.readside.relationnalprojection.ReadSideActor;
import net.sauray.domain.readside.relationnalprojection.ReadSideSubscriber;
import net.sauray.infrastructure.Consistency;
import net.sauray.infrastructure.ReactiveSQL;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class Main 
{

    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    static Map<UUID, CountDownLatch> seenEvents = new HashMap<>();

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

        Config conf = ConfigFactory.load();
        String host = conf.getString("slick.db.host");
        int port = conf.getInt("slick.db.port");
        String user = conf.getString("slick.db.user");
        String password = conf.getString("slick.db.password");
        String database = conf.getString("slick.db.database");


        Behavior<CommandWrapper> behavior =  Guardian.create();

        ActorSystem<CommandWrapper> system = ActorSystem.create(behavior, "bank-accounts");

        try {
            OffsetStore offsetStore = OffsetStore.init(user, password, database, host, port);

            final Duration timeout = Duration.ofSeconds(3);
            final JdbcReadJournal readJournal = PersistenceQuery.get(system).getReadJournalFor(JdbcReadJournal.class, JdbcReadJournal.Identifier());
            offsetStore.latestOffset(ReadSideActor.readSideId).whenComplete((startingOffset, error) -> {
                if(error != null) {
                    error.printStackTrace();
                } else {

                try {
                    final var conn = ReactiveSQL.reactiveConnection(user, password, database, host, port).block();
                    readJournal
                        .eventsByTag(BankAccountEntity.TAG, Offset.sequence(startingOffset))
                        .grouped(1)
                        .runWith(Sink.fromSubscriber(new ReadSideSubscriber(conn)), system);

                    Main.start(system, offsetStore);
                } catch (InterruptedException | ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // eventStream.mapAsync(5, envelope -> {
                //             final CompletionStage<Done> f =
                //                     AskPattern.ask(
                //                             system,
                //                             (ActorRef<Done> replyTo) -> new Guardian.UpdateReadSides(envelope.persistenceId(), (BankEvent)envelope.event(), replyTo),
                //                             timeout,
                //                             system.scheduler());
                //             return f.thenApplyAsync(in -> Pair.of(envelope.offset(), (BankEvent)envelope.event()), system.executionContext());
                //         })
                        // .map(pair -> {
                        //     var latch = seenEvents.get(pair.second.id());
                        //     if(latch != null) { latch.notify();}
                        //     return pair;
                        // })
                        // .mapAsync(5, pair -> {
                        //   offsetStore.updateOffset(ReadSideActor.readSideId, pair.first);
                        //   return pair.second;
                        // })
                        
                        //.runWith(Sink.ignore(), system);

                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static CompletionStage<BankCommandReply> waitForReadSide(BankCommandReply reply) {
      if(reply.eventId() != null) {
        CountDownLatch latch = new CountDownLatch(1);
        seenEvents.put(reply.eventId(), latch);
        try {
          if(latch.await(5, TimeUnit.SECONDS)) {
            return CompletableFuture.completedStage(reply);
          } else {
            logger.warn("ReadSide out of sync");
            return CompletableFuture.completedStage(reply);
          }
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.warn("Thread interrupted: ReadSide out of sync");
            return CompletableFuture.completedStage(reply);
        }
      } else {
        return CompletableFuture.completedFuture(reply);
      }
    }

    private static CompletionStage<BankCommandReply> removeMoney(ActorSystem<CommandWrapper> system, OffsetStore offsetStore, String accountId, long value, Consistency consistency) {
        CompletionStage<BankCommandReply> result = AskPattern.ask(
                system,
                replyTo -> new BankCommandWrapper(new RemoveMoneyFromAccount(accountId, value, replyTo)),
                Duration.ofSeconds(10),
                system.scheduler()
        );

        return result.thenCompose(
                (reply) -> {
                    switch(consistency) {
                        case STRONG:
                           waitForReadSide(reply);
                        case EVENTUAL:
                            return CompletableFuture.completedFuture(reply);
                        default:
                            return CompletableFuture.<BankCommandReply>failedFuture(new Exception(""));
                    }
                }
        );

    }

    private static CompletionStage<BankCommandReply> getAccountBalance(ActorSystem<CommandWrapper> system, OffsetStore offsetStore, String accountId) {
        return AskPattern.ask(
                system,
                replyTo -> new BankCommandWrapper(new GetAccountBalance("1", replyTo)),
                Duration.ofSeconds(5),
                system.scheduler()
        );
    }

    private static CompletionStage<BankCommandReply> addMoney(ActorSystem<CommandWrapper> system, OffsetStore offsetStore, String accountId, long value, Consistency consistency) {
        CompletionStage<BankCommandReply> result = AskPattern.ask(
                system,
                replyTo -> new BankCommandWrapper(new AddMoneyOnAccount(accountId, value, replyTo)),
                Duration.ofSeconds(10),
                system.scheduler()
        );

        return result.thenCompose(
                (reply) -> {
                    switch(consistency) {
                        case STRONG:
                            waitForReadSide(reply);
                        case EVENTUAL:
                            return CompletableFuture.completedFuture(reply);
                        default:
                            return CompletableFuture.failedFuture(new Exception("unknown consistency"));
                    }
                }
        );
    }


    private static void start(ActorSystem<CommandWrapper> system, OffsetStore offsetStore) {
        System.out.println(system.path().root());
        System.out.println(system.path().name());
        System.out.println(system.path().toString());

        logger.info("Adding money started");
         for (int i = 0; i < 50; i++) {
          addMoney(system, offsetStore, "2", 10L, Consistency.STRONG);
         }
        addMoney(system, offsetStore, "2", 10L, Consistency.STRONG)
                .thenCompose(reply -> {
                    logger.info("Money added finished");
                    logger.info("Removing money started ");
                    return removeMoney(system, offsetStore, "1", 1L, Consistency.STRONG);
                })
                .thenCompose(reply -> {
                    logger.info("Money removed finished");
                    logger.info("Getting money started");
                    return getAccountBalance(system, offsetStore, "1");
                })
                .thenCompose(reply -> {
                    logger.info("Getting money finished");
                    logger.info("Removing money started ");
                    return removeMoney(system, offsetStore, "1", 2L, Consistency.STRONG);
                })
        .whenComplete((reply, error) -> {
            logger.info("Money removed finished");
            if(error != null) {
                error.printStackTrace();
            } else {
                System.out.println(reply.toString());
            }
        });
    }
}
