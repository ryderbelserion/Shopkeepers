package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
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

public class SnowmanShop extends SKLivingShopObject<Snowman> {

	private final Property<Boolean> pumpkinHeadProperty = new BooleanProperty(shopkeeper, "pumpkinHead", true);

	public SnowmanShop(	LivingShops livingShops, SKLivingShopObjectType<SnowmanShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		pumpkinHeadProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		pumpkinHeadProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn(Snowman entity) {
		super.onSpawn(entity);
		this.applyPumpkinHead(entity);
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getPumpkinHeadEditorButton());
		return editorButtons;
	}

	// PUMPKIN HEAD

	public boolean hasPumpkinHead() {
		return pumpkinHeadProperty.getValue();
	}

	public void setPumpkinHead(boolean pumpkinHead) {
		pumpkinHeadProperty.setValue(pumpkinHead);
		shopkeeper.markDirty();
		this.applyPumpkinHead(this.getEntity()); // Null if not spawned
	}

	public void cyclePumpkinHead() {
		this.setPumpkinHead(!this.hasPumpkinHead());
	}

	private void applyPumpkinHead(Snowman entity) {
		if (entity == null) return;
		entity.setDerp(!this.hasPumpkinHead());
	}

	private ItemStack getPumpkinHeadEditorItem() {
		ItemStack iconItem = new ItemStack(this.hasPumpkinHead() ? Material.CARVED_PUMPKIN : Material.PUMPKIN);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonSnowmanPumpkinHead, Messages.buttonSnowmanPumpkinHeadLore);
		return iconItem;
	}

	private Button getPumpkinHeadEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getPumpkinHeadEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cyclePumpkinHead();
				return true;
			}
		};
	}
}
