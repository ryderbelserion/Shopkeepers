package com.nisovin.shopkeepers.property;

import java.util.Set;

import com.nisovin.shopkeepers.property.Property.UpdateFlag;

/**
 * A listener that reacts to value changes of a {@link Property}.
 *
 * @param <T>
 *            the type of the changed value
 */
@FunctionalInterface
public interface ValueChangeListener<T> {

	/**
	 * Called when the value of a {@link Property} has changed.
	 * <p>
	 * This method might not be invoked for calls to {@link Property#setValue(Object, Set)} if the value did not
	 * actually change.
	 * 
	 * @param property
	 *            the involved property, not <code>null</code>
	 * @param oldValue
	 *            the old value
	 * @param newValue
	 *            the new value
	 * @param updateFlags
	 *            the update flags, not <code>null</code>
	 */
	public void onValueChanged(Property<T> property, T oldValue, T newValue, Set<? extends UpdateFlag> updateFlags);
}
