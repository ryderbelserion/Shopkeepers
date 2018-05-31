package com.nisovin.shopkeepers.types;

import org.bukkit.entity.Player;

public abstract class SelectableType extends AbstractType {

	protected SelectableType(String identifier, String permission) {
		super(identifier, permission);
	}

	protected abstract void onSelect(Player player);
}
