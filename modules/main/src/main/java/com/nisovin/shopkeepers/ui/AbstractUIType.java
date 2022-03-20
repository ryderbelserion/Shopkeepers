package com.nisovin.shopkeepers.ui;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.types.AbstractType;

public abstract class AbstractUIType extends AbstractType implements UIType {

	protected AbstractUIType(String identifier, @Nullable String permission) {
		super(identifier, permission);
	}
}
