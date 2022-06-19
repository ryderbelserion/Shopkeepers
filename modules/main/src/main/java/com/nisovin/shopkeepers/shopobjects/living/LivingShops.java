package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;

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

				// TODO Remove this migration again at some point
				// MC 1.16:
				// Convert pig-zombie to zombified-piglin (but only if we run on MC 1.16 or above):
				if (objectTypeId.equals("pig-zombie")) {
					shopObjectData.set(AbstractShopObject.SHOP_OBJECT_TYPE_ID, "zombified-piglin");
					Log.warning(logPrefix + "Migrated object type 'pig-zombie' to 'zombified-piglin'.");
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
	private final CreatureForceSpawnListener creatureForceSpawnListener = new CreatureForceSpawnListener();

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
		// Register force-creature-spawn event handler:
		if (Settings.bypassSpawnBlocking) {
			Bukkit.getPluginManager().registerEvents(creatureForceSpawnListener, plugin);
		}
	}

	public void onDisable() {
		livingEntityShopListener.onDisable();
		HandlerList.unregisterAll(creatureForceSpawnListener);
		// Reset force spawning:
		creatureForceSpawnListener.forceCreatureSpawn(null, null);

		// Stop living entity AI:
		livingEntityAI.onDisable();
	}

	public SKLivingShopObjectTypes getLivingShopObjectTypes() {
		return livingShopObjectTypes;
	}

	public LivingEntityAI getLivingEntityAI() {
		return livingEntityAI;
	}

	// Bypassing creature spawn blocking plugins ('region protection' plugins):
	void forceCreatureSpawn(Location location, EntityType entityType) {
		if (Settings.bypassSpawnBlocking) {
			creatureForceSpawnListener.forceCreatureSpawn(location, entityType);
		}
	}
}
