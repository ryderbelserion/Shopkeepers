package com.nisovin.shopkeepers.ui.hiring;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.ui.AbstractUIType;

public final class HiringUIType extends AbstractUIType {

	public static final HiringUIType INSTANCE = new HiringUIType();

	private HiringUIType() {
		super("hiring", ShopkeepersPlugin.HIRE_PERMISSION);
	}
}
