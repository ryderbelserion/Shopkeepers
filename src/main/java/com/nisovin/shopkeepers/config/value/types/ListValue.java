package com.nisovin.shopkeepers.config.value.types;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.config.value.ValueType;
import com.nisovin.shopkeepers.util.Validate;

public class ListValue<E> extends ValueType<List<E>> {

	private final ValueType<E> elementValueType;

	public ListValue(ValueType<E> elementValueType) {
		Validate.notNull(elementValueType, "elementValueType is null!");
		this.elementValueType = elementValueType;
	}

	@Override
	public List<E> load(Object configValue) throws SettingLoadException {
		if (configValue == null) return null;
		if (!(configValue instanceof List<?>)) {
			throw new SettingLoadException("Expecting a list of values, but got " + configValue.getClass().getName());
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
}
