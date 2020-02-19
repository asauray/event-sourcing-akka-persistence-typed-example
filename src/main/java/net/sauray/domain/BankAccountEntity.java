package net.sauray.domain;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandler;
import akka.persistence.typed.javadsl.CommandHandlerBuilder;
import akka.persistence.typed.javadsl.Effect;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;

import net.sauray.domain.commands.*;
import net.sauray.domain.commands.replies.*;
import net.sauray.domain.events.*;
import net.sauray.domain.states.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/*
 * BankAccountEntity.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class BankAccountEntity extends EventSourcedBehavior<BankCommand, BankEvent, BankAccountState>
{

  // this makes the context available to the command handler etc.
  private final ActorContext<BankCommand> context;

  // optionally if you only need `ActorContext.getSelf()`
  private final ActorRef<BankCommand> self;

  public static String TAG = "bank-account";
  public static Set<String> tags = new HashSet<>(Collections.singletonList(TAG));


  public static Behavior<BankCommand> create(String persistenceId) {
    return Behaviors.setup(context -> new BankAccountEntity(new PersistenceId(persistenceId), context));
  }

  public BankAccountEntity(PersistenceId persistenceId, ActorContext<BankCommand> context) {
    super(persistenceId);
    this.context = context;
    this.self = context.getSelf();
  }

  @Override
  public BankAccountState emptyState() {
    return new BankAccountState(0L);
  }

  @Override
  public CommandHandler<BankCommand, BankEvent, BankAccountState> commandHandler() {
    CommandHandlerBuilder<BankCommand, BankEvent, BankAccountState> builder = newCommandHandlerBuilder();

    builder
      .forState(state -> state.getAmountCents() > 0)
      .onCommand(RemoveMoneyFromAccount.class, this::handleRemoveMoneyFromAccount);

    builder.forState(state -> state.getAmountCents() <= 0)
      .onCommand(RemoveMoneyFromAccount.class, cmd -> Effect().reply(cmd.getReplyTo(), new InsufficientFunds(0L, cmd.getAmountCents())));

    builder
      .forAnyState()
      .onCommand(AddMoneyOnAccount.class, this::handleAddMoneyOnAccount)
      .onCommand(GetAccountBalance.class, (state, cmd) -> Effect().reply(cmd.getReplyTo(), new BankCommandSuccessfulReply(state, null)));


    return builder
      .build();
  }

  @Override
  public Set<String> tagsFor(BankEvent event) {
    return tags;
  }

  @Override
  public EventHandler<BankAccountState, BankEvent> eventHandler() {
    return newEventHandlerBuilder()
      .forAnyState()
      .onEvent(MoneyAddedToAccount.class, this::handleMoneyAddedToAccount)
      .onEvent(MoneyRemovedFromAccount.class, this::handleMoneyRemovedFromAccount)
      .build();
  }

  public Effect<BankEvent, BankAccountState> handleAddMoneyOnAccount(AddMoneyOnAccount command) {
    BankEvent event = new MoneyAddedToAccount(UUID.randomUUID(), command.getAmountCents());
    return
      Effect()
      .persist(event)
      .thenReply(command.getReplyTo(), state -> new BankCommandSuccessfulReply(state, event.id()));
  }

  public Effect<BankEvent, BankAccountState> handleRemoveMoneyFromAccount(BankAccountState previousState, RemoveMoneyFromAccount command) {
    if(previousState.getAmountCents() - command.getAmountCents() > 0) {
      BankEvent event = new MoneyRemovedFromAccount(UUID.randomUUID(), command.getAmountCents());
      return Effect()
        .persist(event)
        .thenReply(command.getReplyTo(), state -> new BankCommandSuccessfulReply(state, event.id()));
    } else {
      return Effect()
        .reply(command.getReplyTo(), new InsufficientFunds(previousState.getAmountCents(), command.getAmountCents()));
    }
  }

  public BankAccountState handleMoneyAddedToAccount(BankAccountState state, MoneyAddedToAccount event) {
    return state.add(event.getAmountCents());
  }

  public BankAccountState handleMoneyRemovedFromAccount(BankAccountState state, MoneyRemovedFromAccount event) {
    return state.remove(event.getAmountCents());
  }
}

