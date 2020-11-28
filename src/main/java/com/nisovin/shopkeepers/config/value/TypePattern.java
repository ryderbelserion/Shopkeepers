package com.nisovin.shopkeepers.config.value;

import java.lang.reflect.Type;

public interface TypePattern {

	public boolean matches(Type type);

}
