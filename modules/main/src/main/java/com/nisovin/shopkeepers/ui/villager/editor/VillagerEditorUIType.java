package com.nisovin.shopkeepers.ui.villager.editor;

import com.nisovin.shopkeepers.ui.AbstractUIType;

public final class VillagerEditorUIType extends AbstractUIType {

	public static final VillagerEditorUIType INSTANCE = new VillagerEditorUIType();

	private VillagerEditorUIType() {
		super("villager-editor", null); // Permission is checked on UI opening.
	}
}
