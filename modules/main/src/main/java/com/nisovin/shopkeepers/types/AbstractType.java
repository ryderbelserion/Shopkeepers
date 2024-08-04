package com.nisovin.shopkeepers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.types.Type;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public abstract class AbstractType implements Type {

	/**
	 * A unique identifier.
	 * <p>
	 * Make sure that the identifier (and its {@link StringUtils#normalize(String) normalization})
	 * are unique among all other {@link Type types} of the same context.
	 * <p>
	 * This could for example be used inside save/configuration files, so it should not contain any
	 * characters which could cause problems with that.
	 */
	protected final String identifier; // Not null or empty
	// Unmodifiable, not null, can be empty, normalized:
	protected final List<? extends String> aliases;
	protected final @Nullable String permission; // Can be null

	protected AbstractType(String identifier, @Nullable String permission) {
		this(identifier, Collections.emptyList(), permission);
	}

	protected AbstractType(
			String identifier,
			List<? extends String> aliases,
			@Nullable String permission
	) {
		this.identifier = StringUtils.normalize(identifier);
		Validate.notEmpty(this.identifier, "identifier is null or empty");
		Validate.notNull(aliases, "aliases is null");
		if (aliases.isEmpty()) {
			this.aliases = Collections.emptyList();
		} else {
			List<String> normalizedAliases = new ArrayList<>(aliases.size());
			for (String alias : aliases) {
				Validate.notEmpty(alias, "aliases contains null or empty alias");
				normalizedAliases.add(StringUtils.normalize(alias));
			}
			this.aliases = Collections.unmodifiableList(normalizedAliases);
		}
		this.permission = StringUtils.isEmpty(permission) ? null : permission;
	}

	@Override
	public final String getIdentifier() {
		return identifier;
	}

	@Override
	public Collection<? extends String> getAliases() {
		return aliases;
	}

	@Override
	public @Nullable String getPermission() {
		return permission;
	}

	@Override
	public boolean hasPermission(Player player) {
		if (permission != null) {
			return PermissionUtils.hasPermission(player, permission);
		} else {
			return true;
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean matches(String identifier) {
		Validate.notNull(identifier, "identifier is null");
		String normalized = StringUtils.normalize(identifier);
		if (normalized.equals(this.identifier)) return true;
		if (this.aliases.contains(normalized)) return true;
		String displayName = StringUtils.normalize(this.getDisplayName());
		return normalized.equals(displayName);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getName());
		builder.append(" [identifier=");
		builder.append(identifier);
		builder.append("]");
		return builder.toString();
	}

	// hashCode and equals: Instances are compared by identity.

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	public final boolean equals(@Nullable Object obj) {
		return super.equals(obj);
	}
}
