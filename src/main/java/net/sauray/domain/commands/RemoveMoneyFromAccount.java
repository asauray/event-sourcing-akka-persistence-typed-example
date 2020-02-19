package net.sauray.domain.commands;

import akka.actor.typed.ActorRef;
import net.sauray.domain.Utils;
import net.sauray.domain.commands.replies.BankCommandReply;

/*
 * RemoveMoneyFromAccount.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class RemoveMoneyFromAccount implements BankCommand
{
  
  private final Long amountCents;
  private final String accountId;
  private final ActorRef<BankCommandReply> actorReplyTo;

  public RemoveMoneyFromAccount(String accountId, Long amountCents, ActorRef<BankCommandReply> replyTo) {
    Utils.validateIsPositive(amountCents);
    this.accountId = accountId;
    this.amountCents = amountCents;
    this.actorReplyTo = replyTo;
  }

  @Override
  public String entityId() {
    return accountId;
  }

  public Long getAmountCents() {
    return amountCents;
  }

  @Override
  public ActorRef<BankCommandReply> getReplyTo() {
    return actorReplyTo;
  }
}

