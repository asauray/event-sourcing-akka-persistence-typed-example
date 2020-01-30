package net.sauray.domain.events;

/*
 * MoneyRemoveFromAccount.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class MoneyRemovedFromAccount implements BankEvent
{

  private final Long amountCents;

  public MoneyRemovedFromAccount(Long amountCents) {
    this.amountCents = amountCents; 
  }

  public Long getAmountCents() {
    return amountCents;
  }
}

