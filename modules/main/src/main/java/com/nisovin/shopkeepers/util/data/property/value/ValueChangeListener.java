package com.nisovin.shopkeepers.util.data.property.value;

import java.util.Set;

import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue.UpdateFlag;

/**
 * A listener that reacts to value changes of a {@link PropertyValue}.
 *
 * @param <T>
 *            the type of the changed value
 * @see PropertyValue#onValueChanged(ValueChangeListener)
 */
@FunctionalInterface
public interface ValueChangeListener<T> {

	/**
	 * This is called when the value of a {@link PropertyValue} has changed.
	 * <p>
	 * This method might not be invoked for calls to {@link PropertyValue#setValue(Object, Set)} if
	 * the value did not actually change.
	 * 
	 * @param property
	 *            the involved property, not <code>null</code>
	 * @param oldValue
	 *            the old value, can be <code>null</code> for the first initialization of the value
	 *            of the {@link PropertyValue} even if the property is not
	 *            {@link Property#isNullable nullable}
	 * @param newValue
	 *            the new value, valid according to {@link Property#validateValue(Object)}
	 * @param updateFlags
	 *            the update flags, not <code>null</code>, not meant to be modified
	 */
	public void onValueChanged(
			PropertyValue<T> property,
			T oldValue,
			T newValue,
			Set<? extends UpdateFlag> updateFlags
	);
}
