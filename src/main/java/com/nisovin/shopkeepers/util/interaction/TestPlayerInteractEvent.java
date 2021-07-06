package com.nisovin.shopkeepers.util.interaction;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A test event that is called to check if a player can interact with the given block.
 */
public class TestPlayerInteractEvent extends PlayerInteractEvent {

	public TestPlayerInteractEvent(Player who, Action action, ItemStack item, Block clickedBlock, BlockFace clickedFace) {
		super(who, action, item, clickedBlock, clickedFace);
	}
}
