package com.nisovin.shopkeepers.shopkeeper.migration;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Applies data migrations to a given {@link ShopkeeperData}.
 * <p>
 * {@link Migration}s can be registered via
 * {@link ShopkeeperDataMigrator#registerMigration(Migration)}.
 */
public abstract class Migration {

	private final String name;
	private final MigrationPhase targetPhase;

	/**
	 * Creates a new {@link Migration}.
	 * 
	 * @param name
	 *            the migration name, not <code>null</code> or empty
	 * @param targetPhase
	 *            the target migration phase, not <code>null</code>
	 */
	public Migration(String name, MigrationPhase targetPhase) {
		Validate.notEmpty(name, "name is null or empty");
		Validate.notNull(targetPhase, "targetPhase is null");
		this.name = name;
		this.targetPhase = targetPhase;
	}

	/**
	 * Gets the name of this migration.
	 * 
	 * @return the name, not <code>null</code> or empty
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Gets the {@link MigrationPhase} at which this migration is meant to execute.
	 * 
	 * @return the target migration phase, not <code>null</code>
	 */
	public final MigrationPhase getTargetPhase() {
		return targetPhase;
	}

	/**
	 * Applies the data migrations to the given shopkeeper data.
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
	 *            context specific log prefix, can be empty, not <code>null</code>
	 * @return <code>true</code> if the data has changed as a result of these migrations
	 * @throws InvalidDataException
	 *             if the data is invalid and cannot be migrated
	 */
	public abstract boolean migrate(
			ShopkeeperData shopkeeperData,
			String logPrefix
	) throws InvalidDataException;

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + targetPhase.hashCode();
		return result;
	}

	@Override
	public final boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Migration)) return false;
		Migration other = (Migration) obj;
		if (!name.equals(other.name)) return false;
		if (!targetPhase.equals(other.targetPhase)) return false;
		return true;
	}

	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Migration [name=");
		builder.append(name);
		builder.append(", targetPhase=");
		builder.append(targetPhase);
		builder.append("]");
		return builder.toString();
	}
}
