package com.nisovin.shopkeepers.util.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ConversionUtils {

	public static final Map<? extends String, ? extends Boolean> BOOLEAN_VALUES;
	public static final Map<? extends String, ? extends Trilean> TRILEAN_VALUES;

	static {
		// Initialize boolean values:
		Map<String, Boolean> booleanValues = new HashMap<>();
		booleanValues.put("true", true);
		booleanValues.put("t", true);
		booleanValues.put("1", true);
		booleanValues.put("yes", true);
		booleanValues.put("y", true);
		booleanValues.put("on", true);
		booleanValues.put("enabled", true);

		booleanValues.put("false", false);
		booleanValues.put("f", false);
		booleanValues.put("0", false);
		booleanValues.put("no", false);
		booleanValues.put("n", false);
		booleanValues.put("off", false);
		booleanValues.put("disabled", false);

		BOOLEAN_VALUES = Collections.unmodifiableMap(booleanValues);

		// Initialize trilean values:
		Map<String, Trilean> trileanValues = new HashMap<>();
		BOOLEAN_VALUES.entrySet().forEach(entry -> {
			trileanValues.put(entry.getKey(), Trilean.fromBoolean(entry.getValue()));
		});

		trileanValues.put("undefined", Trilean.UNDEFINED);
		trileanValues.put("null", Trilean.UNDEFINED);
		trileanValues.put("none", Trilean.UNDEFINED);
		trileanValues.put("unset", Trilean.UNDEFINED);
		trileanValues.put("default", Trilean.UNDEFINED);

		TRILEAN_VALUES = Collections.unmodifiableMap(trileanValues);
	}

	public static @Nullable Integer parseInt(@Nullable String string) {
		if (string == null) return null;
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static @Nullable Long parseLong(@Nullable String string) {
		if (string == null) return null;
		try {
			return Long.parseLong(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static @Nullable Double parseDouble(@Nullable String string) {
		if (string == null) return null;
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static @Nullable Float parseFloat(@Nullable String string) {
		if (string == null) return null;
		try {
			return Float.parseFloat(string);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static @Nullable Boolean parseBoolean(@Nullable String string) {
		if (string == null) return null;
		return BOOLEAN_VALUES.get(string.toLowerCase(Locale.ROOT));
	}

	public static @Nullable Trilean parseTrilean(@Nullable String string) {
		if (string == null) return null;
		return TRILEAN_VALUES.get(string.toLowerCase(Locale.ROOT));
	}

	public static @Nullable UUID parseUUID(@Nullable String string) {
		if (string == null) return null;
		String uuidString = string;
		if (string.length() == 32) {
			// Possibly flat uuid, insert '-':
			uuidString = uuidString.substring(0, 8)
					+ "-"
					+ uuidString.substring(8, 12)
					+ "-"
					+ uuidString.substring(12, 16)
					+ "-"
					+ uuidString.substring(16, 20)
					+ "-"
					+ uuidString.substring(20, 32);
		}
		if (uuidString.length() == 36) {
			try {
				return UUID.fromString(uuidString);
			} catch (IllegalArgumentException e) {
				// Not a valid uuid.
			}
		}
		return null;
	}

	public static <E extends Enum<E>> @Nullable E parseEnum(
			Class<@NonNull E> enumType,
			@Nullable String enumName
	) {
		Validate.notNull(enumType, "enumType is null");
		if (enumName == null || enumName.isEmpty()) return null;

		// Try to parse the enum value without normalizing the enum name first (in case the enum
		// does not adhere to the expected normalized format):
		@Nullable E enumValue = EnumUtils.valueOf(enumType, enumName);
		if (enumValue != null) return enumValue;

		// Try with enum name normalization:
		String normalizedEnumName = EnumUtils.normalizeEnumName(enumName);
		return EnumUtils.valueOf(enumType, normalizedEnumName); // Returns null if parsing fails
	}

	// PARSE LISTS:

	public static List<Integer> parseIntList(
			Collection<? extends @Nullable String> strings
	) {
		Validate.notNull(strings, "strings is null");
		List<Integer> result = new ArrayList<>(strings.size());
		strings.forEach(string -> {
			Integer value = parseInt(string);
			if (value != null) {
				result.add(value);
			}
		});
		return result;
	}

	public static List<Long> parseLongList(
			Collection<? extends @Nullable String> strings
	) {
		Validate.notNull(strings, "strings is null");
		List<Long> result = new ArrayList<>(strings.size());
		strings.forEach(string -> {
			Long value = parseLong(string);
			if (value != null) {
				result.add(value);
			}
		});
		return result;
	}

	public static List<Double> parseDoubleList(
			Collection<? extends @Nullable String> strings
	) {
		Validate.notNull(strings, "strings is null");
		List<Double> result = new ArrayList<>(strings.size());
		strings.forEach(string -> {
			Double value = parseDouble(string);
			if (value != null) {
				result.add(value);
			}
		});
		return result;
	}

	public static List<Float> parseFloatList(
			Collection<? extends @Nullable String> strings
	) {
		Validate.notNull(strings, "strings is null");
		List<Float> result = new ArrayList<>(strings.size());
		strings.forEach(string -> {
			Float value = parseFloat(string);
			if (value != null) {
				result.add(value);
			}
		});
		return result;
	}

	public static <E extends Enum<E>> List<@NonNull E> parseEnumList(
			Class<@NonNull E> enumType,
			Collection<? extends @Nullable String> strings
	) {
		Validate.notNull(enumType, "enumType is null");
		Validate.notNull(strings, "strings is null");
		List<@NonNull E> result = new ArrayList<>(strings.size());
		strings.forEach(string -> {
			@Nullable E value = parseEnum(enumType, string);
			if (value != null) {
				result.add(value);
			}
		});
		return result;
	}

	// CONVERT OBJECTS:

	public static @Nullable String toString(@Nullable Object object) {
		return (object != null) ? object.toString() : null;
	}

	public static @Nullable Boolean toBoolean(@Nullable Object object) {
		@Nullable Trilean value = toTrilean(object);
		return value == null ? null : value.toBoolean();
	}

	public static @Nullable Trilean toTrilean(@Nullable Object object) {
		if (object == null || object instanceof Boolean) {
			// Unlike in parseTrilean, null is considered a valid value here to allow the conversion
			// from nullable Boolean (instead of indicating a conversion error).
			return Trilean.fromNullableBoolean((@Nullable Boolean) object);
		} else if (object instanceof Number) {
			int i = ((Number) object).intValue();
			if (i == 1) {
				return Trilean.TRUE;
			} else if (i == 0) {
				return Trilean.FALSE;
			}
		} else if (object instanceof String) {
			return parseTrilean((String) object);
		}
		return null;
	}

	public static @Nullable Integer toInteger(@Nullable Object object) {
		if (object instanceof Integer) {
			return (Integer) object;
		} else if (object instanceof Number) {
			return ((Number) object).intValue();
		} else if (object instanceof String) {
			return parseInt((String) object);
		}
		return null;
	}

	public static @Nullable Long toLong(@Nullable Object object) {
		if (object instanceof Long) {
			return (Long) object;
		} else if (object instanceof Number) {
			return ((Number) object).longValue();
		} else if (object instanceof String) {
			return parseLong((String) object);
		}
		return null;
	}

	public static @Nullable Double toDouble(@Nullable Object object) {
		if (object instanceof Double) {
			return (Double) object;
		} else if (object instanceof Number) {
			return ((Number) object).doubleValue();
		} else if (object instanceof String) {
			return parseDouble((String) object);
		}
		return null;
	}

	public static @Nullable Float toFloat(@Nullable Object object) {
		if (object instanceof Float) {
			return (Float) object;
		} else if (object instanceof Number) {
			return ((Number) object).floatValue();
		} else if (object instanceof String) {
			return parseFloat((String) object);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <E extends Enum<E>> @Nullable E toEnum(
			Class<@NonNull E> enumType,
			@Nullable Object object
	) {
		if (enumType.isInstance(object)) return (E) object;
		// Note: We only expect objects of type String to be valid here. We do not convert the
		// object to a String if it is not already a String.
		if (object instanceof String) {
			String enumName = (String) object;
			return parseEnum(enumType, enumName);
		}
		return null;
	}

	// CONVERT LISTS OF OBJECTS:

	public static @Nullable List<Integer> toIntegerList(@Nullable List<?> list) {
		if (list == null) return null;
		List<Integer> result = new ArrayList<>(list.size());
		list.forEach(value -> {
			Integer integerValue = toInteger(value);
			if (integerValue != null) {
				result.add(integerValue);
			}
		});
		return result;
	}

	public static @Nullable List<Double> toDoubleList(@Nullable List<?> list) {
		if (list == null) return null;
		List<Double> result = new ArrayList<>(list.size());
		list.forEach(value -> {
			Double doubleValue = toDouble(value);
			if (doubleValue != null) {
				result.add(doubleValue);
			}
		});
		return result;
	}

	public static @Nullable List<Float> toFloatList(@Nullable List<?> list) {
		if (list == null) return null;
		List<Float> result = new ArrayList<>(list.size());
		list.forEach(value -> {
			Float floatValue = toFloat(value);
			if (floatValue != null) {
				result.add(floatValue);
			}
		});
		return result;
	}

	public static @Nullable List<Long> toLongList(@Nullable List<?> list) {
		if (list == null) return null;
		List<Long> result = new ArrayList<>(list.size());
		list.forEach(value -> {
			Long longValue = toLong(value);
			if (longValue != null) {
				result.add(longValue);
			}
		});
		return result;
	}

	public static @Nullable List<Boolean> toBooleanList(@Nullable List<?> list) {
		if (list == null) return null;
		List<Boolean> result = new ArrayList<>(list.size());
		list.forEach(value -> {
			Boolean booleanValue = toBoolean(value);
			if (booleanValue != null) {
				result.add(booleanValue);
			}
		});
		return result;
	}

	public static @Nullable List<Trilean> toTrileanList(@Nullable List<?> list) {
		if (list == null) return null;
		List<Trilean> result = new ArrayList<>(list.size());
		list.forEach(value -> {
			Trilean trileanValue = toTrilean(value);
			if (trileanValue != null) {
				result.add(trileanValue);
			}
		});
		return result;
	}

	public static @Nullable List<String> toStringList(@Nullable List<?> list) {
		if (list == null) return null;
		List<String> result = new ArrayList<>(list.size());
		list.forEach(value -> {
			String stringValue = toString(value);
			if (stringValue != null) {
				result.add(stringValue);
			}
		});
		return result;
	}

	public static <E extends Enum<E>> @Nullable List<@NonNull E> toEnumList(
			Class<@NonNull E> enumType,
			@Nullable List<?> list
	) {
		if (list == null) return null;
		List<@NonNull E> result = new ArrayList<>(list.size());
		list.forEach(value -> {
			@Nullable E enumValue = toEnum(enumType, value);
			if (enumValue != null) {
				result.add(enumValue);
			}
		});
		return result;
	}

	private ConversionUtils() {
	}
}
