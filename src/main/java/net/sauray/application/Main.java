package net.sauray.application;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.persistence.typed.javadsl.EventSourcedBehavior;
import net.sauray.domain.BankAccountEntity;
import net.sauray.domain.commands.*;
import net.sauray.domain.commands.replies.BankCommandReply;
import net.sauray.domain.events.*;
import net.sauray.domain.states.*;

/**
 * Hello world!
 *
 */
public class Main 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        EventSourcedBehavior<BankCommand, BankEvent, BankAccountState> behavior =  BankAccountEntity.createBehavior("1");
        ActorSystem<BankCommand> system = ActorSystem.create(behavior, "bank-accounts");
        final ActorRef<BankCommand> ref = system;

        CompletionStage<BankCommandReply> result1 = AskPattern.ask(
            ref,
            replyTo -> new AddMoneyOnAccount(10l, replyTo), 
            Duration.ofSeconds(3),
            system.scheduler()
        );
        result1.whenComplete(
            (reply, failure) -> {
              System.out.println(reply);
            }
        );

        CompletionStage<BankCommandReply> result2 = AskPattern.ask(
            ref,
            replyTo -> new RemoveMoneyFromAccount(1l, replyTo), 
            Duration.ofSeconds(3),
            system.scheduler()
        );
        result2.whenComplete(
            (reply, failure) -> {
              System.out.println(reply);
            }
        );

        CompletionStage<BankCommandReply> result3 = AskPattern.ask(
            ref,
            replyTo -> new GetAccountBalance(replyTo), 
            Duration.ofSeconds(3),
            system.scheduler()
        );
        result3.whenComplete(
            (reply, failure) -> {
              System.out.println(reply);
            }
        );

        CompletionStage<BankCommandReply> result4 = AskPattern.ask(
            ref,
            replyTo -> new RemoveMoneyFromAccount(15l, replyTo), 
            Duration.ofSeconds(3),
            system.scheduler()
        );
        result4.whenComplete(
            (reply, failure) -> {
              System.out.println(reply);
            }
        );
    }
}
