package net.sauray.domain.events;

/*
 * MoneyRemoveFromAccount.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class MoneyAddedToAccount implements BankEvent
{

  private final Long amountCents;

	public MoneyAddedToAccount(Long amountCents) {
	  this.amountCents = amountCents;	
	}

	public Long getAmountCents() {
		return amountCents;
	}
}

