package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// TODO Use BabyableShop as base once there is a common interface for this inside Bukkit.
public class ZombieShop<E extends Zombie> extends SKLivingShopObject<E> {

	private final Property<Boolean> babyProperty = new BooleanProperty()
			.key("baby")
			.defaultValue(false)
			.onValueChanged(this::applyBaby)
			.build(properties);

	public ZombieShop(	LivingShops livingShops, SKLivingShopObjectType<? extends ZombieShop<E>> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		babyProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		babyProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyBaby();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getBabyEditorButton());
		return editorButtons;
	}

	// BABY

	public boolean isBaby() {
		return babyProperty.getValue();
	}

	public void setBaby(boolean baby) {
		babyProperty.setValue(baby);
	}

	public void cycleBaby() {
		this.setBaby(!this.isBaby());
	}

	private void applyBaby() {
		E entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setBaby(this.isBaby());
	}

	private ItemStack getBabyEditorItem() {
		ItemStack iconItem = new ItemStack(Material.EGG);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonBaby, Messages.buttonBabyLore);
		return iconItem;
	}

	private Button getBabyEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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
