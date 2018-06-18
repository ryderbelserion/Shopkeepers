package com.nisovin.shopkeepers.types;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.types.Type;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractType implements Type {

	/**
	 * An unique identifier.
	 * <p>
	 * Make sure that the identifier (and its {@link StringUtils#normalize(String) normalization}) are unique among all
	 * other {@link Type types} of the same context.
	 * <p>
	 * This could for example be used inside save/configuration files, so it should not contain any characters which
	 * could cause problems with that.
	 */
	protected final String identifier; // not null or empty
	protected final String permission; // can be null

	protected AbstractType(String identifier, String permission) {
		this.identifier = StringUtils.normalize(identifier);
		Validate.notEmpty(this.identifier, "Empty identifier!");
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

	// TODO remove this and instead add aliases?
	@Override
	public boolean matches(String identifier) {
		identifier = StringUtils.normalize(identifier);
		return this.identifier.equals(identifier);
	}

	// not overriding equals and hashCode: only the exact same type instance is considered equal
}
