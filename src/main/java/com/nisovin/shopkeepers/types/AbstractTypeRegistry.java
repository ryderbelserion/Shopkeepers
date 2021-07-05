package com.nisovin.shopkeepers.types;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nisovin.shopkeepers.api.types.TypeRegistry;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class AbstractTypeRegistry<T extends AbstractType> implements TypeRegistry<T> {

	private final Map<String, T> registeredTypes = new LinkedHashMap<>();
	private final Collection<T> registeredTypesView = Collections.unmodifiableCollection(registeredTypes.values());

	protected AbstractTypeRegistry() {
	}

	@Override
	public void register(T type) {
		Validate.notNull(type);
		String identifier = type.getIdentifier();
		assert identifier != null && !identifier.isEmpty();
		Validate.isTrue(!registeredTypes.containsKey(identifier),
				"Another " + this.getTypeName() + " with identifier '" + identifier + "' is already registered!");
		registeredTypes.put(identifier, type);
	}

	@Override
	public void registerAll(Collection<? extends T> types) {
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

	@Override
	public Collection<? extends T> getRegisteredTypes() {
		return registeredTypesView;
	}

	@Override
	public T get(String identifier) {
		return registeredTypes.get(identifier);
	}

	@Override
	public T match(String identifier) {
		if (identifier == null || identifier.isEmpty()) return null;
		// Normalizing the identifier beforehand might slightly improve performance of this loop:
		identifier = StringUtils.normalize(identifier);
		for (T type : registeredTypesView) {
			if (type.matches(identifier)) {
				return type;
			}
		}
		return null;
	}

	@Override
	public void clearAll() {
		registeredTypes.clear();
	}
}
