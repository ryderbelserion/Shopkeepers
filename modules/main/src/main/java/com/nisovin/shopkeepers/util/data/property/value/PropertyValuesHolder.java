package com.nisovin.shopkeepers.util.data.property.value;

import java.util.List;

/**
 * The owner of a set of {@link PropertyValue}s.
 * <p>
 * Each {@link PropertyValue} is owned by exactly one {@link PropertyValuesHolder}.
 */
public interface PropertyValuesHolder {

	/**
	 * Gets the {@link PropertyValue}s of this holder.
	 * 
	 * @return an unmodifiable view on the {@link PropertyValue}s of this holder
	 */
	public List<? extends PropertyValue<?>> getPropertyValues();
}
