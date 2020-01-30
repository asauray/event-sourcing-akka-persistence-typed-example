package net.sauray.domain.commands.replies;

/*
 * InsufficientFunds.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class InsufficientFunds implements BankCommandReply {

  private final Long amountCents;
  private final Long requiredAmountCents;

  public InsufficientFunds(Long amountCents, Long requiredAmountCents) {
    this.amountCents = amountCents;
    this.requiredAmountCents = requiredAmountCents;
  }

  public Long getAmountCents() {
    return amountCents;
  }

  public Long getRequiredAmountCents() {
    return requiredAmountCents;
  }

  @Override
  public String toString() {
    return String.format("Bank Command failed: insufficient funds %d, required %d", this.amountCents, this.requiredAmountCents);
  }
}
