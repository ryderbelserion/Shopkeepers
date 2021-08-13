package com.nisovin.shopkeepers.user;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.util.java.LRUCache;
import com.nisovin.shopkeepers.util.java.Validate;

public final class SKUser implements User {

	private static final Map<UUID, User> cache = new LRUCache<>(100);

	public static User of(UUID uniqueId, String name) {
		return cache.compute(uniqueId, (uuid, oldUser) -> {
			if (oldUser != null && oldUser.getName().equals(name)) {
				return oldUser;
			} else {
				return new SKUser(uniqueId, name);
			}
		});
	}

	private final UUID uniqueId;
	private final String name;

	private SKUser(UUID uniqueId, String name) {
		Validate.notNull(uniqueId, "uniqueId is null");
		Validate.notEmpty(name, "name is null or empty");
		this.uniqueId = uniqueId;
		this.name = name;
	}

	@Override
	public UUID getUniqueId() {
		return uniqueId;
	}

	@Override
	public String getName() {
		Player player = this.getPlayer();
		if (player != null) return player.getName();
		return name;
	}

	@Override
	public String getDisplayName() {
		Player player = this.getPlayer();
		if (player != null) return player.getDisplayName();
		return name;
	}

	@Override
	public boolean isOnline() {
		return (this.getPlayer() != null);
	}

	@Override
	public Player getPlayer() {
		return Bukkit.getPlayer(uniqueId);
	}

	@Override
	public OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(uniqueId); // Non-blocking
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SKUser [uniqueId=");
		builder.append(uniqueId);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uniqueId.hashCode();
		result = prime * result + name.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof SKUser)) return false;
		SKUser other = (SKUser) obj;
		if (!uniqueId.equals(other.uniqueId)) return false;
		if (!name.equals(other.name)) return false;
		return true;
	}
}
