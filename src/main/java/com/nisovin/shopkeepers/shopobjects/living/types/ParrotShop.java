package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Utils;

public class ParrotShop extends SittableShop<Parrot> {

	private static final Property<Parrot.Variant> PROPERTY_PARROT_VARIANT = new EnumProperty<>(Parrot.Variant.class, "parrotVariant", Parrot.Variant.RED);

	private Parrot.Variant parrotVariant = PROPERTY_PARROT_VARIANT.getDefaultValue();

	public ParrotShop(	LivingShops livingShops, SKLivingShopObjectType<ParrotShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	protected boolean isBabyable() {
		return false; // baby parrots don't seem to work
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.parrotVariant = PROPERTY_PARROT_VARIANT.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_PARROT_VARIANT.save(shopkeeper, configSection, parrotVariant);
	}

	@Override
	protected void onSpawn(Parrot entity) {
		super.onSpawn(entity);
		this.applyParrotVariant(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getParrotVariantEditorButton());
		return editorButtons;
	}

	// PARROT VARIANT

	public void setParrotVariant(Parrot.Variant parrotVariant) {
		Validate.notNull(parrotVariant, "Parrot variant is null!");
		this.parrotVariant = parrotVariant;
		shopkeeper.markDirty();
		this.applyParrotVariant(this.getEntity()); // null if not active
	}

	private void applyParrotVariant(Parrot entity) {
		if (entity == null) return;
		entity.setVariant(parrotVariant);
	}

	public void cycleParrotVariant() {
		this.setParrotVariant(Utils.getNextEnumConstant(Parrot.Variant.class, parrotVariant));
	}

	private ItemStack getParrotVariantEditorItem() {
		ItemStack iconItem;
		switch (parrotVariant) {
		case BLUE:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.BLUE));
			break;
		case CYAN:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.LIGHT_BLUE));
			break;
		case GRAY:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.LIGHT_GRAY));
			break;
		case GREEN:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.LIME));
			break;
		case RED:
		default:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.RED));
			break;
		}
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getParrotVariantEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getParrotVariantEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleParrotVariant();
				return true;
			}
		};
	}
}
