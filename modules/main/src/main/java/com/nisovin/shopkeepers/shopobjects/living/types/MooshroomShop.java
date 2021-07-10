package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.EditorHandler;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class MooshroomShop extends BabyableShop<MushroomCow> {

	private final Property<MushroomCow.Variant> variantProperty = new EnumProperty<>(shopkeeper, MushroomCow.Variant.class, "variant", MushroomCow.Variant.RED);

	public MooshroomShop(	LivingShops livingShops, SKLivingShopObjectType<MooshroomShop> livingObjectType,
							AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		variantProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		variantProperty.save(configSection);
	}

	@Override
	protected void onSpawn(MushroomCow entity) {
		super.onSpawn(entity);
		this.applyVariant(entity);
	}

	@Override
	public List<EditorHandler.Button> createEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getVariantEditorButton());
		return editorButtons;
	}

	// VARIANT

	public MushroomCow.Variant getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(MushroomCow.Variant variant) {
		Validate.notNull(variant, "Variant is null!");
		variantProperty.setValue(variant);
		shopkeeper.markDirty();
		this.applyVariant(this.getEntity()); // Null if not spawned
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(EnumUtils.cycleEnumConstant(MushroomCow.Variant.class, this.getVariant(), backwards));
	}

	private void applyVariant(MushroomCow entity) {
		if (entity == null) return;
		entity.setVariant(this.getVariant());
	}

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem;
		switch (this.getVariant()) {
		case RED:
			iconItem = new ItemStack(Material.RED_MUSHROOM);
			break;
		case BROWN:
		default:
			iconItem = new ItemStack(Material.BROWN_MUSHROOM);
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonMooshroomVariant, Messages.buttonMooshroomVariantLore);
		return iconItem;
	}

	private EditorHandler.Button getVariantEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getVariantEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleVariant(backwards);
				return true;
			}
		};
	}
}
