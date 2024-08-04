package com.nisovin.shopkeepers.types;

import java.util.List;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.types.SelectableType;

public abstract class AbstractSelectableType extends AbstractType implements SelectableType {

	protected AbstractSelectableType(String identifier, @Nullable String permission) {
		super(identifier, permission);
	}

	protected AbstractSelectableType(
			String identifier,
			List<? extends String> aliases,
			@Nullable String permission
	) {
		super(identifier, aliases, permission);
	}

	protected abstract void onSelect(Player player);
}
