package com.nisovin.shopkeepers.config.lib.value.types;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.Validate;

public class ListValue<E> extends ValueType<List<E>> {

	public static final String DEFAULT_LIST_DELIMITER = ",";

	private final ValueType<E> elementValueType;

	public ListValue(ValueType<E> elementValueType) {
		Validate.notNull(elementValueType, "elementValueType is null!");
		this.elementValueType = elementValueType;
	}

	@Override
	public List<E> load(Object configValue) throws ValueLoadException {
		if (configValue == null) return null;
		if (!(configValue instanceof List<?>)) {
			throw new ValueLoadException("Expecting a list of values, but got " + configValue.getClass().getName());
		}
		List<?> configValues = (List<?>) configValue;
		List<E> values = new ArrayList<>(configValues.size());
		for (Object configElement : configValues) {
			values.add(elementValueType.load(configElement));
		}
		return values;
	}

	@Override
	public Object save(List<E> value) {
		if (value == null) return null;
		List<Object> configValues = new ArrayList<>(value.size());
		for (E element : value) {
			configValues.add(elementValueType.save(element));
		}
		return configValues;
	}

	@Override
	public String format(List<E> value) {
		return this.format(value, DEFAULT_LIST_DELIMITER);
	}

	public String format(List<E> value, String listDelimiter) {
		if (value == null) return "null";
		return value.stream()
				.map(element -> elementValueType.format(element))
				.collect(Collectors.joining(listDelimiter, "[", "]"));
	}

	@Override
	public List<E> parse(String input) throws ValueParseException {
		return this.parseValue(input, DEFAULT_LIST_DELIMITER);
	}

	public List<E> parseValue(String input, String listDelimiter) throws ValueParseException {
		Validate.notNull(input, "input is null");
		String[] splits = input.split(listDelimiter);
		List<E> values = new ArrayList<>(splits.length);
		for (String split : splits) {
			// Can throw ValueParseException:
			E element = elementValueType.parse(split.trim());
			assert element != null;
			values.add(element);
		}
		return values;
	}
}
