package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.PlayerCommand;

class CommandConfirm extends PlayerCommand {

	private final Confirmations confirmations;

	CommandConfirm(Confirmations confirmations) {
		super(Arrays.asList("confirm"));
		this.confirmations = confirmations;

		// set description:
		this.setDescription("Confirms a potentially damaging action.");

		// hidden command:
		this.setHiddenInParentHelp(true);
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		assert (input.getSender() instanceof Player);
		Player player = (Player) input.getSender();
		confirmations.handleConfirmation(player);
	}
}
