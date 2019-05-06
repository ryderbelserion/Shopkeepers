package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class ZombieShop extends SKLivingShopObject<Zombie> {

	// TODO use BabyableShop as base once there is a common interface for this inside bukkit
	private boolean baby = false;

	public ZombieShop(	LivingShops livingShops, SKLivingShopObjectType<ZombieShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		baby = configSection.getBoolean("baby");
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("baby", baby);
	}

	@Override
	protected void onSpawn(Zombie entity) {
		super.onSpawn(entity);
		this.applyBaby(entity);
	}

	// EDITOR ACTIONS

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.getEditorButtons(); // assumes modifiable
		editorButtons.add(new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getBabyEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleBaby();
				return true;
			}
		});
		return editorButtons;
	}

	// BABY STATE

	public void setBaby(boolean baby) {
		this.baby = baby;
		shopkeeper.markDirty();
		this.applyBaby(this.getEntity()); // null if not active
	}

	protected void applyBaby(Zombie entity) {
		if (entity == null) return;
		entity.setBaby(baby);
	}

	public void cycleBaby() {
		this.setBaby(!baby);
	}

	protected ItemStack getBabyEditorItem() {
		// TODO use mob-specific spawn egg (if available; some mobs (illusioner) don't have a spawn egg)?
		// on the other hand: using a single item consistently for the editor icon has benefits as well
		ItemStack iconItem = new ItemStack(Material.EGG);
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}
}
