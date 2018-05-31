package com.nisovin.shopkeepers.types;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.util.Utils;

public abstract class TypeRegistry<T extends AbstractType> {

	protected final Map<String, T> registeredTypes = new HashMap<>();

	// true on success:
	public boolean register(T type) {
		Validate.notNull(type);
		String identifier = type.getIdentifier();
		assert identifier != null && !identifier.isEmpty();
		Validate.isTrue(!registeredTypes.containsKey(identifier),
				"Another " + this.getTypeName() + " with identifier '" + identifier + "' is already registered!");
		registeredTypes.put(identifier, type);
		return true;
	}

	public void registerAll(Collection<T> types) {
		if (types == null) return;
		for (T type : types) {
			if (type != null) {
				this.register(type);
			}
		}
	}

	/**
	 * A name for the type this TypeRegistry is managing.
	 * <p>
	 * Used to print slightly more informative debug messages.<br>
	 * Examples: 'shop type', 'shop object type', 'trading window', 'hiring window', etc.
	 * 
	 * @return a name for the type this class is handling
	 */
	protected abstract String getTypeName();

	public Collection<T> getRegisteredTypes() {
		return Collections.unmodifiableCollection(registeredTypes.values());
	}

	/**
	 * Gets the number of the different types registered in this class.
	 * 
	 * @return the number of registered types
	 */
	public int numberOfRegisteredTypes() {
		return registeredTypes.size();
	}

	public T get(String identifier) {
		return registeredTypes.get(identifier);
	}

	public T match(String identifier) {
		if (identifier == null || identifier.isEmpty()) return null;
		// might slightly improve performance of this loop: java /might/ skip 'toLowerCase' calls if the string already
		// is in lower case:
		identifier = Utils.normalize(identifier);
		for (T type : registeredTypes.values()) {
			if (type.matches(identifier)) {
				return type;
			}
		}
		return null;
	}

	public void clearAll() {
		registeredTypes.clear();
	}
}
