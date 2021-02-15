package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;

/**
 * Helper functions related to shopkeeper metadata.
 */
public class ShopkeeperMetadata {

	private ShopkeeperMetadata() {
	}

	public static final String SHOPKEEPER_METADATA_KEY = "shopkeeper";

	// ENTITY METADATA

	public static void apply(Entity entity) {
		entity.setMetadata(SHOPKEEPER_METADATA_KEY, new FixedMetadataValue(ShopkeepersPlugin.getInstance(), true));
	}

	public static void remove(Entity entity) {
		entity.removeMetadata(SHOPKEEPER_METADATA_KEY, ShopkeepersPlugin.getInstance());
	}

	public static boolean isTagged(Entity entity) {
		return entity.hasMetadata(SHOPKEEPER_METADATA_KEY);
	}

	// BLOCK METADATA

	public static void apply(Block block) {
		block.setMetadata(SHOPKEEPER_METADATA_KEY, new FixedMetadataValue(ShopkeepersPlugin.getInstance(), true));
	}

	public static void remove(Block block) {
		block.removeMetadata(SHOPKEEPER_METADATA_KEY, ShopkeepersPlugin.getInstance());
	}

	public static boolean isTagged(Block block) {
		return block.hasMetadata(SHOPKEEPER_METADATA_KEY);
	}
}
