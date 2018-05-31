package com.nisovin.shopkeepers.ui.defaults;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.ui.AbstractUIType;

public final class HiringUIType extends AbstractUIType {

	HiringUIType() {
		super("hiring", ShopkeepersPlugin.HIRE_PERMISSION);
	}
}
