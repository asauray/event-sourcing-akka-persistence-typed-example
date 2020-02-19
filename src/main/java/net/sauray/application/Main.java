package net.sauray.application;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AskPattern;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal;
import akka.persistence.query.Offset;
import akka.persistence.query.PersistenceQuery;
import akka.stream.javadsl.Sink;
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
import net.sauray.infrastructure.Consistency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class Main 
{

    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    static Set<UUID> seenEvents = new HashSet<>();

    public static void main( String[] args )
    {
        new java.util.concurrent.Flow()

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

                readJournal
                        .eventsByTag(BankAccountEntity.TAG, Offset.sequence(startingOffset))
                        .mapAsync(5, envelope -> {
                            final CompletionStage<Done> f =
                                    AskPattern.ask(
                                            system,
                                            (ActorRef<Done> replyTo) -> new Guardian.UpdateReadSides(envelope.persistenceId(), (BankEvent)envelope.event(), replyTo),
                                            timeout,
                                            system.scheduler());
                            return f.thenApplyAsync(in -> Pair.of(envelope.offset(), (BankEvent)envelope.event()), system.executionContext());
                        })
                        .map(pair -> {
                            seenEvents.add(pair.second.id());
                            return pair;
                        })
                        .mapAsync(1, pair -> offsetStore.updateOffset(ReadSideActor.readSideId, pair.first))
                        .runWith(Sink.ignore(), system);

                    Main.start(system, offsetStore);
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static CompletableFuture<Void> letMeKnow(UUID eventId) {
        return CompletableFuture.runAsync(() -> {
            while(!seenEvents.contains(eventId)) {
                for (UUID seenEvent : seenEvents) {
                    logger.info(seenEvent.toString());
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logger.info("READ SIDE HAS NOW SEEN EVENT " + eventId.toString());
        }).orTimeout(15, TimeUnit.SECONDS);
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
                            if(reply.eventId() != null) {
                                return letMeKnow(reply.eventId()).thenApply((t) -> reply);
                            } else {
                                return CompletableFuture.completedFuture(reply);
                            }
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
                            return letMeKnow(reply.eventId()).thenApply((t) -> reply);
                        case EVENTUAL:
                            return CompletableFuture.completedFuture(reply);
                        default:
                            return CompletableFuture.failedFuture(new Exception("unknown consistency"));
                    }
                }
        );

    }
/*
        if(failure != null) {
                        var test = CompletableFuture.<BankCommandReply>failedFuture(new Exception(""));
                        return test;
                    } else {

                    }
                    return CompletableFuture.<BankCommandReply>failedFuture(new Exception(""));
         */


    private static void start(ActorSystem<CommandWrapper> system, OffsetStore offsetStore) {
        System.out.println(system.path().root());
        System.out.println(system.path().name());
        System.out.println(system.path().toString());

        logger.info("Adding money started");
        addMoney(system, offsetStore, "2", 10L, Consistency.STRONG)
                .thenCompose(reply -> {
                    logger.info("Money added finished");
                    logger.info("Removing money started ");
                    return removeMoney(system, offsetStore, "1", 1L, Consistency.EVENTUAL);
                })
                .thenCompose(reply -> {
                    logger.info("Money removed finished");
                    logger.info("Getting money started");
                    return getAccountBalance(system, offsetStore, "1");
                })
                .thenCompose(reply -> {
                    logger.info("Getting money finished");
                    logger.info("Removing money started ");
                    return removeMoney(system, offsetStore, "1", 2L, Consistency.EVENTUAL);
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
