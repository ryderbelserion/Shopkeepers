package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

// TODO use BabyableShop as base once there is a common interface for this inside bukkit
public class PigZombieShop extends SKLivingShopObject<PigZombie> {

	private static final Property<Boolean> PROPERTY_BABY = new BooleanProperty("baby", false);

	private boolean baby = PROPERTY_BABY.getDefaultValue();

	public PigZombieShop(	LivingShops livingShops, SKLivingShopObjectType<PigZombieShop> livingObjectType,
							AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.baby = PROPERTY_BABY.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_BABY.save(shopkeeper, configSection, baby);
	}

	@Override
	protected void onSpawn(PigZombie entity) {
		super.onSpawn(entity);
		this.applyBaby(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getBabyEditorButton());
		return editorButtons;
	}

	// BABY STATE

	public void setBaby(boolean baby) {
		this.baby = baby;
		shopkeeper.markDirty();
		this.applyBaby(this.getEntity()); // null if not active
	}

	private void applyBaby(PigZombie entity) {
		if (entity == null) return;
		entity.setBaby(baby);
	}

	public void cycleBaby() {
		this.setBaby(!baby);
	}

	private ItemStack getBabyEditorItem() {
		// TODO use mob-specific spawn egg (if available; some mobs (illusioner) don't have a spawn egg)?
		// on the other hand: using a single item consistently for the editor icon has benefits as well
		ItemStack iconItem = new ItemStack(Material.EGG);
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getBabyEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getBabyEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleBaby();
				return true;
			}
		};
	}
}
