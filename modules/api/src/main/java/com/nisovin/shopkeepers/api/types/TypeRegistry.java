package com.nisovin.shopkeepers.api.types;

import java.util.Collection;

public interface TypeRegistry<T extends Type> {

	public void register(T type);

	public void registerAll(Collection<? extends T> types);

	public Collection<? extends T> getRegisteredTypes();

	public T get(String identifier);

	public T match(String identifier);

	public void clearAll();
}
