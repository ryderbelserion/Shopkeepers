package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.entity.WanderingTrader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.compat.NMSManager;
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

public class WanderingTraderShop extends BabyableShop<@NonNull WanderingTrader> {

	private final WanderingTraderSounds wanderingTraderSounds;

	public WanderingTraderShop(
			LivingShops livingShops,
			SKLivingShopObjectType<@NonNull WanderingTraderShop> livingObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
		wanderingTraderSounds = new WanderingTraderSounds(Unsafe.initialized(this));
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
		WanderingTrader entity = Unsafe.assertNonNull(this.getEntity());

		// Disable the vanilla ambient sounds if we simulate the ambient and/or trading sounds
		// ourselves:
		if (Settings.simulateWanderingTraderTradingSounds
				|| Settings.simulateWanderingTraderAmbientSounds) {
			entity.setSilent(true);
		}

		// Disable the delayed despawning of the wandering trader:
		NMSManager.getProvider().setDespawnDelay(entity, 0);
	}

	@Override
	public void onTick() {
		super.onTick();

		// Ambient sounds:
		if (Settings.simulateWanderingTraderAmbientSounds) {
			wanderingTraderSounds.tick();
		}
	}

	@Override
	public List<@NonNull Button> createEditorButtons() {
		List<@NonNull Button> editorButtons = super.createEditorButtons();
		return editorButtons;
	}
}
