package com.nisovin.shopkeepers.config.lib.value;

import java.lang.reflect.Type;

public interface TypePattern {

	public boolean matches(Type type);

}
