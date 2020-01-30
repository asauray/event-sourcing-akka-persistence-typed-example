package net.sauray.domain;

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

/*
 * BankAccountEntity.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class BankAccountEntity extends EventSourcedBehavior<BankCommand, BankEvent, BankAccountState>
{

  // static EventSourcedBehavior<BankCommand, BankEvent, BankState> eventSourcedBehavior = new BankAccountEntity(new PersistenceId("test"));
  public static EventSourcedBehavior<BankCommand, BankEvent, BankAccountState> createBehavior(String persistenceId) {
    return new BankAccountEntity(new PersistenceId("test"));
  }

  public BankAccountEntity(PersistenceId persistenceId) {
    super(persistenceId);
  }

  @Override
  public BankAccountState emptyState() {
    return new BankAccountState(0l);
  }

  @Override
  public CommandHandler<BankCommand, BankEvent, BankAccountState> commandHandler() {
    CommandHandlerBuilder<BankCommand, BankEvent, BankAccountState> builder = newCommandHandlerBuilder();

    builder
      .forState(state -> state.getAmountCents() > 0)
      .onCommand(RemoveMoneyFromAccount.class, (state, cmd) -> handleRemoveMoneyFromAccount(state, cmd))
      .onCommand(GetAccountBalance.class, (state, cmd) -> Effect().reply(cmd, new BankCommandSuccessfulReply(state)));

    builder.forState(state -> state.getAmountCents() <= 0)
      .onCommand(RemoveMoneyFromAccount.class, cmd -> Effect().reply(cmd, new InsufficientFunds(0l, cmd.getAmountCents())));

    builder
      .forAnyState()
      .onCommand(AddMoneyOnAccount.class, cmd -> handleAddMoneyOnAccount(cmd));


    return builder
      .build();
  }

  @Override
  public EventHandler<BankAccountState, BankEvent> eventHandler() {
    return newEventHandlerBuilder()
      .forAnyState()
      .onEvent(MoneyAddedToAccount.class, (state, event) -> handleMoneyAddedToAccount(state, event))
      .onEvent(MoneyRemovedFromAccount.class, (state, event) -> handleMoneyRemovedFromAccount(state, event))
      .build();
  }

  public Effect<BankEvent, BankAccountState> handleAddMoneyOnAccount(AddMoneyOnAccount command) {
    BankEvent event = new MoneyAddedToAccount(command.getAmountCents());
    return 
      Effect()
      .persist(event)
      .thenReply(command, state -> new BankCommandSuccessfulReply(state));
  }

  public Effect<BankEvent, BankAccountState> handleRemoveMoneyFromAccount(BankAccountState previousState, RemoveMoneyFromAccount command) {
    if(previousState.getAmountCents() - command.getAmountCents() > 0) {
      BankEvent event = new MoneyRemovedFromAccount(command.getAmountCents());
      return Effect()
        .persist(event)
        .thenReply(command, state -> new BankCommandSuccessfulReply(state));
    } else {
      return Effect()
        .reply(command, new InsufficientFunds(previousState.getAmountCents(), command.getAmountCents()));
    }

  }

  public BankAccountState handleMoneyAddedToAccount(BankAccountState state, MoneyAddedToAccount event) {
    return state.add(event.getAmountCents());
  }

  public BankAccountState handleMoneyRemovedFromAccount(BankAccountState state, MoneyRemovedFromAccount event) {
    return state.remove(event.getAmountCents());
  }
}

