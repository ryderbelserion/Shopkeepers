package com.nisovin.shopkeepers.util.java;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

/**
 * Values in three-valued logic. See
 * <a href="https://en.wikipedia.org/wiki/Three-valued_logic">Wikipedia</a>.
 */
public enum Trilean {

	FALSE(false) {
		@Override
		public Trilean and(Trilean other) {
			return FALSE; // False even if other is Undefined
		}

		@Override
		public Trilean or(Trilean other) {
			return other; // Undefined if other is Undefined
		}

		@Override
		public Trilean not() {
			return TRUE;
		}
	},
	TRUE(true) {
		@Override
		public Trilean and(Trilean other) {
			return other; // Undefined if other is Undefined
		}

		@Override
		public Trilean or(Trilean other) {
			return TRUE; // True even if other is Undefined
		}

		@Override
		public Trilean not() {
			return FALSE;
		}
	},
	UNDEFINED(null) {
		@Override
		public Trilean and(Trilean other) {
			return other == FALSE ? FALSE : UNDEFINED;
		}

		@Override
		public Trilean or(Trilean other) {
			return other == TRUE ? TRUE : UNDEFINED;
		}

		@Override
		public Trilean not() {
			return UNDEFINED;
		}
	};

	private final @Nullable Boolean booleanValue;

	private Trilean(@Nullable Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}

	/**
	 * Gets the logical "and" of this and the given value.
	 * 
	 * @param other
	 *            the other value
	 * @return the logical "and" value
	 */
	public abstract Trilean and(Trilean other);

	/**
	 * Gets the logic "or" of this and the given value.
	 * 
	 * @param other
	 *            the other value
	 * @return the logical "or" value
	 */
	public abstract Trilean or(Trilean other);

	/**
	 * Gets the logical "not" of this value.
	 * 
	 * @return the logical "not" value
	 */
	public abstract Trilean not();

	/**
	 * Converts this value to a non-<code>null</code> {@link Boolean}.
	 * 
	 * @return the boolean value
	 */
	public boolean toBoolean() {
		return booleanValue == null ? false : Unsafe.assertNonNull(booleanValue);
	}

	/**
	 * Converts this value to a nullable {@link Boolean}.
	 * 
	 * @return the nullable boolean value
	 */
	public @Nullable Boolean toNullableBoolean() {
		return booleanValue;
	}

	/**
	 * Converts the given non-<code>null</code> {@link Boolean} value to the corresponding
	 * {@link Trilean} value.
	 * 
	 * @param value
	 *            the non-<code>null</code> boolean value
	 * @return the trilean value
	 */
	public static Trilean fromBoolean(boolean value) {
		return value ? TRUE : FALSE;
	}

	/**
	 * Converts the given nullable {@link Boolean} value to the corresponding {@link Trilean} value.
	 * 
	 * @param value
	 *            the nullable boolean value
	 * @return the trilean value
	 */
	public static Trilean fromNullableBoolean(@Nullable Boolean value) {
		return value != null ? fromBoolean((boolean) value) : UNDEFINED;
	}
}
