package com.nisovin.shopkeepers.property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for all {@link PropertyContainer}s.
 */
public abstract class AbstractPropertyContainer implements PropertyContainer {

	private final List<Property<?>> properties = new ArrayList<>();
	private final List<? extends Property<?>> propertiesView = Collections.unmodifiableList(properties);

	/**
	 * Creates a new {@link AbstractPropertyContainer}.
	 */
	public AbstractPropertyContainer() {
	}

	/**
	 * Adds the given {@link Property} to this container.
	 * <p>
	 * A property is only added once to exactly one container and then never removed again.
	 * 
	 * @param property
	 *            the property
	 */
	final void add(Property<?> property) {
		Validate.notNull(property, "property is null");
		Validate.isTrue(property.getContainer() == this, "property has already been added to another container");
		String key = property.getKey();
		for (Property<?> otherProperty : propertiesView) {
			if (key.equalsIgnoreCase(otherProperty.getKey())) {
				Validate.error("Another property with the same key has already been added: " + key);
			}
		}
	}

	@Override
	public final List<? extends Property<?>> getProperties() {
		return propertiesView;
	}

	/**
	 * Gets a prefix that can be used for log messages related to this container.
	 * 
	 * @return the log prefix, can be empty
	 */
	public abstract String getLogPrefix();

	/**
	 * Marks this container as 'dirty'.
	 */
	public abstract void markDirty();
}
