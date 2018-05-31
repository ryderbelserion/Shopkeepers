package com.nisovin.shopkeepers.types;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractType {

	/**
	 * An unique identifier for this type object.
	 * <p>
	 * This could for example be used inside save/configuration files, so it should not contain any characters which
	 * could cause problems with that.
	 */
	protected final String identifier; // not null or empty
	/**
	 * The permission a player needs in order to use/access this type in some way.
	 * <p>
	 * Can be <code>null</code> to indicate that no permission is needed.
	 */
	protected final String permission; // can be null

	protected AbstractType(String identifier, String permission) {
		Validate.notEmpty(identifier, "Empty identifier!");
		this.identifier = identifier;
		this.permission = Utils.isEmpty(permission) ? null : permission;
	}

	public final String getIdentifier() {
		return identifier;
	}

	public boolean hasPermission(Player player) {
		return (permission == null || Utils.hasPermission(player, permission));
	}

	public boolean isEnabled() {
		return true;
	}

	/**
	 * Checks if the given (possibly inaccurate) identifier matches to this type.
	 * 
	 * @param identifier
	 *            an (possible inaccurate) identifier
	 * @return <code>true</code> if the given identifier is considered to represent this type
	 */
	public boolean matches(String identifier) {
		identifier = Utils.normalize(identifier);
		return Utils.normalize(this.identifier).equals(identifier);
	}
}
