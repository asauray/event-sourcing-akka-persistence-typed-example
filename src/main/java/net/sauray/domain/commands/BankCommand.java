package net.sauray.domain.commands;

import akka.persistence.typed.ExpectingReply;
import net.sauray.domain.commands.replies.BankCommandReply;

/*
 * BankCommand.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public interface BankCommand extends ExpectingReply<BankCommandReply>
{
	
}
