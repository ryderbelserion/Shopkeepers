package com.nisovin.shopkeepers.shopobjects.living.types.villager;

import org.bukkit.Sound;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;

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
		return this.isShopkeeperTrading() ? Sound.ENTITY_WANDERING_TRADER_TRADE : Sound.ENTITY_WANDERING_TRADER_AMBIENT;
	}

	@Override
	protected Sound getTradeSound() {
		return Sound.ENTITY_WANDERING_TRADER_YES;
	}

	@Override
	protected Sound getTradeInteractionSound(ItemStack resultItem) {
		return ItemUtils.isEmpty(resultItem) ? Sound.ENTITY_WANDERING_TRADER_NO : Sound.ENTITY_WANDERING_TRADER_YES;
	}
}
