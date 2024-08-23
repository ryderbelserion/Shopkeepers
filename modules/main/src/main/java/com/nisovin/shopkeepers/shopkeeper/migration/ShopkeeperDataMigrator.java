package com.nisovin.shopkeepers.shopkeeper.migration;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Access point to register and invoke shopkeeper data migrations.
 */
public final class ShopkeeperDataMigrator {

	private static final List<Migration> migrations = new ArrayList<>();

	/**
	 * Registers the given {@link Migration}.
	 * <p>
	 * Each migration is expected to be registered only once, and then never unregistered again. The
	 * name of the migration needs to be unique among all registered migrations.
	 * 
	 * @param migration
	 *            the migration, not <code>null</code>
	 */
	public static void registerMigration(Migration migration) {
		Validate.notNull(migration, "migration is null");
		// Check for migrations with duplicate names:
		String migrationName = migration.getName();
		for (Migration otherMigration : migrations) {
			if (otherMigration.getName().equalsIgnoreCase(migrationName)) {
				Validate.error("There already exists another shopkeeper data migration with the same name: "
						+ migrationName);
			}
		}

		migrations.add(migration);
	}

	/**
	 * Logs the registered shopkeeper data migrations.
	 */
	public static void logRegisteredMigrations() {
		Log.info("Registered shopkeeper data migrations:");
		migrations.forEach(migration -> {
			Log.info("  - " + migration.getName() + " (" + migration.getTargetPhase().getName() + ")");
		});
	}

	// TODO Replace the logPrefix with a PrefixedLogger or similar.
	/**
	 * Applies migrations to the given shopkeeper data.
	 * <p>
	 * This operation does not check if the given data is complete or if all of it is valid: Some
	 * missing or invalid data may be silently ignored, whereas invalid data that is relevant to the
	 * migration may cause the migration to fail with an {@link InvalidDataException}. However,
	 * missing data should never result in the migration to fail, but rather cause the affected
	 * migrations to be silently skipped.
	 * 
	 * @param shopkeeperData
	 *            the shopkeeper data, not <code>null</code>
	 * @param logPrefix
	 *            a context specific log prefix, can be empty, not <code>null</code>
	 * @return <code>true</code> if the data has changed as a result of these migrations
	 * @throws InvalidDataException
	 *             if the data is invalid and cannot be migrated
	 */
	public static boolean migrate(
			ShopkeeperData shopkeeperData,
			String logPrefix
	) throws InvalidDataException {
		Validate.notNull(shopkeeperData, "shopkeeperData is null");
		Validate.notNull(logPrefix, "logPrefix is null");
		boolean migrated = false;

		// Early migration phase:
		MigrationPhase currentPhase = MigrationPhase.EARLY;
		migrated |= migrate(currentPhase, shopkeeperData, logPrefix);

		// Default migration phase:
		currentPhase = MigrationPhase.DEFAULT;
		migrated |= migrate(currentPhase, shopkeeperData, logPrefix);

		// Shopkeeper class specific data migrations:
		AbstractShopType<?> shopType = shopkeeperData.getOrNullIfMissing(AbstractShopkeeper.SHOP_TYPE);
		if (shopType != null) {
			currentPhase = MigrationPhase.ofShopkeeperClass(shopType.getShopkeeperClass());
			migrated |= migrate(currentPhase, shopkeeperData, logPrefix);
		}

		// Shop object class specific data migrations:
		ShopObjectData shopObjectData = shopkeeperData.getOrNullIfMissing(AbstractShopkeeper.SHOP_OBJECT_DATA);
		if (shopObjectData != null) {
			AbstractShopObjectType<?> shopObjectType = shopObjectData.getOrNullIfMissing(AbstractShopObject.SHOP_OBJECT_TYPE);
			if (shopObjectType != null) {
				currentPhase = MigrationPhase.ofShopObjectClass(shopObjectType.getShopObjectClass());
				migrated |= migrate(currentPhase, shopkeeperData, logPrefix);
			}
		}

		// Late migration phase:
		currentPhase = MigrationPhase.LATE;
		migrated |= migrate(currentPhase, shopkeeperData, logPrefix);

		return migrated;
	}

	private static boolean migrate(
			MigrationPhase currentPhase,
			ShopkeeperData shopkeeperData,
			String logPrefix
	) throws InvalidDataException {
		boolean migrated = false;
		for (Migration migration : migrations) {
			if (migration.getTargetPhase().isApplicable(currentPhase)) {
				migrated |= migration.migrate(shopkeeperData, logPrefix);
			}
		}
		return migrated;
	}

	private ShopkeeperDataMigrator() {
	}
}
