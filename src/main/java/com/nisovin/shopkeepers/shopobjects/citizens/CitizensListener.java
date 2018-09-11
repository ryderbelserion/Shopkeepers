package com.nisovin.shopkeepers.shopobjects.citizens;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import net.citizensnpcs.api.event.NPCRemoveEvent;
import net.citizensnpcs.api.event.NPCRemoveTraitEvent;
import net.citizensnpcs.api.event.NPCTraitCommandAttachEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;

class CitizensListener implements Listener {

	CitizensListener() {
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onNPCRemoved(NPCRemoveEvent event) {
		NPC npc = event.getNPC();
		if (npc.hasTrait(CitizensShopkeeperTrait.class)) {
			CitizensShopkeeperTrait shopkeeperTrait = npc.getTrait(CitizensShopkeeperTrait.class);
			shopkeeperTrait.onTraitDeletion();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onTraitRemoved(NPCRemoveTraitEvent event) {
		if (event.getTrait() instanceof CitizensShopkeeperTrait) {
			CitizensShopkeeperTrait shopkeeperTrait = (CitizensShopkeeperTrait) event.getTrait();
			shopkeeperTrait.onTraitDeletion();
		}
	}

	// gets called after the trait got added
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onTraitAddedByPlayer(NPCTraitCommandAttachEvent event) {
		if (!(event.getCommandSender() instanceof Player)) return;
		Player player = (Player) event.getCommandSender();
		NPC npc = event.getNPC();
		Class<? extends Trait> traitClass = event.getTraitClass();
		// trait got not removed again by some other event handler:
		if (!npc.hasTrait(traitClass)) return;

		Trait trait = npc.getTrait(event.getTraitClass());
		if (trait instanceof CitizensShopkeeperTrait) {
			CitizensShopkeeperTrait shopkeeperTrait = (CitizensShopkeeperTrait) trait;
			shopkeeperTrait.onTraitAddedByPlayer(player);
		}
	}
}
