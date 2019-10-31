package com.nisovin.shopkeepers.commands.shopkeepers;

import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
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
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		confirmations.handleConfirmation(input.getSender());
	}
}
