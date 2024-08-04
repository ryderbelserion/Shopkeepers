package com.nisovin.shopkeepers.util.data.property.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for all {@link PropertyValuesHolder}s.
 */
public abstract class AbstractPropertyValuesHolder implements PropertyValuesHolder {

	private final List<PropertyValue<?>> propertyValues = new ArrayList<>();
	private final List<? extends PropertyValue<?>> propertyValuesView = Collections.unmodifiableList(
			propertyValues
	);

	/**
	 * Creates a new {@link AbstractPropertyValuesHolder}.
	 */
	public AbstractPropertyValuesHolder() {
	}

	/**
	 * Adds the given {@link PropertyValue} to this holder.
	 * <p>
	 * A {@link PropertyValue} is only added once to exactly one holder and then never removed
	 * again.
	 * 
	 * @param propertyValue
	 *            the property value
	 */
	final void add(PropertyValue<?> propertyValue) {
		Validate.notNull(propertyValue, "propertyValue is null");
		Validate.isTrue(propertyValue.getHolder() == this,
				"propertyValue has already been added to another holder");
		String propertyName = propertyValue.getProperty().getName();
		for (PropertyValue<?> otherPropertyValue : propertyValuesView) {
			if (propertyName.equalsIgnoreCase(otherPropertyValue.getProperty().getName())) {
				Validate.error(
						"Another PropertyValue with the same property name has already been added: "
								+ propertyName
				);
			}
		}
		propertyValues.add(propertyValue);
	}

	@Override
	public final List<? extends PropertyValue<?>> getPropertyValues() {
		return propertyValuesView;
	}

	/**
	 * Gets a prefix that can be used for log messages related to this holder.
	 * 
	 * @return the log prefix, can be empty
	 */
	public abstract String getLogPrefix();

	/**
	 * Marks this holder as 'dirty'.
	 */
	public abstract void markDirty();
}
