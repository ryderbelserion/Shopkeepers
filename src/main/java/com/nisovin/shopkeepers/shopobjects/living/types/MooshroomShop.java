package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;

public class MooshroomShop extends BabyableShop<MushroomCow> {

	private static final Property<MushroomCow.Variant> PROPERTY_VARIANT = new EnumProperty<>(MushroomCow.Variant.class, "variant", MushroomCow.Variant.RED);

	private MushroomCow.Variant variant = PROPERTY_VARIANT.getDefaultValue();

	public MooshroomShop(	LivingShops livingShops, SKLivingShopObjectType<MooshroomShop> livingObjectType,
							AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.variant = PROPERTY_VARIANT.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_VARIANT.save(shopkeeper, configSection, variant);
	}

	@Override
	protected void onSpawn(MushroomCow entity) {
		super.onSpawn(entity);
		this.applyVariant(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getVariantEditorButton());
		return editorButtons;
	}

	// VARIANT

	public void setVariant(MushroomCow.Variant variant) {
		Validate.notNull(variant, "Variant is null!");
		this.variant = variant;
		shopkeeper.markDirty();
		this.applyVariant(this.getEntity()); // Null if not active
	}

	private void applyVariant(MushroomCow entity) {
		if (entity == null) return;
		entity.setVariant(variant);
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(EnumUtils.cycleEnumConstant(MushroomCow.Variant.class, variant, backwards));
	}

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem;
		switch (variant) {
		case RED:
			iconItem = new ItemStack(Material.RED_MUSHROOM);
			break;
		case BROWN:
		default:
			iconItem = new ItemStack(Material.BROWN_MUSHROOM);
			break;
		}
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonMooshroomVariant, Messages.buttonMooshroomVariantLore);
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
