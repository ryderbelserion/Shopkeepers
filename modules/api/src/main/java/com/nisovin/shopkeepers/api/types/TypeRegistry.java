package com.nisovin.shopkeepers.api.types;

import java.util.Collection;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A registry that keeps track of registered {@link Type Types}.
 *
 * @param <T>
 *            the type of {@link Type} that is managed by this registry
 */
public interface TypeRegistry<T extends Type> {

	/**
	 * Registers the given {@link Type}.
	 * 
	 * @param type
	 *            the type to register, not <code>null</code>
	 */
	public void register(@NonNull T type);

	/**
	 * {@link #register(Type) Registers} the given {@link Type Types}.
	 * 
	 * @param types
	 *            the types to register, not <code>null</code>, has no effect if the given
	 *            collection is empty
	 */
	public void registerAll(Collection<? extends @NonNull T> types);

	/**
	 * Gets the registered types.
	 * 
	 * @return an unmodifiable view on the registered types
	 */
	public Collection<? extends @NonNull T> getRegisteredTypes();

	/**
	 * Gets the registered type for the given identifier.
	 * 
	 * @param identifier
	 *            the identifier, not <code>null</code>
	 * @return the corresponding type, or <code>null</code> if no corresponding type is found
	 */
	public @Nullable T get(String identifier);

	/**
	 * Gets the registered type for the given identifier.
	 * <p>
	 * Unlike {@link #get(String)}, this method uses the more lenient {@link Type#matches(String)}
	 * to identify a corresponding type.
	 * 
	 * @param identifier
	 *            the identifier, not <code>null</code>
	 * @return the corresponding type, or <code>null</code> if no corresponding type is found
	 */
	public @Nullable T match(String identifier);

	/**
	 * Unregisters all types from this registry.
	 */
	public void clearAll();
}
