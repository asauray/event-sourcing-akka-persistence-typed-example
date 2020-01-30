package net.sauray.domain.commands;

import akka.actor.typed.ActorRef;
import net.sauray.domain.Utils;
import net.sauray.domain.commands.replies.BankCommandReply;

/*
 * PutMoneyOnAccount.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class AddMoneyOnAccount implements BankCommand
{
  
  private final Long amountCents;
  private final ActorRef<BankCommandReply> actorReplyTo;

  public AddMoneyOnAccount(Long amountCents, ActorRef<BankCommandReply> replyTo) {
    Utils.validateIsPositive(amountCents);
    this.amountCents = amountCents;
    this.actorReplyTo = replyTo;
  }

  public Long getAmountCents() {
    return amountCents;
  }

  @Override
  public ActorRef<BankCommandReply> replyTo() {
    return this.actorReplyTo;
  }
}

