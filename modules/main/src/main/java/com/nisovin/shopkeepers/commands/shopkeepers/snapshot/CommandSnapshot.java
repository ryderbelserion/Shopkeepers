package com.nisovin.shopkeepers.commands.shopkeepers.snapshot;

import java.util.Arrays;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.Confirmations;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandRegistry;

public class CommandSnapshot extends Command {

	public CommandSnapshot(Confirmations confirmations) {
		super("snapshot", Arrays.asList("snapshots"));

		// Set permission:
		this.setPermission(ShopkeepersPlugin.SNAPSHOT_PERMISSION);

		// Flatten in help:
		this.setHiddenInOwnHelp(true);
		this.setHiddenInParentHelp(true);
		this.setIncludeChildsInParentHelp(true);

		// Register child commands:
		CommandRegistry childCommands = this.getChildCommands();
		childCommands.register(new CommandSnapshotHelp(Unsafe.initialized(this)));
		childCommands.register(new CommandSnapshotList());
		childCommands.register(new CommandSnapshotCreate());
		childCommands.register(new CommandSnapshotRemove(confirmations));
		childCommands.register(new CommandSnapshotRestore());
	}
}
