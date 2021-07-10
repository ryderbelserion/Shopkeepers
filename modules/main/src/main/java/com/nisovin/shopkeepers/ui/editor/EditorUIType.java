package com.nisovin.shopkeepers.ui.editor;

import com.nisovin.shopkeepers.ui.AbstractUIType;

public final class EditorUIType extends AbstractUIType {

	public static final EditorUIType INSTANCE = new EditorUIType();

	private EditorUIType() {
		super("editor", null);
	}
}
