package com.nisovin.shopkeepers.types;

import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.types.SelectableType;

public abstract class AbstractSelectableType extends AbstractType implements SelectableType {

	protected AbstractSelectableType(String identifier, String permission) {
		super(identifier, permission);
	}

	protected AbstractSelectableType(String identifier, List<String> aliases, String permission) {
		super(identifier, aliases, permission);
	}

	protected abstract void onSelect(Player player);
}
