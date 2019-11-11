package com.nisovin.shopkeepers.compat.api;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface NMSCallProvider {

	public String getVersionId();

	public void overwriteLivingEntityAI(LivingEntity entity);

	// whether tickAI and getCollisionDistance are supported
	public default boolean supportsCustomMobAI() {
		return true;
	}

	public void tickAI(LivingEntity entity);

	public void setOnGround(Entity entity, boolean onGround);

	// on some MC versions (ex. MC 1.9, 1.10) NoAI only disables AI
	public default boolean isNoAIDisablingGravity() {
		return true;
	}

	public void setNoclip(Entity entity);

	/**
	 * Checks if the <code>provided</code> itemstack fulfills the requirements of a trading recipe requiring the given
	 * <code>required</code> itemstack.
	 * <p>
	 * This mimics minecraft's item comparison: This checks if the itemstacks are either both emtpy, or of same type and
	 * the provided itemstack's metadata contains all the contents of the required itemstack's metadata (with list
	 * metadata having to be equal).
	 * 
	 * @param provided
	 *            the provided itemstack
	 * @param required
	 *            the required itemstack
	 * @return <code>true</code> if the provided itemstack matches the required itemstack
	 */
	public boolean matches(ItemStack provided, ItemStack required);

	// Note: It is not safe to reduce the number of trading recipes! Reducing the size below the selected index can
	// crash the client. It's left to the caller to ensure that the number of recipes does not get reduced, for example
	// by inserting dummy entries
	public void updateTrades(Player player);

	// for use in chat hover messages, null if not supported
	public String getItemSNBT(ItemStack itemStack);

	// for use in translatable item type names, null if not supported
	// Note: This might not necessarily match an ItemStack's name (for example for certain items, such as different
	// types of potions, skulls, etc.)
	public String getItemTypeTranslationKey(Material material);
}
