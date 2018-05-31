package com.nisovin.shopkeepers.types;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.types.Type;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractType implements Type {

	/**
	 * This could for example be used inside save/configuration files, so it should not contain any characters which
	 * could cause problems with that.
	 */
	protected final String identifier; // not null or empty
	protected final String permission; // can be null

	protected AbstractType(String identifier, String permission) {
		Validate.notEmpty(identifier, "Empty identifier!");
		this.identifier = identifier;
		this.permission = StringUtils.isEmpty(permission) ? null : permission;
	}

	@Override
	public final String getIdentifier() {
		return identifier;
	}

	@Override
	public String getPermission() {
		return permission;
	}

	@Override
	public boolean hasPermission(Player player) {
		return (permission == null || Utils.hasPermission(player, permission));
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean matches(String identifier) {
		identifier = StringUtils.normalize(identifier);
		return StringUtils.normalize(this.identifier).equals(identifier);
	}
}
