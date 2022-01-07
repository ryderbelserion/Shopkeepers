package com.nisovin.shopkeepers.commands.shopkeepers.snapshot;

import java.util.Arrays;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.lib.commands.HelpCommand;

public class CommandSnapshotHelp extends HelpCommand {

	public CommandSnapshotHelp(CommandSnapshot helpSource) {
		super("help", Arrays.asList("?"), helpSource);

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SNAPSHOT_PERMISSION);

		// Hidden to reduce the number of commands shown in the parent help:
		this.setHiddenInParentHelp(true);
	}
}
