package net.sauray.domain.commands.replies;

import net.sauray.domain.states.BankAccountState;

import java.util.UUID;

/*
 * BankCommandSuccessfulReply.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class BankCommandSuccessfulReply implements BankCommandReply
{

  public final BankAccountState accountState;
  private UUID eventId;

  public BankCommandSuccessfulReply(BankAccountState accountState, UUID eventId) {
    this.accountState = accountState;
    this.eventId = eventId;
  }

  public BankAccountState getAccountState() {
    return accountState;
  }

  @Override
  public String toString() {
    return String.format("Bank Command Success: account balance is now %d", accountState.getAmountCents());
  }

  @Override
  public UUID eventId() {
    return eventId;
  }
}

