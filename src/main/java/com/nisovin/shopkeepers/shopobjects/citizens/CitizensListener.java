package com.nisovin.shopkeepers.shopobjects.citizens;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import net.citizensnpcs.api.event.NPCRemoveEvent;
import net.citizensnpcs.api.event.NPCRemoveTraitEvent;
import net.citizensnpcs.api.npc.NPC;

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
}
