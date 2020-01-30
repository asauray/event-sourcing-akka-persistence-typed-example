package net.sauray.domain.commands.replies;

import net.sauray.domain.states.BankAccountState;

/*
 * BankCommandSuccessfulReply.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class BankCommandSuccessfulReply implements BankCommandReply
{

  private final BankAccountState accountState;

  public BankCommandSuccessfulReply(BankAccountState accountState) {
    this.accountState = accountState; 
  }

  public BankAccountState getAccountState() {
    return accountState;
  }

  @Override
  public String toString() {
    return String.format("Bank Command Success: account balance is now %d", accountState.getAmountCents());
  }
}

