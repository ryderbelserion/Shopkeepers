package com.nisovin.shopkeepers.compat.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public interface NMSCallProvider {

	public String getVersionId();

	public void overwriteLivingEntityAI(LivingEntity entity);

	// whether tickAI and getCollisionDistance are supported
	public default boolean supportsCustomMobAI() {
		return true;
	}

	public void tickAI(LivingEntity entity);

	// returns the distance to the nearest block collision in the range of the given direction vector
	// note: this uses the blocks collision bounding boxes (so this goes through passable blocks, like liquids, etc.)
	// note: does not modify the start location and direction vector
	public double getCollisionDistance(Location start, Vector direction);

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
}
