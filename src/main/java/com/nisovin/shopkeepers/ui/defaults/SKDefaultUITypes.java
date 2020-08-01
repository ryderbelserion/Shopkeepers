package com.nisovin.shopkeepers.ui.defaults;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.ui.AbstractUIType;

public class SKDefaultUITypes implements DefaultUITypes {

	private final EditorUIType editorUIType = new EditorUIType();
	private final TradingUIType tradingUIType = new TradingUIType();
	private final HiringUIType hiringUIType = new HiringUIType();
	private final VillagerEditorUIType villagerEditorUIType = new VillagerEditorUIType();

	public SKDefaultUITypes() {
	}

	@Override
	public List<AbstractUIType> getAllUITypes() {
		List<AbstractUIType> defaults = new ArrayList<>();
		defaults.add(editorUIType);
		defaults.add(tradingUIType);
		defaults.add(hiringUIType);
		defaults.add(villagerEditorUIType);
		return defaults;
	}

	@Override
	public EditorUIType getEditorUIType() {
		return editorUIType;
	}

	@Override
	public TradingUIType getTradingUIType() {
		return tradingUIType;
	}

	@Override
	public HiringUIType getHiringUIType() {
		return hiringUIType;
	}

	// Internal.
	public VillagerEditorUIType getVillagerEditorUIType() {
		return villagerEditorUIType;
	}

	// STATICS (for convenience):

	public static SKDefaultUITypes getInstance() {
		return SKShopkeepersPlugin.getInstance().getDefaultUITypes();
	}

	public static EditorUIType EDITOR() {
		return getInstance().getEditorUIType();
	}

	public static TradingUIType TRADING() {
		return getInstance().getTradingUIType();
	}

	public static HiringUIType HIRING() {
		return getInstance().getHiringUIType();
	}

	public static VillagerEditorUIType VILLAGER_EDITOR() {
		return getInstance().getVillagerEditorUIType();
	}
}
