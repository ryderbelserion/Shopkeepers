package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.entity.WanderingTrader;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.types.villager.WanderingTraderSounds;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.trading.TradingHandler;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;

public class WanderingTraderShop extends BabyableShop<WanderingTrader> {

	private final WanderingTraderSounds wanderingTraderSounds;

	public WanderingTraderShop(	LivingShops livingShops, SKLivingShopObjectType<WanderingTraderShop> livingObjectType,
								AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
		wanderingTraderSounds = new WanderingTraderSounds(this);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
	}

	@Override
	public void setup() {
		super.setup();

		if (Settings.simulateWanderingTraderTradingSounds) {
			UIHandler tradingUIHandler = shopkeeper.getUIHandler(DefaultUITypes.TRADING());
			if (tradingUIHandler instanceof TradingHandler) {
				TradingHandler tradingHandler = (TradingHandler) tradingUIHandler;
				tradingHandler.addListener(wanderingTraderSounds);
			}
		}
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		WanderingTrader entity = this.getEntity();
		if (Settings.simulateWanderingTraderTradingSounds || Settings.simulateWanderingTraderAmbientSounds) {
			entity.setSilent(true);
		}
	}

	@Override
	public void tick() {
		super.tick();

		// Ambient sounds:
		if (Settings.simulateWanderingTraderAmbientSounds) {
			wanderingTraderSounds.tick();
		}
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		return editorButtons;
	}
}
