package com.nisovin.shopkeepers.api.shopobjects.living;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObject;

/**
 * A {@link ShopObject} that uses a specific mob type to represent a {@link Shopkeeper}.
 */
public interface LivingShopObject extends EntityShopObject {

	/**
	 * Gets the {@link EntityType} that is used by this {@link LivingShopObject}.
	 * 
	 * @return the used entity type
	 */
	public EntityType getEntityType();

	// EQUIPMENT

	/**
	 * Gets the {@link LivingShopEquipment}.
	 * <p>
	 * For certain mobs, the shop object might apply a default equipment or alter the configured
	 * equipment items before they are applied to the spawned mob, which might not be reflected by
	 * the returned {@link LivingShopEquipment}. For example, mobs that would usually burn in
	 * sunlight might use a default item as helmet when no indestructible head item is configured.
	 * And similarly, placeholder items are converted to their substituted item when they are
	 * applied to the mob.
	 * 
	 * @return the equipment to apply to the spawned entity
	 */
	public LivingShopEquipment getEquipment();

	/**
	 * Attempts to open the equipment editor for this shop object and the specified player.
	 * 
	 * @param player
	 *            the player to open the equipment editor for
	 * @param editAllSlots
	 *            <code>true</code> to open the equipment editor and allow the player to edit all
	 *            equipment slots regardless of which slots the mob actually supports, and
	 *            regardless of the `enable-all-equipment-editor-slots` setting. <code>false</code>
	 *            to handle the request similar to when the player uses the button in the editor.
	 * @return <code>true</code> if the interface was successfully opened
	 */
	public boolean openEquipmentEditor(Player player, boolean editAllSlots);
}
