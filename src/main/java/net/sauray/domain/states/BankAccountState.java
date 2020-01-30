package net.sauray.domain.states;

/*
 * BankState.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class BankAccountState
{

  private Long amountCents;

  public BankAccountState(Long amountCents) {
    this.amountCents = amountCents; 
  }

  public Long getAmountCents() {
    return amountCents;
  }

  public BankAccountState add(Long amount) {
    return new BankAccountState(this.amountCents + amount);
  }

  public BankAccountState remove(Long amount) {
    return new BankAccountState(this.amountCents - amount);
  }

  @Override
  public String toString() {
    return String.format("Bank Account: %d", amountCents);
  }

}

