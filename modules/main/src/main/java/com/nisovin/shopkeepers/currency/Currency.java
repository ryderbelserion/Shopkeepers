package com.nisovin.shopkeepers.currency;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Information about an item-based currency.
 */
public final class Currency {

	private final String id;
	private final String displayName;
	private final ItemData itemData;
	private final int value;

	/**
	 * Creates a new {@link Currency}.
	 * 
	 * @param id
	 *            the unique id, not <code>null</code> or empty, gets
	 *            {@link StringUtils#normalize(String) normalized}
	 * @param displayName
	 *            a display name that is used in messages, not <code>null</code> or empty
	 * @param itemData
	 *            the item data, not <code>null</code>, not empty
	 * @param value
	 *            the value, has to be positive
	 */
	public Currency(String id, String displayName, ItemData itemData, int value) {
		Validate.notEmpty(id, "id is null or empty");
		Validate.notEmpty(displayName, "displayName is null or empty");
		Validate.notNull(itemData, "itemData is null");
		Validate.isTrue(!ItemUtils.isEmpty(itemData.asUnmodifiableItemStack()), "itemData is empty");
		Validate.isTrue(value > 0, "value has to be positive");
		this.id = StringUtils.normalize(id);
		Validate.notEmpty(this.id, "id is blank");
		this.displayName = displayName;
		this.itemData = itemData;
		this.value = value;
	}

	/**
	 * Gets the unique and persistent id of this currency.
	 * 
	 * @return the id, not <code>null</code> or empty
	 */
	public String getId() {
		return id;
	}

	/**
	 * Gets the display name.
	 * 
	 * @return the display name, not <code>null</code> or empty
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Gets the {@link ItemData} of this currency.
	 * 
	 * @return the item data, not <code>null</code>, does not produce empty item stacks
	 */
	public ItemData getItemData() {
		return itemData;
	}

	/**
	 * Gets the value of a single item of this currency measured in some base currency with value
	 * <code>1</code>.
	 * 
	 * @return the value, is positive
	 */
	public int getValue() {
		return value;
	}

	/**
	 * Gets the maximum stack size of this currency item.
	 * 
	 * @return the maximum stack size of this currency item
	 */
	public int getMaxStackSize() {
		return itemData.getMaxStackSize();
	}

	/**
	 * Gets the value of a full item stack of size {@link #getMaxStackSize()} of this currency.
	 * 
	 * @return the value of a full item stack of this currency
	 */
	public int getStackValue() {
		return this.getMaxStackSize() * value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Currency [id=");
		builder.append(id);
		builder.append(", itemData=");
		builder.append(itemData);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id.hashCode();
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Currency)) return false;
		Currency other = (Currency) obj;
		if (!id.equals(other.id)) return false;
		return true;
	}
}
