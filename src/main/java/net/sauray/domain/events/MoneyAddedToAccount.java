package net.sauray.domain.events;

/*
 * MoneyRemoveFromAccount.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

import java.util.UUID;

public class MoneyAddedToAccount implements BankEvent
{

  private UUID id;
  private final Long amountCents;

  public MoneyAddedToAccount(UUID id, Long amountCents) {
    this.id = id;
    this.amountCents = amountCents;
  }

  public Long getAmountCents() {
    return amountCents;
  }

  @Override
  public UUID id() {
    return id;
  }
}

