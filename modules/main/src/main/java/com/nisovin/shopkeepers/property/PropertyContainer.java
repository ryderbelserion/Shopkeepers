package com.nisovin.shopkeepers.property;

import java.util.List;

/**
 * The container for a set of {@link Property properties}.
 * <p>
 * Each {@link Property} is added to exactly one {@link PropertyContainer}.
 */
public interface PropertyContainer {

	/**
	 * Gets the properties of this container.
	 * 
	 * @return an unmodifiable view on the properties of this container
	 */
	public List<? extends Property<?>> getProperties();
}
