package com.nisovin.shopkeepers.shopkeeper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperSnapshot;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.MissingDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.InstantSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Implementation of {@link ShopkeeperSnapshot}.
 */
public final class SKShopkeeperSnapshot implements ShopkeeperSnapshot {

	/**
	 * The maximum snapshot name length.
	 */
	public static final int MAX_NAME_LENGTH = 64;

	/**
	 * Checks if the given {@link ShopkeeperSnapshot} name is valid.
	 * <p>
	 * The tested constraints are not fixed and might change or be extended in the future.
	 * Currently, this performs at least the following checks:
	 * <ul>
	 * <li>The name is not <code>null</code> or empty.
	 * <li>The name's length does not exceed {@link #MAX_NAME_LENGTH}.
	 * <li>The name does not contain the color code character {@link ChatColor#COLOR_CHAR}
	 * (character '&amp;' is allowed).
	 * </ul>
	 * 
	 * @param name
	 *            the name
	 * @return <code>true</code> if the name is valid
	 */
	public static boolean isNameValid(String name) {
		try {
			validateName(name);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Validates the given {@link ShopkeeperSnapshot} name.
	 * <p>
	 * The checked constraints are consistent with {@link #isNameValid(String)}.
	 * 
	 * @param name
	 *            the name
	 * @throws IllegalArgumentException
	 *             if the name is invalid
	 */
	public static void validateName(String name) {
		Validate.notEmpty(name, "name is null or empty");
		Validate.isTrue(name.length() <= MAX_NAME_LENGTH,
				() -> "name is more than " + MAX_NAME_LENGTH + " characters long: " + name);
		Validate.isTrue(!TextUtils.containsColorChar(name),
				() -> "name contains color code character '§': " + name);
	}

	private static final Property<String> NAME = new BasicProperty<String>()
			.dataKeyAccessor("name", StringSerializers.SCALAR)
			.validator(SKShopkeeperSnapshot::validateName)
			.build();
	private static final Property<Instant> TIMESTAMP = new BasicProperty<Instant>()
			.dataKeyAccessor("timestamp", InstantSerializers.ISO)
			.build();
	private static final Property<DataContainer> DATA = new BasicProperty<DataContainer>()
			.dataKeyAccessor("data", DataContainerSerializers.DEFAULT)
			.build();

	/**
	 * A {@link DataSerializer} for {@link SKShopkeeperSnapshot} values.
	 */
	public static final DataSerializer<SKShopkeeperSnapshot> SERIALIZER = new DataSerializer<SKShopkeeperSnapshot>() {
		@Override
		public @Nullable Object serialize(SKShopkeeperSnapshot value) {
			Validate.notNull(value, "value is null");
			DataContainer dataContainer = DataContainer.create();
			dataContainer.set(NAME, value.getName());
			dataContainer.set(TIMESTAMP, value.getTimestamp());
			dataContainer.set(DATA, value.getShopkeeperData());
			return dataContainer.serialize();
		}

		@Override
		public SKShopkeeperSnapshot deserialize(Object data) throws InvalidDataException {
			DataContainer dataContainer = DataContainerSerializers.DEFAULT.deserialize(data);
			try {
				String name = dataContainer.get(NAME);
				Instant timestamp = dataContainer.get(TIMESTAMP);
				ShopkeeperData shopkeeperData = ShopkeeperData.ofNonNull(dataContainer.get(DATA));
				return new SKShopkeeperSnapshot(name, timestamp, shopkeeperData);
			} catch (MissingDataException e) {
				throw new InvalidDataException(e.getMessage(), e);
			}
		}
	};

	/**
	 * A {@link DataSerializer} for lists of {@link ShopkeeperSnapshot}s.
	 * <p>
	 * All contained elements are expected to not be <code>null</code>.
	 */
	public static final DataSerializer<List<? extends SKShopkeeperSnapshot>> LIST_SERIALIZER = new DataSerializer<List<? extends SKShopkeeperSnapshot>>() {
		@Override
		public @Nullable Object serialize(
				@ReadOnly List<? extends SKShopkeeperSnapshot> value
		) {
			Validate.notNull(value, "value is null");
			List<Object> snapshotListData = new ArrayList<>(value.size());
			value.forEach(snapshot -> {
				Object snapshotData = Unsafe.assertNonNull(SERIALIZER.serialize(snapshot));
				snapshotListData.add(snapshotData);
			});
			return snapshotListData;
		}

		@Override
		public List<? extends SKShopkeeperSnapshot> deserialize(
				Object data
		) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			if (!(data instanceof List)) {
				throw new InvalidDataException("Data is not a List, but of type "
						+ data.getClass().getName() + "!");
			}
			List<?> snapshotListData = (List<?>) data;
			List<SKShopkeeperSnapshot> snapshots = new ArrayList<>(snapshotListData.size());
			for (Object snapshotData : snapshotListData) {
				try {
					if (snapshotData == null) {
						throw new InvalidDataException("Data is null!");
					}
					assert snapshotData != null;
					SKShopkeeperSnapshot snapshot = SERIALIZER.deserialize(snapshotData);
					snapshots.add(snapshot);
				} catch (InvalidDataException e) {
					int snapshotNumber = snapshots.size() + 1;
					throw new InvalidDataException("Shopkeeper snapshot " + snapshotNumber
							+ " is invalid: " + e.getMessage(), e);
				}
			}
			return snapshots;
		}
	};

	/////

	private final String name;
	private final Instant timestamp;
	private final ShopkeeperData shopkeeperData;

	/**
	 * Creates a new {@link SKShopkeeperSnapshot}.
	 * 
	 * @param name
	 *            the name of this snapshot, has to be {@link #isNameValid(String) valid}
	 * @param timestamp
	 *            the timestamp of when this snapshot was taken, not <code>null</code>
	 * @param shopkeeperData
	 *            the (dynamic) shopkeeper data, not <code>null</code>
	 */
	public SKShopkeeperSnapshot(String name, Instant timestamp, ShopkeeperData shopkeeperData) {
		validateName(name);
		Validate.notNull(timestamp, "timestamp is null");
		Validate.notNull(shopkeeperData, "shopkeeperData is null");
		this.name = name;
		this.timestamp = timestamp;
		this.shopkeeperData = shopkeeperData;
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public final Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * The (dynamic) shopkeeper data at the time this snapshot was taken.
	 * 
	 * @return the shopkeeper data, not <code>null</code>
	 */
	public final ShopkeeperData getShopkeeperData() {
		return shopkeeperData;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getSimpleName() + " [name=");
		builder.append(name);
		builder.append(", timestamp=");
		builder.append(timestamp);
		builder.append(", shopkeeperData=");
		builder.append(shopkeeperData);
		builder.append("]");
		return builder.toString();
	}

	// Identity-based comparisons.

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	public final boolean equals(@Nullable Object obj) {
		return super.equals(obj);
	}
}
