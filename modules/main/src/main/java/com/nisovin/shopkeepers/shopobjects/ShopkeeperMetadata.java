package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Helper functions related to shopkeeper metadata.
 */
public final class ShopkeeperMetadata {

	public static final String SHOPKEEPER_METADATA_KEY = "shopkeeper";

	// ENTITY METADATA

	public static void apply(Entity entity) {
		Validate.notNull(entity, "entity is null");
		entity.setMetadata(SHOPKEEPER_METADATA_KEY, new FixedMetadataValue(
				ShopkeepersPlugin.getInstance(),
				true
		));
	}

	public static void remove(Entity entity) {
		Validate.notNull(entity, "entity is null");
		entity.removeMetadata(SHOPKEEPER_METADATA_KEY, ShopkeepersPlugin.getInstance());
	}

	public static boolean isTagged(Entity entity) {
		Validate.notNull(entity, "entity is null");
		return entity.hasMetadata(SHOPKEEPER_METADATA_KEY);
	}

	// BLOCK METADATA

	public static void apply(Block block) {
		Validate.notNull(block, "block is null");
		block.setMetadata(SHOPKEEPER_METADATA_KEY, new FixedMetadataValue(
				ShopkeepersPlugin.getInstance(),
				true
		));
	}

	public static void remove(Block block) {
		Validate.notNull(block, "block is null");
		block.removeMetadata(SHOPKEEPER_METADATA_KEY, ShopkeepersPlugin.getInstance());
	}

	public static boolean isTagged(Block block) {
		Validate.notNull(block, "block is null");
		return block.hasMetadata(SHOPKEEPER_METADATA_KEY);
	}

	private ShopkeeperMetadata() {
	}
}
