package com.nisovin.shopkeepers.villagers;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityTransformEvent.TransformReason;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.util.Utils;

// Prevents curing of zombie villagers
public class BlockZombieVillagerCuringListener implements Listener {

	public BlockZombieVillagerCuringListener() {
	}

	// Try to prevent curing as early as possible, so that the player doesn't waste his golden apple
	@EventHandler(ignoreCancelled = true)
	void onZombieVillagerCureStarted(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof ZombieVillager)) return;
		Player player = event.getPlayer();
		ItemStack itemInHand;
		if (event.getHand() == EquipmentSlot.HAND) {
			itemInHand = player.getInventory().getItemInMainHand();
		} else if (event.getHand() == EquipmentSlot.OFF_HAND) {
			itemInHand = player.getInventory().getItemInOffHand();
		} else {
			return; // unknown
		}
		if (itemInHand != null && itemInHand.getType() == Material.GOLDEN_APPLE) {
			// prevent curing:
			event.setCancelled(true);
			Utils.sendMessage(player, Settings.msgZombieVillagerCuringDisabled);
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onZombieVillagerCured(EntityTransformEvent event) {
		if (event.getTransformReason() != TransformReason.CURED) return;
		if (!(event.getEntity() instanceof ZombieVillager)) return;

		ZombieVillager zombieVillager = (ZombieVillager) event.getEntity();
		event.setCancelled(true);
		zombieVillager.setConversionTime(-1); // stop conversion

		// inform the player who initiated the curing:
		OfflinePlayer conversionOfflinePlayer = zombieVillager.getConversionPlayer();
		Player conversionPlayer = (conversionOfflinePlayer == null) ? null : conversionOfflinePlayer.getPlayer();
		if (conversionPlayer != null) {
			Utils.sendMessage(conversionPlayer, Settings.msgZombieVillagerCuringDisabled);
		}
	}
}
