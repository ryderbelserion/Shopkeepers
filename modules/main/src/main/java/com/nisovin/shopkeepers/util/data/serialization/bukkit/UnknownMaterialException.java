package com.nisovin.shopkeepers.util.data.serialization.bukkit;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;

/**
 * An {@link InvalidDataException} that indicates that some data represents an unknown
 * {@link Material}.
 */
public class UnknownMaterialException extends InvalidDataException {

	private static final long serialVersionUID = 501465265297838645L;

	/**
	 * Creates a new {@link UnknownMaterialException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public UnknownMaterialException(@Nullable String message) {
		super(message);
	}

	/**
	 * Creates a new {@link UnknownMaterialException}.
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause
	 */
	public UnknownMaterialException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
