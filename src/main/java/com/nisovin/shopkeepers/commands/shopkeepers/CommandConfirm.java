package com.nisovin.shopkeepers.commands.shopkeepers;

import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;

class CommandConfirm extends Command {

	private final Confirmations confirmations;

	CommandConfirm(Confirmations confirmations) {
		super("confirm");
		this.confirmations = confirmations;

		// set description:
		this.setDescription("Confirms a potentially dangerous action.");

		// hidden command:
		this.setHiddenInParentHelp(true);
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		confirmations.handleConfirmation(input.getSender());
	}
}
