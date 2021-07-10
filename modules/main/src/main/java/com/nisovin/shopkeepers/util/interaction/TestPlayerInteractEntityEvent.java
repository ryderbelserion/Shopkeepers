package com.nisovin.shopkeepers.util.interaction;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * A test event that is called to check if a player can interact with the given entity.
 */
public class TestPlayerInteractEntityEvent extends PlayerInteractEntityEvent {

	public TestPlayerInteractEntityEvent(Player who, Entity clickedEntity) {
		super(who, clickedEntity);
	}
}
