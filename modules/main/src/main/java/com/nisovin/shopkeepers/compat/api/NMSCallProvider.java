package com.nisovin.shopkeepers.compat.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public interface NMSCallProvider {

	void overwriteLivingEntityAI(LivingEntity entity);

	// Whether tickAI and getCollisionDistance are supported.
	default boolean supportsCustomMobAI() {
		return true;
	}

	void tickAI(LivingEntity entity, int ticks);

	void setOnGround(Entity entity, boolean onGround);

	// On some MC versions (e.g. MC 1.9, 1.10) NoAI only disables AI.
	default boolean isNoAIDisablingGravity() {
		return true;
	}

	void setNoclip(Entity entity);

	// Performs any version-specific setup that needs to happen before the entity is spawned. The
	// available operations may be limited during this phase of the entity spawning.
	default void prepareEntity(Entity entity) {

	}

	// Performs any version-specific setup of the entity that needs to happen right after the entity
	// was spawned.
	default void setupSpawnedEntity(Entity entity) {

	}

	default boolean matches(
            @ReadOnly @Nullable ItemStack provided,
            @Nullable UnmodifiableItemStack required
    ) {
		return this.matches(provided, ItemUtils.asItemStackOrNull(required));
	}

	/**
	 * Checks if the <code>provided</code> item stack fulfills the requirements of a trading recipe
	 * requiring the given <code>required</code> item stack.
	 * <p>
	 * This mimics Minecraft's item comparison: This checks if the item stacks are either both
	 * empty, or of same type and the provided item stack's metadata contains all the contents of
	 * the required item stack's metadata (with any list metadata being equal).
	 *
	 * @param provided
	 *            the provided item stack
	 * @param required
	 *            the required item stack, this may be an unmodifiable item stack
	 * @return <code>true</code> if the provided item stack matches the required item stack
	 */
    boolean matches(
            @ReadOnly @Nullable ItemStack provided,
            @ReadOnly @Nullable ItemStack required
    );

	// Note: It is not safe to reduce the number of trading recipes! Reducing the size below the
	// selected index can crash the client. It's left to the caller to ensure that the number of
	// recipes does not get reduced, for example by inserting dummy entries.
    void updateTrades(Player player);

	// For use in chat hover messages, null if not supported.
	// TODO: Bukkit 1.20.6 also contains ItemMeta#getAsString now. However, this only includes the
	// item's NBT data, not the full item stack NBT. And BungeeCord's HoverEvent Item content does
	// not correctly serialize the data currently
	// (https://github.com/SpigotMC/BungeeCord/issues/3688).
	public @Nullable String getItemSNBT(@ReadOnly ItemStack itemStack);
}
