package com.nisovin.shopkeepers.ui;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.ui.confirmations.ConfirmationUIType;
import com.nisovin.shopkeepers.ui.editor.EditorUIType;
import com.nisovin.shopkeepers.ui.hiring.HiringUIType;
import com.nisovin.shopkeepers.ui.trading.TradingUIType;
import com.nisovin.shopkeepers.ui.villagerEditor.VillagerEditorUIType;

public final class SKDefaultUITypes implements DefaultUITypes {

	private final EditorUIType editorUIType = EditorUIType.INSTANCE;
	private final TradingUIType tradingUIType = TradingUIType.INSTANCE;
	private final HiringUIType hiringUIType = HiringUIType.INSTANCE;
	private final VillagerEditorUIType villagerEditorUIType = VillagerEditorUIType.INSTANCE;
	private final ConfirmationUIType confirmationUIType = ConfirmationUIType.INSTANCE;

	public SKDefaultUITypes() {
	}

	@Override
	public List<? extends @NonNull AbstractUIType> getAllUITypes() {
		List<@NonNull AbstractUIType> defaults = new ArrayList<>();
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

	// Internal.
	public ConfirmationUIType getConfirmationUIType() {
		return confirmationUIType;
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

	public static ConfirmationUIType CONFIRMATION() {
		return getInstance().getConfirmationUIType();
	}
}
