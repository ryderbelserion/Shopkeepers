package com.nisovin.shopkeepers.villagers;

import java.util.Map;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

// Handles prevention of trading and hiring of other villagers (including wandering traders)
public class VillagerInteractionListener implements Listener {

	private final ShopkeepersPlugin plugin;

	public VillagerInteractionListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	// HIGH, since we don't want to handle hiring if another plugin has cancelled the event.
	// But not HIGHEST, so that other plugins can still react to us cancelling the event.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	void onEntityInteract(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof AbstractVillager)) return;
		AbstractVillager villager = (AbstractVillager) event.getRightClicked();
		boolean isVillager = (villager instanceof Villager);
		boolean isWanderingTrader = (!isVillager && villager instanceof WanderingTrader);
		if (!isVillager && !isWanderingTrader) return; // unknown villager sub-type

		if (plugin.getShopkeeperRegistry().isShopkeeper(villager)) {
			// shopkeeper interaction is handled elsewhere
			return;
		}
		Log.debug("Interaction with non-shopkeeper villager ..");

		if (CitizensHandler.isNPC(villager)) {
			// ignore any interaction with citizens2 NPCs
			Log.debug("  ignoring (probably citizens2) NPC");
			return;
		}

		if ((isVillager && Settings.disableOtherVillagers) || (isWanderingTrader && Settings.disableWanderingTraders)) {
			// prevent trading with non-shopkeeper villagers:
			event.setCancelled(true);
			Log.debug("  trading prevented");
		}

		// only trigger hiring for main-hand events:
		if (event.getHand() != EquipmentSlot.HAND) return;

		if ((isVillager && Settings.hireOtherVillagers) || (isWanderingTrader && Settings.hireWanderingTraders)) {
			Player player = event.getPlayer();
			// allow hiring of other villagers
			Log.debug("  possible hire ..");
			if (this.handleHireOtherVillager(player, villager)) {
				// hiring was successful -> prevent normal trading
				Log.debug("    ..success (normal trading prevented)");
				event.setCancelled(true);
			} else {
				// hiring was not successful -> no preventing of normal villager trading
				Log.debug("    ..failed");
			}
		}
	}

	// returns false, if the player wasn't able to hire this villager
	private boolean handleHireOtherVillager(Player player, AbstractVillager villager) {
		// check if the player is allowed to remove (attack) the entity (in case the entity is protected by another
		// plugin)
		Log.debug("    checking villager access ..");
		TestEntityDamageByEntityEvent fakeDamageEvent = new TestEntityDamageByEntityEvent(player, villager);
		plugin.getServer().getPluginManager().callEvent(fakeDamageEvent);
		if (fakeDamageEvent.isCancelled()) {
			Log.debug("    no permission to remove villager");
			return false;
		}
		// hire him if holding his hiring item
		PlayerInventory playerInventory = player.getInventory();
		ItemStack itemInMainHand = playerInventory.getItemInMainHand();
		if (!Settings.isHireItem(itemInMainHand)) {
			Utils.sendMessage(player, Settings.msgVillagerForHire, "{costs}", String.valueOf(Settings.hireOtherVillagersCosts),
					"{hire-item}", Settings.hireItem.getType().name()); // TODO also print required hire item name and lore?
			return false;
		} else {
			// check if the player has enough of those hiring items
			int costs = Settings.hireOtherVillagersCosts;
			if (costs > 0) {
				ItemStack[] storageContents = playerInventory.getStorageContents();
				if (ItemUtils.containsAtLeast(storageContents, Settings.hireItem, costs)) {
					Log.debug("  Villager hiring: the player has the needed amount of hiring items");
					int inHandAmount = itemInMainHand.getAmount();
					int remaining = inHandAmount - costs;
					Log.debug("  Villager hiring: in hand=" + inHandAmount + " costs=" + costs + " remaining=" + remaining);
					if (remaining > 0) {
						itemInMainHand.setAmount(remaining);
					} else { // remaining <= 0
						playerInventory.setItemInMainHand(null); // remove item in hand
						if (remaining < 0) {
							// remove remaining costs from inventory:
							ItemUtils.removeItems(storageContents, Settings.hireItem, -remaining);
							// apply the change to the player's inventory:
							playerInventory.setStorageContents(storageContents);
						}
					}
				} else {
					Utils.sendMessage(player, Settings.msgCantHire);
					return false;
				}
			}

			// give player the shop creation item
			ItemStack shopCreationItem = Settings.createShopCreationItem();
			Map<Integer, ItemStack> remaining = playerInventory.addItem(shopCreationItem);
			if (!remaining.isEmpty()) {
				villager.getWorld().dropItem(villager.getLocation(), shopCreationItem);
			}

			// remove the entity:
			// note: the leashed trader llamas for the wandering trader will break and the llamas will remain
			villager.remove();

			// update client's inventory
			player.updateInventory();

			Utils.sendMessage(player, Settings.msgHired);
			return true;
		}
	}
}
