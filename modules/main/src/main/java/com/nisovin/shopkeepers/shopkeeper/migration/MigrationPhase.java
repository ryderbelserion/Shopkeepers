package com.nisovin.shopkeepers.shopkeeper.migration;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Shopkeeper data migration phases.
 * <p>
 * These migration phases determine the order in which shopkeeper data {@link Migration migrations}
 * are executed. They also allow migrations to be applied only to the data of specific types of
 * {@link #ofShopkeeperClass(Class) shopkeepers} or {@link #ofShopObjectClass(Class) shop objects}.
 * <p>
 * Each shopkeeper data {@link Migration migration} specifies a {@link Migration#getTargetPhase()
 * target migration phase} at which it wants to execute. This can be one of the fixed migration
 * phases that each shopkeeper data migration goes through, or it can be one of the migration phases
 * that only applies to specific types of shopkeepers or shop objects. If its target migration phase
 * is {@link #isApplicable(MigrationPhase) applicable} in the current migration phase of some
 * shopkeeper data, the migration is {@link Migration#migrate(ShopkeeperData, String) executed}. All
 * migrations that are applicable in the same migration phase are executed in the order of their
 * {@link ShopkeeperDataMigrator#registerMigration(Migration) registration}.
 */
public class MigrationPhase {

	/**
	 * Migrations for this phase are executed first.
	 */
	public static final MigrationPhase EARLY = new MigrationPhase("early");

	/**
	 * Migrations for this phase are executed after {@link #EARLY}.
	 */
	public static final MigrationPhase DEFAULT = new MigrationPhase("default");

	/**
	 * Migrations for the returned phase are executed after {@link #DEFAULT} for shopkeepers of the
	 * specified type or any sub-type.
	 * 
	 * @param shopkeeperClass
	 *            the shopkeeper class, not <code>null</code>
	 * @return the migration phase, not <code>null</code>
	 */
	public static MigrationPhase ofShopkeeperClass(Class<? extends Shopkeeper> shopkeeperClass) {
		return ShopkeeperClassMigrationPhase.of(shopkeeperClass);
	}

	/**
	 * A {@link MigrationPhase} that is only executed for shopkeepers of a specific type or any
	 * sub-type.
	 */
	public static final class ShopkeeperClassMigrationPhase extends MigrationPhase {

		private static final Map<Class<? extends Shopkeeper>, ShopkeeperClassMigrationPhase> CACHE = new HashMap<>();

		private static ShopkeeperClassMigrationPhase of(
				Class<? extends Shopkeeper> shopkeeperClass
		) {
			ShopkeeperClassMigrationPhase migrationPhase = CACHE.computeIfAbsent(
					shopkeeperClass,
					ShopkeeperClassMigrationPhase::new
			);
			assert migrationPhase != null;
			return migrationPhase;
		}

		private static String getName(Class<? extends Shopkeeper> shopkeeperClass) {
			Validate.notNull(shopkeeperClass, "shopkeeperClass is null");
			return ClassUtils.getSimpleTypeName(shopkeeperClass);
		}

		private final Class<? extends Shopkeeper> shopkeeperClass;

		private ShopkeeperClassMigrationPhase(Class<? extends Shopkeeper> shopkeeperClass) {
			super(getName(shopkeeperClass));
			this.shopkeeperClass = shopkeeperClass;
		}

		/**
		 * Gets the shopkeeper class.
		 * 
		 * @return the shopkeeper class, not <code>null</code>
		 */
		public final Class<? extends Shopkeeper> getShopkeeperClass() {
			return shopkeeperClass;
		}

		@Override
		public boolean isApplicable(MigrationPhase migrationPhase) {
			if (this == migrationPhase) return true;
			if (!(migrationPhase instanceof ShopkeeperClassMigrationPhase)) return false;
			ShopkeeperClassMigrationPhase other = (ShopkeeperClassMigrationPhase) migrationPhase;
			return shopkeeperClass.isAssignableFrom(other.shopkeeperClass);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + shopkeeperClass.hashCode();
			return result;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof ShopkeeperClassMigrationPhase)) return false;
			ShopkeeperClassMigrationPhase other = (ShopkeeperClassMigrationPhase) obj;
			return (shopkeeperClass == other.shopkeeperClass);
		}
	}

	/**
	 * Migrations for the returned phase are executed after {@link #ofShopkeeperClass(Class)} for
	 * shop object's of the specified type or any sub-type.
	 * 
	 * @param shopObjectClass
	 *            the shop object class, not <code>null</code>
	 * @return the migration phase, not <code>null</code>
	 */
	public static MigrationPhase ofShopObjectClass(Class<? extends ShopObject> shopObjectClass) {
		return ShopObjectClassMigrationPhase.of(shopObjectClass);
	}

	/**
	 * A {@link MigrationPhase} that is only executed for shop object's of a specific type or any
	 * sub-type.
	 */
	public static final class ShopObjectClassMigrationPhase extends MigrationPhase {

		private static final Map<Class<? extends ShopObject>, ShopObjectClassMigrationPhase> CACHE = new HashMap<>();

		private static ShopObjectClassMigrationPhase of(Class<? extends ShopObject> shopObjectClass) {
			ShopObjectClassMigrationPhase migrationPhase = CACHE.computeIfAbsent(
					shopObjectClass,
					ShopObjectClassMigrationPhase::new
			);
			assert migrationPhase != null;
			return migrationPhase;
		}

		private static String getName(Class<? extends ShopObject> shopObjectClass) {
			Validate.notNull(shopObjectClass, "shopObjectClass is null");
			return ClassUtils.getSimpleTypeName(shopObjectClass);
		}

		private final Class<? extends ShopObject> shopObjectClass;

		private ShopObjectClassMigrationPhase(Class<? extends ShopObject> shopObjectClass) {
			super(getName(shopObjectClass));
			this.shopObjectClass = shopObjectClass;
		}

		/**
		 * Gets the shop object class.
		 * 
		 * @return the shop object class, not <code>null</code>
		 */
		public final Class<? extends ShopObject> getShopObjectClass() {
			return shopObjectClass;
		}

		@Override
		public boolean isApplicable(MigrationPhase migrationPhase) {
			if (this == migrationPhase) return true;
			if (!(migrationPhase instanceof ShopObjectClassMigrationPhase)) return false;
			ShopObjectClassMigrationPhase other = (ShopObjectClassMigrationPhase) migrationPhase;
			return shopObjectClass.isAssignableFrom(other.shopObjectClass);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + shopObjectClass.hashCode();
			return result;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof ShopObjectClassMigrationPhase)) return false;
			ShopObjectClassMigrationPhase other = (ShopObjectClassMigrationPhase) obj;
			return (shopObjectClass == other.shopObjectClass);
		}
	}

	/**
	 * Migrations for this phase are executed last.
	 */
	public static final MigrationPhase LATE = new MigrationPhase("late");

	private final String name;

	private MigrationPhase(String name) {
		Validate.notEmpty(name, "name is empty");
		this.name = name;
	}

	/**
	 * Gets the name of this migration phase.
	 * 
	 * @return the name, not <code>null</code> or empty
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Checks whether this {@link MigrationPhase} is meant to execute in the given current migration
	 * phase.
	 * 
	 * @param migrationPhase
	 *            the current migration phase, not <code>null</code>
	 * @return <code>true</code> if this migration phase is meant to execute in the given current
	 *         migration phase
	 */
	public boolean isApplicable(MigrationPhase migrationPhase) {
		return this.equals(migrationPhase);
	}

	@Override
	public String toString() {
		return "MigrationPhase " + this.getName();
	}
}
