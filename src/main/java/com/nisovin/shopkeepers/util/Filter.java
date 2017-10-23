package com.nisovin.shopkeepers.util;

public interface Filter<T> {

	public boolean accept(T object);
}
