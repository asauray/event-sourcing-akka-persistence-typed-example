package net.sauray.domain.commands;

import akka.actor.typed.ActorRef;
import net.sauray.domain.commands.replies.BankCommandReply;

/*
 * GetAccountBalance.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class GetAccountBalance implements BankCommand
{
  private final ActorRef<BankCommandReply> actorReplyTo;
  private final String accountId;
  public GetAccountBalance(String accountId, ActorRef<BankCommandReply> actorReplyTo) {
    this.accountId = accountId;
    this.actorReplyTo = actorReplyTo; 
  }

  @Override
  public String entityId() {
    return accountId;
  }

  @Override
  public ActorRef<BankCommandReply> getReplyTo() {
    return actorReplyTo;
  }
}

