package com.nisovin.shopkeepers.shopobjects.living.types.villager;

import org.bukkit.Sound;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * Mimics the ambient and trading sound effects of vanilla wandering traders.
 */
public class WanderingTraderSounds extends VillagerSounds {

	public WanderingTraderSounds(SKLivingShopObject<? extends WanderingTrader> shopObject) {
		super(shopObject);
	}

	@Override
	protected Sound getAmbientSound() {
		if (this.isShopkeeperTrading()) {
			return Sound.ENTITY_WANDERING_TRADER_TRADE;
		} else {
			return Sound.ENTITY_WANDERING_TRADER_AMBIENT;
		}
	}

	@Override
	protected Sound getTradeSound() {
		return Sound.ENTITY_WANDERING_TRADER_YES;
	}

	@Override
	protected Sound getTradeInteractionSound(@Nullable ItemStack resultItem) {
		if (ItemUtils.isEmpty(resultItem)) {
			return Sound.ENTITY_WANDERING_TRADER_NO;
		} else {
			return Sound.ENTITY_WANDERING_TRADER_YES;
		}
	}
}
