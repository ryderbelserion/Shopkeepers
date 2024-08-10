package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.logging.Log;

public class LivingShops {

	static {
		// Register shopkeeper data migrations:
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"living-shop-object-types",
				MigrationPhase.EARLY
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				ShopObjectData shopObjectData = shopkeeperData.getOrNullIfMissing(
						AbstractShopkeeper.SHOP_OBJECT_DATA
				);
				if (shopObjectData == null) return false;

				String objectTypeId = shopObjectData.getOrNullIfMissing(
						AbstractShopObject.SHOP_OBJECT_TYPE_ID
				);
				if (objectTypeId == null) {
					return false; // Shop object type is missing. -> Skip migration.
				}

				boolean migrated = false;

				// TODO Remove these migrations again at some point
				// MC 1.16:
				// Convert 'pig-zombie' to 'zombified-piglin':
				if (objectTypeId.equals("pig-zombie")) {
					shopObjectData.set(AbstractShopObject.SHOP_OBJECT_TYPE_ID, "zombified-piglin");
					Log.warning(logPrefix + "Migrated object type 'pig-zombie' to 'zombified-piglin'.");
					migrated = true;
				}

				// MC 1.20.5:
				// Convert 'mushroom-cow' to 'mooshroom':
				if (objectTypeId.equals("mushroom-cow")) {
					shopObjectData.set(AbstractShopObject.SHOP_OBJECT_TYPE_ID, "mooshroom");
					Log.warning(logPrefix + "Migrated object type 'mushroom-cow' to 'mooshroom'.");
					migrated = true;
				}
				// Convert 'snowman' to 'snow-golem':
				if (objectTypeId.equals("snowman")) {
					shopObjectData.set(AbstractShopObject.SHOP_OBJECT_TYPE_ID, "snow-golem");
					Log.warning(logPrefix + "Migrated object type 'snowman' to 'snow-golem'.");
					migrated = true;
				}

				return migrated;
			}
		});
	}

	private final SKShopkeepersPlugin plugin;
	private final SKLivingShopObjectTypes livingShopObjectTypes = new SKLivingShopObjectTypes(
			Unsafe.initialized(this)
	);
	private final LivingEntityAI livingEntityAI;
	private final LivingEntityShopListener livingEntityShopListener;

	public LivingShops(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		livingEntityAI = new LivingEntityAI(plugin);
		livingEntityShopListener = new LivingEntityShopListener(plugin);
	}

	public void onRegisterDefaults() {
		livingShopObjectTypes.onRegisterDefaults();
	}

	public void onEnable() {
		livingEntityAI.onEnable();
		livingEntityShopListener.onEnable();
	}

	public void onDisable() {
		livingEntityShopListener.onDisable();

		// Stop living entity AI:
		livingEntityAI.onDisable();
	}

	public SKLivingShopObjectTypes getLivingShopObjectTypes() {
		return livingShopObjectTypes;
	}

	public LivingEntityAI getLivingEntityAI() {
		return livingEntityAI;
	}

	// Bypassing creature spawn blocking plugins (e.g. region protection plugins):
	void forceCreatureSpawn(Location location, EntityType entityType) {
		if (Settings.bypassSpawnBlocking) {
			plugin.getForcingCreatureSpawner().forceCreatureSpawn(location, entityType);
		}
	}
}
